package io.cockroachdb.pool.configurer.model;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;

public class Problem {
    /**
     * Transient CockroachDB/PostgresSQL SQL state codes but only 40001 is safe to retry in terms
     * of non-idempotent side effects (like INSERT:s)
     */
    private static final List<String> TRANSIENT_CODES = List.of(
            "40001", "08001", "08003", "08004", "08006", "08007", "08S01", "57P01"
    );

    public static Problem of(Throwable cause) {
        String sqlState = null;
        if (cause instanceof SQLException) {
            sqlState = ((SQLException) cause).getSQLState();
        }
        return new Problem(cause.getClass().getSimpleName(),
                cause.getMessage(),
                toString(cause),
                sqlState);
    }

    private static String toString(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw, true));
        return sw.toString();
    }

    private final String className;

    private final String message;

    private final String stackTrace;

    private final String sqlState;

    private final Instant createdAt = Instant.now();

    public Problem(String className,
                   String message,
                   String stackTrace,
                   String sqlState) {
        this.className = className;
        this.message = message;
        this.stackTrace = stackTrace;
        this.sqlState = sqlState;
    }

    public boolean isTransient() {
        return sqlState != null && TRANSIENT_CODES.contains(sqlState);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getClassName() {
        return className;
    }

    public String getSQLCode() {
        return sqlState;
    }

    public String getMessage() {
        return message;
    }

    public String getStackTrace() {
        return stackTrace;
    }
}
