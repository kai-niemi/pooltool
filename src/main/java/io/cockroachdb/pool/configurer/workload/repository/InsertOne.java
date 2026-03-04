package io.cockroachdb.pool.configurer.workload.repository;

import java.io.StringReader;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;

public class InsertOne extends AbstractWorkload {
    public InsertOne(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    @Override
    public void run() {
        SampleEntity profile = new SampleEntity();
        profile.setExpireAt(LocalDateTime.now());
        profile.setVersion(0);
        profile.setProfile(SampleRepository.JSON_DATA);

        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(conn -> {
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO pool_test (expire_at,payload,version) "
                    + "VALUES (?,?,?) returning id::uuid",
                    PreparedStatement.RETURN_GENERATED_KEYS);
            ps.setObject(1, profile.getExpireAt());
            ps.setCharacterStream(2, new StringReader(profile.getProfile()));
            ps.setInt(3, profile.getVersion());
            return ps;
        }, keyHolder);

        profile.setId(keyHolder.getKeyAs(UUID.class));
    }
}
