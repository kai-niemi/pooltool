package io.cockroachdb.pool.configurer.workload.repository;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.jdbc.core.JdbcTemplate;

public abstract class AbstractWorkload implements Runnable {
    protected final JdbcTemplate jdbcTemplate;

    protected final SampleRepository sampleRepository;

    private final AtomicReference<Optional<SampleEntity>> latestEntity
            = new AtomicReference<>(Optional.empty());

    protected AbstractWorkload(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.sampleRepository = new SampleRepository(jdbcTemplate);
    }

    protected Optional<SampleEntity> findNext(boolean followerRead) {
        Optional<SampleEntity> e = latestEntity.get();
        if (e.isPresent()) {
            e = sampleRepository.findByNextId(e.get().getId(), followerRead);
        }
        if (e.isEmpty()) {
            e = sampleRepository.findFirst(followerRead);
        }
        latestEntity.set(e);
        return e;
    }
}

