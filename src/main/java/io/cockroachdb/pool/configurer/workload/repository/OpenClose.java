package io.cockroachdb.pool.configurer.workload.repository;

import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;

public class OpenClose extends AbstractWorkload {
    public OpenClose(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    @Override
    public void run() {
        jdbcTemplate.execute((ConnectionCallback<Object>) conn -> null);
    }
}
