package io.cockroachdb.pool.configurer.model;

import org.springframework.util.Assert;
import org.springframework.validation.annotation.Validated;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

@Validated
public class DataSourceModel {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final DataSourceModel instance = new DataSourceModel();

        public Builder withHikariConfig(HikariConfigModel hikariConfig) {
            instance.hikariConfig = hikariConfig;
            return this;
        }

        public Builder withDriverClassName(String driverClassName) {
            instance.driverClassName = driverClassName;
            return this;
        }

        public Builder withUrl(String url) {
            instance.url = url;
            return this;
        }

        public Builder withUsername(String username) {
            instance.userName = username;
            return this;
        }

        public Builder withPassword(String password) {
            instance.password = password;
            return this;
        }

        public Builder withName(String name) {
            instance.name = name;
            return this;
        }

        public Builder withTraceLogging(boolean traceLogging) {
            instance.traceLogging = traceLogging;
            return this;
        }

        public Builder withProbability(double probability) {
            instance.probability = probability;
            return this;
        }

        public Builder withWaitTime(long waitTime) {
            instance.waitTime = waitTime;
            return this;
        }

        public Builder withWaitTimeVariation(long waitTimeVariation) {
            instance.waitTimeVariation = waitTimeVariation;
            return this;
        }

        public DataSourceModel build() {
            Assert.hasLength(instance.driverClassName, "name is required");
            Assert.notNull(instance.url, "url is required");
            return instance;
        }
    }

    private String driverClassName;

    private String url;

    private String userName;

    private String password;

    private String name;

    @JsonProperty("hikari")
    private HikariConfigModel hikariConfig;

    @JsonIgnore
    private boolean traceLogging;

    @JsonIgnore
    private double probability;

    @JsonIgnore
    private long waitTime;

    @JsonIgnore
    private long waitTimeVariation;

    protected DataSourceModel() {
    }

    public double getProbability() {
        return probability;
    }

    public long getWaitTime() {
        return waitTime;
    }

    public long getWaitTimeVariation() {
        return waitTimeVariation;
    }

    public boolean isTraceLogging() {
        return traceLogging;
    }

    public String getDriverClassName() {
        return driverClassName;
    }

    public String getUrl() {
        return url;
    }

    public String getUserName() {
        return userName;
    }

    public String getPassword() {
        return password;
    }

    public String getName() {
        return name;
    }

    public HikariConfigModel getHikariConfig() {
        return hikariConfig;
    }
}
