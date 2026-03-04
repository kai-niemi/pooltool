package io.cockroachdb.pool.configurer.config;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Role;
import org.springframework.context.annotation.Scope;
import org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.metrics.micrometer.MicrometerMetricsTrackerFactory;

import net.ttddyy.dsproxy.listener.MethodExecutionContext;
import net.ttddyy.dsproxy.listener.lifecycle.JdbcLifecycleEventListenerAdapter;
import net.ttddyy.dsproxy.listener.logging.DefaultQueryLogEntryCreator;
import net.ttddyy.dsproxy.listener.logging.SLF4JLogLevel;
import net.ttddyy.dsproxy.listener.logging.SLF4JQueryLoggingListener;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;

import io.cockroachdb.pool.configurer.model.DataSourceModel;
import io.micrometer.core.instrument.MeterRegistry;

@Configuration
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class DataSourceConfig implements BeanClassLoaderAware {
    public static final String SQL_TRACE_LOGGER = "io.cockroachdb.pool.configurer.SQL_TRACE";

    private final Logger logger = LoggerFactory.getLogger(SQL_TRACE_LOGGER);

    private ClassLoader classLoader;

    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Bean
    @Lazy
    @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public Function<DataSource, PlatformTransactionManager> transactionManagerFactory() {
        return model -> {
            DataSourceTransactionManager transactionManager = new DataSourceTransactionManager();
            transactionManager.setDataSource(model);
            transactionManager.setGlobalRollbackOnParticipationFailure(false);
            transactionManager.setEnforceReadOnly(true);
            transactionManager.setNestedTransactionAllowed(true);
            transactionManager.setRollbackOnCommitFailure(false);
            transactionManager.setDefaultTimeout(-1);
            return transactionManager;
        };
    }

    @Bean
    @Lazy
    @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public Function<DataSourceModel, ClosableDataSource> dataSourceFactory(
            @Autowired MeterRegistry registry) {
        return model -> {
            HikariConfig config = model.getHikariConfig();
            config.setJdbcUrl(model.getUrl());
            config.setUsername(model.getUserName());
            config.setPassword(model.getPassword());
            config.setMetricsTrackerFactory(new MicrometerMetricsTrackerFactory(registry));

            HikariDataSource dataSource = DataSourceBuilder.create(classLoader)
                    .type(HikariDataSource.class)
                    .driverClassName(model.getDriverClassName())
                    .url(model.getUrl())
                    .username(model.getUserName())
                    .password(model.getPassword())
                    .build();

            config.copyStateTo(dataSource);

            DataSource target = model.isTraceLogging() ? loggingProxy(dataSource) : dataSource;

            if (model.getWaitTime() > 0) {
                return new ClosableDataSource(
                        slowCloseProxy(target,
                                model.getProbability(),
                                model.getWaitTime(),
                                model.getWaitTimeVariation()));
            }

            return new ClosableDataSource(target);
        };
    }

    private DataSource slowCloseProxy(DataSource dataSource, double probability,
                                      long waitTime, long waitTimeVariation) {
        return ProxyDataSourceBuilder
                .create(dataSource)
                .name("Connection Slow Close")
                .listener(new JdbcLifecycleEventListenerAdapter() {
                    @Override
                    public void beforeClose(MethodExecutionContext executionContext) {
                        try {
                            ThreadLocalRandom random = ThreadLocalRandom.current();
                            if (random.nextDouble(0, 1.0) < Math.min(1.0, probability)) {
                                long sleepTime = waitTime;
                                if (waitTimeVariation > 0) {
                                    sleepTime += random.nextLong(waitTimeVariation);
                                }
                                logger.debug("Delay connection close with %s"
                                        .formatted(Duration.ofSeconds(sleepTime)));
                                TimeUnit.SECONDS.sleep(sleepTime);
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        } finally {
                            super.beforeClose(executionContext);
                        }
                    }
                }).build();
    }

    private DataSource loggingProxy(DataSource dataSource) {
        DefaultQueryLogEntryCreator creator = new DefaultQueryLogEntryCreator();
        creator.setMultiline(true);

        SLF4JQueryLoggingListener listener = new SLF4JQueryLoggingListener();
        listener.setLogger(logger);
        listener.setLogLevel(SLF4JLogLevel.DEBUG);
        listener.setQueryLogEntryCreator(creator);
        listener.setWriteConnectionId(true);

        return ProxyDataSourceBuilder
                .create(dataSource)
                .name("SQL Logger")
                .listener(listener)
                .asJson()
                .build();
    }

    @Bean
    public PersistenceExceptionTranslationPostProcessor exceptionTranslation() {
        return new PersistenceExceptionTranslationPostProcessor();
    }
}
