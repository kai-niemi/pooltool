package io.cockroachdb.pool.configurer.metrics;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.zaxxer.hikari.HikariDataSource;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.search.Search;

@Component
public class MetricsRegistrator {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final String METRIC_CATEGORY = "pool";

    private static final String METRIC_NAME_WAIT = "hikaricp.connections.acquire";

    private static final String METRIC_NAME_USAGE = "hikaricp.connections.usage";

    private static final String METRIC_NAME_CONNECT = "hikaricp.connections.creation";

    private static final String METRIC_NAME_TIMEOUT_RATE = "hikaricp.connections.timeout";

    private static final String METRIC_NAME_TOTAL_CONNECTIONS = "hikaricp.connections";

    private static final String METRIC_NAME_IDLE_CONNECTIONS = "hikaricp.connections.idle";

    private static final String METRIC_NAME_ACTIVE_CONNECTIONS = "hikaricp.connections.active";

    private static final String METRIC_NAME_PENDING_CONNECTIONS = "hikaricp.connections.pending";

    private static final String METRIC_NAME_MAX_CONNECTIONS = "hikaricp.connections.max";

    private static final String METRIC_NAME_MIN_CONNECTIONS = "hikaricp.connections.min";

    @Autowired
    private MeterRegistry meterRegistry;

    public TimeSeries registerDataSourceGaugeMeters(HikariDataSource dataSource) {
        return new TimeSeries(meterRegistry, List.of(
                findMeter(METRIC_NAME_TOTAL_CONNECTIONS, dataSource.getPoolName()),
                findMeter(METRIC_NAME_TIMEOUT_RATE, dataSource.getPoolName()),
                findMeter(METRIC_NAME_TOTAL_CONNECTIONS, dataSource.getPoolName()),
                findMeter(METRIC_NAME_IDLE_CONNECTIONS, dataSource.getPoolName()),
                findMeter(METRIC_NAME_ACTIVE_CONNECTIONS, dataSource.getPoolName()),
                findMeter(METRIC_NAME_PENDING_CONNECTIONS, dataSource.getPoolName()),
                findMeter(METRIC_NAME_MAX_CONNECTIONS, dataSource.getPoolName()),
                findMeter(METRIC_NAME_MIN_CONNECTIONS, dataSource.getPoolName())
        ));
    }

    /**
     * @see com.zaxxer.hikari.metrics.micrometer.MicrometerMetricsTracker
     */
    public TimeSeries registerDataSourceTimeMeters(HikariDataSource dataSource) {
        return new TimeSeries(meterRegistry, List.of(
                findMeter(METRIC_NAME_WAIT, dataSource.getPoolName()),
                findMeter(METRIC_NAME_USAGE, dataSource.getPoolName()),
                findMeter(METRIC_NAME_CONNECT, dataSource.getPoolName())
        ));
    }

    private Search findMeter(String name, String poolName) {
        Search s = meterRegistry.find(name).tag(METRIC_CATEGORY, poolName);
        Meter m = s.meter();
        if (m == null) {
            logger.warn("No meter '%s' found for pool '%s'".formatted(name, poolName));
        }
        return s;
    }

}
