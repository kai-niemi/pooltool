package io.cockroachdb.pool.configurer.model;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

import org.springframework.transaction.annotation.Isolation;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@Validated
public class ConfigModel {
    private String applicationModelYaml;

    @Min(value = 1, message = "Number of vCPUs must be > 0")
    private Integer numVCPUs;

    @Min(value = 1, message = "Number of instances must be > 0")
    private Integer numInstances;

    @Min(value = 0, message = "Connection life time must be >= seconds")
    private Long connectionLifeTimeSeconds;

    @Valid
    private ConfigProfile configProfile;

    @NotEmpty(message = "Application name must not be empty")
    private String appName;

    @NotEmpty(message = "User name must not be empty")
    private String userName;

    private String password;

    @NotEmpty(message = "URL must not be empty")
    @Pattern(regexp = "^jdbc:postgresql:(?://[^/]+/)?(\\w+).*", message = "URL must match 'jdbc:postgresql://host:port/db..'")
    private String url;

    @Valid
    private Isolation isolation;

    private boolean reWriteBatchedInserts;

    private boolean autoCommit;

    private boolean readOnly;

    @NotNull(message = "vCPU ratio must be selected")
    private Multiplier multiplier;

    @NotEmpty
    private EnumSet<Slot> slots = EnumSet.noneOf(Slot.class);

    @NotNull(message = "Pool slot must be selected")
    private Slot slot;

    @NotNull(message = "Maximum pool size must be set")
    @Min(value = 1, message = "Maximum pool size must be >= 1")
    private Integer maximumPoolSize;

    @NotNull(message = "Minimum pool size must be set")
    @Min(value = -1, message = "Minimum pool size (idle) must be >= -1")
    private Integer minimumIdle;

    @NotNull(message = "Connection timeout must be set")
    @Min(value = 1, message = "Connection timeout must be >= 1s")
    private Long connectionTimeout;

    @NotNull(message = "Validation timeout must be set")
    @Min(value = 1, message = "Validation timeout must be >= 1s")
    private Long validationTimeout;

    @NotNull(message = "Idle timeout must be set")
    @Min(value = 10, message = "Idle timeout must be >= 10s")
    private Long idleTimeout;

    @NotNull(message = "Keep alive timeout must be set")
    @Min(value = 30, message = "Keep alive timeout must be >= 30s")
    private Long keepAliveTime;

    @NotNull(message = "Max life time must be set")
    @Min(value = 30, message = "Max life time must be >= 30s")
    private Long maxLifetime;

    @NotNull(message = "Initialization fail timeout must be set")
    @Min(value = -1, message = "Initialization fail timeout must be >= -1s")
    private Long initializationFailTimeout;

    private String validationQuery;

    public ConfigModel applyConfigStrategy() {
        Objects.requireNonNull(configProfile);
        Objects.requireNonNull(configProfile.configStrategy());
        return configProfile.configStrategy().applySettings(this);
    }

    public String getPoolName() {
        return slot.getDisplayName();
    }

    public String getValidationQuery() {
        return validationQuery;
    }

    public void setValidationQuery(String validationQuery) {
        this.validationQuery = validationQuery;
    }

    public EnumSet<Slot> getSlots() {
        return slots;
    }

    public EnumSet<Slot> getOccupiedSlots() {
        List<Slot> slotList = slots
                .stream()
                .filter(Slot::isOccupied)
                .toList();
        if (slotList.isEmpty()) {
            return EnumSet.noneOf(Slot.class);
        }
        return EnumSet.copyOf(slotList);
    }

    public void setSlots(EnumSet<Slot> slots) {
        this.slots = slots;
    }

    public String getApplicationModelYaml() {
        return applicationModelYaml;
    }

    public void setApplicationModelYaml(String applicationModelYaml) {
        this.applicationModelYaml = applicationModelYaml;
    }

    public Integer getNumVCPUs() {
        return numVCPUs;
    }

    public void setNumVCPUs(Integer numVCPUs) {
        this.numVCPUs = numVCPUs;
    }

    public Integer getNumInstances() {
        return numInstances;
    }

    public void setNumInstances(Integer numInstances) {
        this.numInstances = numInstances;
    }

    public Long getConnectionLifeTimeSeconds() {
        return connectionLifeTimeSeconds;
    }

    public void setConnectionLifeTimeSeconds(Long connectionLifeTimeSeconds) {
        this.connectionLifeTimeSeconds = connectionLifeTimeSeconds;
    }

    public ConfigProfile getConfigProfile() {
        return configProfile;
    }

    public void setConfigProfile(ConfigProfile configProfile) {
        this.configProfile = configProfile;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public Isolation getIsolation() {
        return isolation;
    }

    public void setIsolation(Isolation isolation) {
        this.isolation = isolation;
    }

    public boolean isReWriteBatchedInserts() {
        return reWriteBatchedInserts;
    }

    public void setReWriteBatchedInserts(boolean reWriteBatchedInserts) {
        this.reWriteBatchedInserts = reWriteBatchedInserts;
    }

    public boolean isAutoCommit() {
        return autoCommit;
    }

    public void setAutoCommit(boolean autoCommit) {
        this.autoCommit = autoCommit;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public Multiplier getMultiplier() {
        return multiplier;
    }

    public void setMultiplier(Multiplier multiplier) {
        this.multiplier = multiplier;
    }

    public Integer getMaximumPoolSize() {
        return maximumPoolSize;
    }

    public void setMaximumPoolSize(Integer maximumPoolSize) {
        this.maximumPoolSize = maximumPoolSize;
    }

    public Integer getMinimumIdle() {
        return minimumIdle;
    }

    public void setMinimumIdle(Integer minimumIdle) {
        this.minimumIdle = minimumIdle;
    }

    public Long getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(Long connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public Long getValidationTimeout() {
        return validationTimeout;
    }

    public void setValidationTimeout(Long validationTimeout) {
        this.validationTimeout = validationTimeout;
    }

    public Long getIdleTimeout() {
        return idleTimeout;
    }

    public void setIdleTimeout(Long idleTimeout) {
        this.idleTimeout = idleTimeout;
    }

    public Long getKeepAliveTime() {
        return keepAliveTime;
    }

    public void setKeepAliveTime(Long keepAliveTime) {
        this.keepAliveTime = keepAliveTime;
    }

    public Long getMaxLifetime() {
        return maxLifetime;
    }

    public void setMaxLifetime(Long maxLifetime) {
        this.maxLifetime = maxLifetime;
    }

    public Long getInitializationFailTimeout() {
        return initializationFailTimeout;
    }

    public void setInitializationFailTimeout(Long initializationFailTimeout) {
        this.initializationFailTimeout = initializationFailTimeout;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Slot getSlot() {
        return slot;
    }

    public void setSlot(Slot slot) {
        this.slot = slot;
    }
}
