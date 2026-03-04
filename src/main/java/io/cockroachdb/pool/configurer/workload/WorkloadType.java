package io.cockroachdb.pool.configurer.workload;

import org.springframework.jdbc.core.JdbcTemplate;

import io.cockroachdb.pool.configurer.workload.repository.FullScan;
import io.cockroachdb.pool.configurer.workload.repository.InsertBatch;
import io.cockroachdb.pool.configurer.workload.repository.InsertOne;
import io.cockroachdb.pool.configurer.workload.repository.OpenClose;
import io.cockroachdb.pool.configurer.workload.repository.PointRead;
import io.cockroachdb.pool.configurer.workload.repository.SelectOne;
import io.cockroachdb.pool.configurer.workload.repository.SelectVersion;

public enum WorkloadType {
    open_close("Open close",
            "Open and close connections") {
        @Override
        public Runnable createWorkload(JdbcTemplate jdbcTemplate) {
            return new OpenClose(jdbcTemplate);
        }
    },
    singleton_insert("Singleton insert",
            "Single insert statements") {
        @Override
        public Runnable createWorkload(JdbcTemplate jdbcTemplate) {
            return new InsertOne(jdbcTemplate);
        }
    },
    batch_insert("Batch insert",
            "Batch of 32 insert statements") {
        @Override
        public Runnable createWorkload(JdbcTemplate jdbcTemplate) {
            return new InsertBatch(jdbcTemplate);
        }
    },
    point_read("Point read",
            "Single point read") {
        @Override
        public Runnable createWorkload(JdbcTemplate jdbcTemplate) {
            return new PointRead(jdbcTemplate, false);
        }
    },
    point_read_historical("Historical point read",
            "Single point exact staleness read") {
        @Override
        public Runnable createWorkload(JdbcTemplate jdbcTemplate) {
            return new PointRead(jdbcTemplate, true);
        }
    },
    full_scan("Full table scan",
            "Full table scan using order by random") {
        @Override
        public Runnable createWorkload(JdbcTemplate jdbcTemplate) {
            return new FullScan(jdbcTemplate);
        }
    },
    select_one("Select one query",
            "select 1") {
        @Override
        public Runnable createWorkload(JdbcTemplate jdbcTemplate) {
            return new SelectOne(jdbcTemplate);
        }
    },
    select_version("Select version query",
            "select version()") {
        @Override
        public Runnable createWorkload(JdbcTemplate jdbcTemplate) {
            return new SelectVersion(jdbcTemplate);
        }
    };

    private final String displayValue;

    private final String description;

    WorkloadType(String displayValue,
                 String description) {
        this.displayValue = displayValue;
        this.description = description;
    }

    public String getDisplayValue() {
        return displayValue;
    }

    public String getDescription() {
        return description;
    }

    public abstract Runnable createWorkload(JdbcTemplate jdbcTemplate);
}
