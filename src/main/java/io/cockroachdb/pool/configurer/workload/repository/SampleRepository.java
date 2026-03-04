package io.cockroachdb.pool.configurer.workload.repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Calendar;
import java.util.Optional;
import java.util.TimeZone;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

public class SampleRepository {
    protected static final Calendar tzUTC = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

    protected static final String JSON_DATA =
            """
                    [
                      {
                        "_id": "670d522e1ad3f85502f44ecd",
                        "index": 0,
                        "guid": "5a44c0f0-a7a4-4cbc-8bdd-7d550d1e8d2a",
                        "isActive": true,
                        "balance": "$3,972.10",
                        "picture": "http://placehold.it/32x32",
                        "age": 31,
                        "eyeColor": "brown",
                        "name": "Kristi Blackburn",
                        "gender": "female",
                        "company": "INQUALA",
                        "email": "kristiblackburn@inquala.com",
                        "phone": "+1 (897) 482-3250",
                        "address": "793 Columbia Street, Westboro, Illinois, 2347",
                        "about": "Amet aliquip do cupidatat ex incididunt fugiat. Deserunt in pariatur ea do. Occaecat tempor do ad ut do Lorem non mollit occaecat enim occaecat. In non officia tempor amet pariatur est qui pariatur occaecat. Sunt sunt veniam reprehenderit commodo magna id. Consectetur ut ipsum mollit incididunt in amet sunt elit eiusmod irure ex. Pariatur aliquip aliqua voluptate est occaecat irure cillum esse.\\r\\n",
                        "registered": "2015-07-13T02:00:27 -02:00",
                        "latitude": -69.276752,
                        "longitude": 36.555489,
                        "tags": [
                          "ad",
                          "commodo",
                          "do",
                          "pariatur",
                          "non",
                          "tempor",
                          "consequat"
                        ],
                        "friends": [
                          {
                            "id": 0,
                            "name": "Edwards Richmond"
                          },
                          {
                            "id": 1,
                            "name": "Cynthia Potter"
                          },
                          {
                            "id": 2,
                            "name": "Yvonne Fowler"
                          }
                        ],
                        "greeting": "Hello, Kristi Blackburn! You have 7 unread messages.",
                        "favoriteFruit": "apple"
                      }
                    ]
                    """;

    private final JdbcTemplate jdbcTemplate;

    public SampleRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<SampleEntity> findFirst(boolean followerRead) {
        return jdbcTemplate
                .query(followerRead ? "SELECT * FROM pool_test "
                                      + "as of system time follower_read_timestamp() order by id limit 1" :
                                "SELECT * FROM pool_test order by id limit 1",
                        entityRowMapper())
                .stream()
                .findFirst();
    }

    public Optional<SampleEntity> findByNextId(UUID id, boolean followerRead) {
        return jdbcTemplate
                .query(followerRead ? "SELECT * FROM pool_test "
                                      + "as of system time follower_read_timestamp() where id > ? order by id limit 1" :
                                "SELECT * FROM pool_test where id > ? order by id limit 1",
                        entityRowMapper(), id)
                .stream()
                .findFirst();
    }

    public Optional<SampleEntity> findRandom() {
        return jdbcTemplate
                .query("SELECT * FROM pool_test ORDER BY random() limit 1",
                        entityRowMapper())
                .stream()
                .findFirst();
    }

    private RowMapper<SampleEntity> entityRowMapper() {
        return (rs, rowNum) -> {
            SampleEntity profile = new SampleEntity();
            profile.setId(rs.getObject("id", UUID.class));
            profile.setVersion(rs.getInt("version"));

            Timestamp ts = rs.getTimestamp("expire_at", tzUTC);
            profile.setExpireAt(LocalDateTime.ofInstant(Instant.ofEpochMilli(ts.getTime()), ZoneOffset.UTC));

            String payload = rs.getString("payload");
            profile.setProfile(payload);

            return profile;
        };
    }
}
