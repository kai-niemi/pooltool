package io.cockroachdb.pool.configurer.workload.repository;

import org.springframework.jdbc.core.JdbcTemplate;

public class PointRead extends AbstractWorkload {
    private final boolean followerRead;

    public PointRead(JdbcTemplate jdbcTemplate, boolean followerRead) {
        super(jdbcTemplate);
        this.followerRead = followerRead;
    }

    @Override
    public void run() {
        findNext(followerRead).ifPresent(sampleEntity -> {
        });
    }
}
