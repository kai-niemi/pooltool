package io.cockroachdb.pool.configurer.workload;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import io.cockroachdb.pool.configurer.metrics.DurationUtils;
import io.cockroachdb.pool.configurer.metrics.Metrics;
import io.cockroachdb.pool.configurer.model.Problem;

public class WorkloadModel {
    private final Integer id;

    private final String title;

    private final String poolName;

    private final Duration duration;

    private final Metrics metrics;

    private final LinkedList<Problem> problems;

    private final Instant startTime;

    private WorkloadStatus status;

    public WorkloadModel(Integer id,
                         String title,
                         String poolName,
                         Duration duration,
                         Metrics metrics,
                         LinkedList<Problem> problems) {
        Objects.requireNonNull(id);
        Objects.requireNonNull(title);
        Objects.requireNonNull(duration);
        Objects.requireNonNull(metrics);
        Objects.requireNonNull(problems);

        this.id = id;
        this.title = title;
        this.poolName = poolName;
        this.duration = duration;
        this.metrics = metrics;
        this.problems = problems;
        this.startTime = Instant.now();
        this.status = WorkloadStatus.RUNNING;
    }

    public Integer getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getPoolName() {
        return poolName;
    }

    public List<Problem> getProblems() {
        return problems.stream().limit(25).toList();
    }

    public Metrics getMetrics() {
        return isRunning() ? metrics : Metrics.copy(metrics);
    }

    public String getRemainingTime() {
        return isRunning() ?
                DurationUtils.durationToDisplayString(getRemainingDuration())
                : "-";
    }

    public Duration getRemainingDuration() {
        Duration remainingDuration = Duration.between(Instant.now(), startTime.plus(duration));
        return isRunning() && remainingDuration.isPositive()
                ? remainingDuration : Duration.ofSeconds(0);
    }

    public String getCompletionTime() {
        return getCompletionDuration().isPositive()
                ? DurationUtils.durationToDisplayString(getCompletionDuration())
                : "-";
    }

    public Duration getCompletionDuration() {
        return isRunning() ? Duration.ofSeconds(0)
                : Duration.between(startTime, Instant.now());
    }

    public WorkloadStatus getStatus() {
        return status;
    }

    /**
     * Called from js
     */
    public String getStatusBadge() {
        return getStatus().getBadge();
    }

    public void updateStatus(CompletableFuture<?> future) {
        if (isCancelled()) {
            return;
        }
        if (future.isCompletedExceptionally()) {
            this.status = WorkloadStatus.FAILED;
        } else if (!future.isDone()) {
            this.status = WorkloadStatus.RUNNING;
        } else if (future.isCancelled()) {
            this.status = WorkloadStatus.CANCELLED;
        } else {
            this.status = WorkloadStatus.COMPLETED;
        }
    }

    public boolean isRunning() {
        return WorkloadStatus.RUNNING.equals(status);
    }

    public boolean isCancelled() {
        return WorkloadStatus.CANCELLED.equals(status);
    }

    public void cancel() {
        this.status = WorkloadStatus.CANCELLED;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        WorkloadModel workloadModel = (WorkloadModel) o;
        return Objects.equals(id, workloadModel.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
