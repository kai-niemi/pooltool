package io.cockroachdb.pool.configurer.model;

import java.time.Duration;

import org.postgresql.PGProperty;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonIncludeProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.zaxxer.hikari.HikariConfig;

@JsonIncludeProperties(value = {
        "maximumPoolSize", "minimumIdle",
        "connectionTimeout", "validationTimeout", "idleTimeout", "maxLifetime",
        "keepaliveTime", "initializationFailTimeout",
        "transactionIsolation", "readOnly", "autoCommit", "dataSourceProperties","connectionTestQuery"})
@JsonPropertyOrder(value = {
        "maximumPoolSize", "minimumIdle",
        "connectionTimeout", "validationTimeout", "idleTimeout", "maxLifetime",
        "keepaliveTime", "initializationFailTimeout",
        "transactionIsolation", "readOnly", "autoCommit", "dataSourceProperties", "connectionTestQuery"})
public class HikariConfigModel extends HikariConfig {
    public static HikariConfigModel toHikariModel(ConfigModel model)
            throws IllegalArgumentException {
        HikariConfigModel config = new HikariConfigModel();
        config.setPoolName(model.getPoolName());
        config.setMaximumPoolSize(model.getMaximumPoolSize());

        // Can't set -1 even if it's the default
        if (model.getMinimumIdle() >= 0) {
            config.setMinimumIdle(model.getMinimumIdle());
        }

        config.setIdleTimeout(Duration.ofSeconds(model.getIdleTimeout()).toMillis());
        config.setKeepaliveTime(Duration.ofSeconds(model.getKeepAliveTime()).toMillis());
        config.setMaxLifetime(Duration.ofSeconds(model.getMaxLifetime()).toMillis());
        config.setValidationTimeout(Duration.ofSeconds(model.getValidationTimeout()).toMillis());
        config.setConnectionTimeout(Duration.ofSeconds(model.getConnectionTimeout()).toMillis());
        config.setInitializationFailTimeout(Duration.ofSeconds(model.getInitializationFailTimeout()).toMillis());
        config.setConnectionTestQuery(model.getValidationQuery());
        config.setTransactionIsolation("TRANSACTION_" + model.getIsolation().name().toUpperCase());
        config.setAutoCommit(model.isAutoCommit());
        config.setReadOnly(model.isReadOnly());

        if (model.isReWriteBatchedInserts()) {
            config.addDataSourceProperty(PGProperty.REWRITE_BATCHED_INSERTS.getName(), true);
        }

        if (StringUtils.hasLength(model.getAppName())) {
            config.addDataSourceProperty(PGProperty.APPLICATION_NAME.getName(), model.getAppName());
        }

        return config;
    }
}
