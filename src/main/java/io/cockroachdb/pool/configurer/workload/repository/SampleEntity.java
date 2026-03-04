package io.cockroachdb.pool.configurer.workload.repository;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class SampleEntity implements Persistable<UUID> {
    @Transient
    private boolean isNew = true;

    @Id
    @Column("id")
    private UUID id;

    @Column("version")
    private Integer version;

    @Column("expire_at")
    private LocalDateTime expireAt;

    @Transient
    private String profile;

    protected void markNotNew() {
        this.isNew = false;
    }

    @Override
    @JsonIgnore
    public boolean isNew() {
        return isNew;
    }

    @Override
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public LocalDateTime getExpireAt() {
        return expireAt;
    }

    public void setExpireAt(LocalDateTime expireAt) {
        this.expireAt = expireAt;
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }
}

