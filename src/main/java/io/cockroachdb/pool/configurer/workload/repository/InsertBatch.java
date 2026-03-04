package io.cockroachdb.pool.configurer.workload.repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import org.springframework.jdbc.core.JdbcTemplate;

public class InsertBatch extends AbstractWorkload {
    public InsertBatch(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    @Override
    public void run() {
        List<SampleEntity> profileBatch = new ArrayList<>();

        int batchSize = 32;

        IntStream.rangeClosed(1, batchSize).forEach(value -> {
            SampleEntity profile = new SampleEntity();
            profile.setId(UUID.randomUUID());
            profile.setExpireAt(LocalDateTime.now());
            profile.setVersion(0);
            profile.setProfile(SampleRepository.JSON_DATA);
            profileBatch.add(profile);
        });

        jdbcTemplate.update(
                "INSERT INTO pool_test (id,expire_at,payload,version) "
                + "select unnest(?) as id, "
                + "       unnest(?) as expire_at, "
                + "       unnest(?) as payload, "
                + "       unnest(?) as version",
                ps -> {
                    List<UUID> id = new ArrayList<>();
                    List<LocalDateTime> expire_at = new ArrayList<>();
                    List<String> payload = new ArrayList<>();
                    List<Integer> version = new ArrayList<>();

                    profileBatch.forEach(profile -> {
                        id.add(profile.getId());
                        expire_at.add(profile.getExpireAt());
                        payload.add(profile.getProfile());
                        version.add(profile.getVersion());
                    });

                    ps.setArray(1, ps.getConnection()
                            .createArrayOf("UUID", id.toArray()));
                    ps.setArray(2, ps.getConnection()
                            .createArrayOf("TIMESTAMP", expire_at.toArray()));
                    ps.setArray(3, ps.getConnection()
                            .createArrayOf("JSONB", payload.toArray()));
                    ps.setArray(4, ps.getConnection()
                            .createArrayOf("INT", version.toArray()));
                });
    }
}

