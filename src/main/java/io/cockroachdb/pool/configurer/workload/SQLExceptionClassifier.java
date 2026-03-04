package io.cockroachdb.pool.configurer.workload;

import java.sql.SQLException;
import java.util.List;

public interface SQLExceptionClassifier {
    /**
     * Transient CockroachDB/PostgresSQL SQL state codes but only 40001 is safe to retry in terms
     * of non-idempotent side effects (like INSERT:s)
     */
    List<String> TRANSIENT_CODES = List.of(
            "40001", "08001", "08003", "08004", "08006", "08007", "08S01", "57P01"
    );

    default boolean isTransient(SQLException ex) {
        String sqlState = ex.getSQLState();
        return sqlState != null && TRANSIENT_CODES.contains(sqlState);
    }
}
