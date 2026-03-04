package io.cockroachdb.pool.configurer.web;

import java.sql.SQLException;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.zaxxer.hikari.HikariDataSource;

import io.cockroachdb.pool.configurer.metrics.Metrics;
import io.cockroachdb.pool.configurer.metrics.MetricsRegistrator;
import io.cockroachdb.pool.configurer.metrics.TimeSeries;
import io.cockroachdb.pool.configurer.model.DataSourceCreatedEvent;
import io.cockroachdb.pool.configurer.model.Slot;
import io.cockroachdb.pool.configurer.web.support.MessagePublisher;
import io.cockroachdb.pool.configurer.web.support.TopicName;
import io.cockroachdb.pool.configurer.workload.WorkloadManager;

/**
 * Chart JS data paint callback methods.
 */
@WebController
@RequestMapping(value = "/chart")
public class ChartController {
    @Autowired
    @Qualifier("threadPoolTimeSeries")
    private TimeSeries threadPoolTimeSeries;

    @Autowired
    @Qualifier("cpuTimeSeries")
    private TimeSeries cpuTimeSeries;

    @Autowired
    @Qualifier("memoryTimeSeries")
    private TimeSeries memoryTimeSeries;

    @Autowired
    @Qualifier("workloadTimeSeries")
    private TimeSeries workloadTimeSeries;

    @Autowired
    private WorkloadManager workloadManager;

    @Autowired
    private MessagePublisher messagePublisher;

    @Autowired
    private MetricsRegistrator metricsRegistrator;

    private final Map<String, TimeSeries> connectionPoolGaugeTimeSeries = new HashMap<>();

    private final Map<String, TimeSeries> connectionPoolTimeTimeSeries = new HashMap<>();

    private final Duration samplePeriod = Duration.ofMinutes(5);

    @Scheduled(fixedRate = 5, initialDelay = 1, timeUnit = TimeUnit.SECONDS)
    public void updateCharts() {
        messagePublisher.convertAndSend(TopicName.CHART_POOL_UPDATE, null);
        messagePublisher.convertAndSend(TopicName.CHART_VM_UPDATE, null);
        messagePublisher.convertAndSend(TopicName.CHART_WORKLOAD_UPDATE, null);
    }

    @Scheduled(fixedRate = 5, initialDelay = 5, timeUnit = TimeUnit.SECONDS)
    public void updateDataPoints() {
        workloadManager.snapshotDataPoints(samplePeriod);
        threadPoolTimeSeries.snapshotDataPoints(samplePeriod);
        cpuTimeSeries.snapshotDataPoints(samplePeriod);
        memoryTimeSeries.snapshotDataPoints(samplePeriod);
        workloadTimeSeries.snapshotDataPoints(samplePeriod);

        connectionPoolGaugeTimeSeries.values()
                .forEach(timeSeries -> timeSeries.snapshotDataPoints(samplePeriod));
        connectionPoolTimeTimeSeries.values()
                .forEach(timeSeries -> timeSeries.snapshotDataPoints(samplePeriod));
    }

    @EventListener
    public void handle(DataSourceCreatedEvent event) {
        try {
            HikariDataSource dataSource = event.getDataSource().unwrap(HikariDataSource.class);
            Objects.requireNonNull(dataSource.getPoolName(), "pool name is required");

            connectionPoolGaugeTimeSeries.put(dataSource.getPoolName().toLowerCase(),
                    metricsRegistrator.registerDataSourceGaugeMeters(dataSource));

            connectionPoolTimeTimeSeries.put(dataSource.getPoolName().toLowerCase(),
                    metricsRegistrator.registerDataSourceTimeMeters(dataSource));
        } catch (SQLException e) {
            throw new ApplicationContextException("", e);
        }
    }

    @GetMapping("/pool")
    public Callable<String> getPoolChartsPage(@RequestParam("name") String name,
                                              Model model) {
        model.addAttribute("name", Slot.valueOf(name).getDisplayName());
        return () -> "pool-charts";
    }

    @GetMapping(value = "/pool/data-points/gauge",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody List<Map<String, Object>> getConnectionPoolGaugeDataPoints(
            @RequestParam("name") String name) {
        name = name.toLowerCase();
        if (connectionPoolGaugeTimeSeries.containsKey(name)) {
            return connectionPoolGaugeTimeSeries.get(name).getDataPoints();
        }
        return List.of();
    }

    @GetMapping(value = "/pool/data-points/time",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody List<Map<String, Object>> getConnectionPoolTimeDataPoints(
            @RequestParam("name") String name) {
        name = name.toLowerCase();
        if (connectionPoolTimeTimeSeries.containsKey(name)) {
            return connectionPoolTimeTimeSeries.get(name).getDataPoints();
        }
        return List.of();
    }

    @GetMapping("/vm")
    public Callable<String> getVMChartsPage(Model model) {
        return () -> "vm-charts";
    }

    @GetMapping(value = "/vm/thread/data-points",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody List<Map<String, Object>> getThreadPoolDataPoints() {
        return threadPoolTimeSeries.getDataPoints();
    }

    @GetMapping(value = "/vm/cpu/data-points",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody List<Map<String, Object>> getCpuDataPoints() {
        return cpuTimeSeries.getDataPoints();
    }

    @GetMapping(value = "/vm/mem/data-points",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody List<Map<String, Object>> getMemDataPoints() {
        return memoryTimeSeries.getDataPoints();
    }

    @GetMapping("/workload")
    public Callable<String> getWorkloadChartsPage(Model model) {
        return () -> "workload-charts";
    }

    @GetMapping(value = "/workload/data-points",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody List<Map<String, Object>> getWorkloadDataPoints() {
        return workloadTimeSeries.getDataPoints();
    }

    @GetMapping(value = "/workload/data-points/p99",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody List<Map<String, Object>> getWorkloadDataPointsP99(Pageable page) {
        return workloadManager.getDataPoints(Metrics::getP99, page);
    }

    @GetMapping(value = "/workload/data-points/p999",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody List<Map<String, Object>> getWorkloadDataPointsP999(Pageable page) {
        return workloadManager.getDataPoints(Metrics::getP999, page);
    }

    @GetMapping(value = "/workload/data-points/tps",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody List<Map<String, Object>> getWorkloadDataPointsTPS(Pageable page) {
        return workloadManager.getDataPoints(Metrics::getOpsPerSec, page);
    }
}
