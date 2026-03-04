package io.cockroachdb.pool.configurer.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.context.request.async.CallableProcessingInterceptor;
import org.springframework.web.context.request.async.TimeoutCallableProcessingInterceptor;

@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig implements AsyncConfigurer {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public AsyncTaskExecutor getAsyncExecutor() {
        return boundedAsyncExecutor();
    }

    @Bean(name = "boundedAsyncExecutor")
    public AsyncTaskExecutor boundedAsyncExecutor() {
        // Use a bounded platform thread pool with an internal blocking queue.
        // When the queue is at capacity, the thread pool increases to "maxPoolSize" threads.
        // Threads are also reclaimed when they are idle for more than 10 seconds.
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("bounded-");
        executor.setCorePoolSize(4);
        executor.setQueueCapacity(32);
        executor.setKeepAliveSeconds(10);
        executor.initialize();
        return executor;
    }

    /**
     * An unbounded virtual thread executor to be used for I/O bound
     * load test workloads.
     */
    @Bean(name = "workloadAsyncTaskExecutor")
    public AsyncTaskExecutor workloadAsyncTaskExecutor() {
        // Use a preferably unbounded virtual thread task executor for I/O bound workloads.
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
        executor.setThreadNamePrefix("virtual-");
        executor.setVirtualThreads(true);
        return executor;
    }

    @Bean("applicationEventMulticaster")
    public ApplicationEventMulticaster applicationEventMulticaster() {
        SimpleApplicationEventMulticaster eventMulticaster = new SimpleApplicationEventMulticaster();
        eventMulticaster.setTaskExecutor(boundedAsyncExecutor());
        eventMulticaster.setErrorHandler(t -> {
            logger.error("Unexpected error occurred in scheduled task", t);
        });
        return eventMulticaster;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) -> {
            logger.error("Unexpected exception occurred invoking async method: " + method, ex);
        };
    }

    @Bean
    public CallableProcessingInterceptor callableProcessingInterceptor() {
        return new TimeoutCallableProcessingInterceptor();
    }
}


