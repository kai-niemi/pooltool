package io.cockroachdb.pool.configurer.workload;

import java.lang.reflect.UndeclaredThrowableException;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.dao.NonTransientDataAccessException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionException;

import io.cockroachdb.pool.configurer.metrics.Metrics;
import io.cockroachdb.pool.configurer.model.Problem;

@Component
public class WorkloadExecutor {
    private static void backoffDelayWithJitter(int inc) {
        try {
            TimeUnit.MILLISECONDS.sleep(
                    Math.min((long) (Math.pow(2, inc) + Math.random() * 1000), 5000));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static final AtomicInteger monotonicId = new AtomicInteger();

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    @Qualifier("workloadAsyncTaskExecutor")
    private AsyncTaskExecutor asyncTaskExecutor;

    public WorkloadModel submitWorkloadTask(String description,
                                            String poolName,
                                            Duration duration,
                                            Runnable task) {
        final Metrics metrics = Metrics.empty();
        final LinkedList<Problem> problems = new LinkedList<>();

        final WorkloadModel workloadModel = new WorkloadModel(
                monotonicId.incrementAndGet(),
                description,
                poolName,
                duration,
                metrics,
                problems);

        final Predicate<Integer> loopCondition = retries -> {
            return Instant.now().isBefore(Instant.now().plus(duration)) && !workloadModel.isCancelled();
        };

        final CompletableFuture<Boolean> future = asyncTaskExecutor.submitCompletable(() -> {
            final AtomicInteger retries = new AtomicInteger();

            logger.debug("Started workload %s".formatted(workloadModel.getId()));

            while (loopCondition.test(retries.get())) {
                if (Thread.interrupted()) {
                    logger.warn("Thread interrupted - bailing out");
                    break;
                }

                final Instant callTime = Instant.now();

                try {
                    task.run();
                    retries.set(0);
                    metrics.markSuccess(Duration.between(callTime, Instant.now()));
                } catch (Exception ex) {
                    Duration callDuration = Duration.between(callTime, Instant.now());

                    Throwable cause = NestedExceptionUtils.getMostSpecificCause(ex);
                    Problem problem = Problem.of(cause);

                    if (cause instanceof SQLException) {
                        String sqlState = ((SQLException) cause).getSQLState();
                        if (problem.isTransient()) {
                            logger.debug("Transient SQL exception [%s]: [%s]".formatted(sqlState, cause));
                        } else {
                            logger.debug("Non-transient SQL exception [%s]: [%s]".formatted(sqlState, cause));
                        }
                    } else if (cause instanceof TransientDataAccessException) {
                        logger.debug("Transient data access exception: [%s]".formatted(ex));
                    } else if (cause instanceof NonTransientDataAccessException
                               || cause instanceof TransactionException) {
                        logger.debug("Non-transient exception: [%s]".formatted(ex));
                    } else {
                        throw new UndeclaredThrowableException(ex);
                    }

                    metrics.markFail(callDuration, problem.isTransient());
                    problems.add(problem);

                    backoffDelayWithJitter(retries.incrementAndGet());
                }
            }

            logger.debug("Finished workload %s".formatted(workloadModel.getId()));

            return problems.isEmpty();
        });

        asyncTaskExecutor.submit(() -> {
            try {
                future.get();
            } catch (InterruptedException e) {
                problems.add((Problem.of(e)));
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
                problems.add((Problem.of(e.getCause())));
            } finally {
                workloadModel.updateStatus(future);
            }
        });

        return workloadModel;
    }
}