package io.cockroachdb.pool.configurer.workload.repository;

import org.springframework.jdbc.core.JdbcTemplate;

public class SelectVersion extends AbstractWorkload {
    public SelectVersion(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    @Override
    public void run() {
        jdbcTemplate.queryForObject("select version()", String.class);
    }
}

