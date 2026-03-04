package io.cockroachdb.pool.configurer.model;

import java.time.Duration;

import org.springframework.transaction.annotation.Isolation;

public enum ConfigProfile {
    DEFAULT("HikariCP defaults (avoid)") {
        @Override
        public ConfigStrategy configStrategy() {
            return form -> {
                form.setMaximumPoolSize(10);
                form.setMinimumIdle(-1);
                form.setConnectionTimeout(Duration.ofSeconds(30).toSeconds());
                form.setIdleTimeout(Duration.ofMinutes(10).toSeconds());
                form.setConnectionTimeout(Duration.ofSeconds(30).toSeconds());
                form.setMaxLifetime(Duration.ofMinutes(30).toSeconds());
                form.setKeepAliveTime(Duration.ofMinutes(2).toSeconds());
                form.setValidationTimeout(Duration.ofSeconds(5).toSeconds());
                form.setInitializationFailTimeout(Duration.ofSeconds(1).toSeconds());
                form.setIsolation(Isolation.READ_COMMITTED);
                form.setAutoCommit(true);
                form.setReadOnly(false);
                return form;
            };
        }
    },

    FIXED_SIZE("Fixed size with default timeouts") {
        @Override
        public ConfigStrategy configStrategy() {
            return model -> {
                final int vCPUs = model.getMultiplier().apply(model.getNumVCPUs())
                                  / Math.max(1, model.getNumInstances());

                model.setMaximumPoolSize(vCPUs);
                model.setMinimumIdle(-1);
                model.setConnectionTimeout(Duration.ofSeconds(30).toSeconds());
                model.setIdleTimeout(Duration.ofMinutes(10).toSeconds());
                model.setConnectionTimeout(Duration.ofSeconds(30).toSeconds());
                model.setMaxLifetime(Duration.ofMinutes(30).toSeconds());
                model.setKeepAliveTime(Duration.ofMinutes(2).toSeconds());
                model.setValidationTimeout(Duration.ofSeconds(5).toSeconds());
                model.setInitializationFailTimeout(Duration.ofSeconds(1).toSeconds());
                model.setIsolation(Isolation.SERIALIZABLE);
                model.setAutoCommit(true);
                model.setReadOnly(false);
                return model;
            };
        }
    },

    DYNAMIC_SIZE("Dynamic size with default timeouts") {
        @Override
        public ConfigStrategy configStrategy() {
            return model -> {
                final int vCPUs = model.getMultiplier().apply(model.getNumVCPUs())
                                  / Math.max(1, model.getNumInstances());

                model.setMaximumPoolSize(vCPUs);
                model.setMinimumIdle(Math.min(vCPUs, (int) Math.ceil(vCPUs * 0.25)));
                model.setConnectionTimeout(Duration.ofSeconds(30).toSeconds());
                model.setIdleTimeout(Duration.ofMinutes(10).toSeconds());
                model.setConnectionTimeout(Duration.ofSeconds(30).toSeconds());
                model.setMaxLifetime(Duration.ofMinutes(30).toSeconds());
                model.setKeepAliveTime(Duration.ofMinutes(2).toSeconds());
                model.setValidationTimeout(Duration.ofSeconds(5).toSeconds());
                model.setInitializationFailTimeout(Duration.ofSeconds(1).toSeconds());
                model.setIsolation(Isolation.SERIALIZABLE);
                model.setAutoCommit(true);
                model.setReadOnly(false);

                return model;
            };
        }
    },
    OPTIMIZED("Recommended size and timeouts") {
        @Override
        public ConfigStrategy configStrategy() {
            return model -> {
                final Duration connectionLifeTime = Duration.ofSeconds(model.getConnectionLifeTimeSeconds());
                final Duration maxLifeTime = connectionLifeTime.minusSeconds(5);
                final int vCPUs = model.getMultiplier().apply(model.getNumVCPUs())
                                  / Math.max(1, model.getNumInstances());

                model.setMaximumPoolSize(vCPUs);
                model.setMinimumIdle(Math.min(vCPUs, (int) Math.ceil(vCPUs * 0.25)));
                model.setConnectionTimeout(Duration.ofSeconds(10).toSeconds());
                // Set to 5 sec less than connection lifetime
                model.setMaxLifetime(maxLifeTime.toSeconds());
                if (model.getMinimumIdle() < model.getMaximumPoolSize() && model.getMinimumIdle() >= 0) {
                    // Set to 5 sec less than max lifetime
                    model.setIdleTimeout(maxLifeTime.minusSeconds(5).toSeconds());
                } else {
                    model.setIdleTimeout(Duration.ofSeconds(10).toSeconds());
                }
                model.setKeepAliveTime(maxLifeTime.minusSeconds(5).toSeconds());
                model.setConnectionTimeout(Duration.ofSeconds(30).toSeconds());
                model.setValidationTimeout(Duration.ofSeconds(5).toSeconds());

                model.setInitializationFailTimeout(-1L);
                model.setIsolation(Isolation.SERIALIZABLE);
                model.setAutoCommit(true);
                model.setReadOnly(false);

                return model;
            };
        }
    };

    private final String displayValue;

    ConfigProfile(String displayValue) {
        this.displayValue = displayValue;
    }

    public String getDisplayValue() {
        return displayValue;
    }

    public abstract ConfigStrategy configStrategy();
}
