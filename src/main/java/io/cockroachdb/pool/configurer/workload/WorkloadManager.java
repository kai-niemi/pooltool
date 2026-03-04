package io.cockroachdb.pool.configurer.workload;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.IntStream;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import io.cockroachdb.pool.configurer.config.ClosableDataSource;
import io.cockroachdb.pool.configurer.metrics.Metrics;
import io.cockroachdb.pool.configurer.metrics.MetricsDataPoint;
import io.cockroachdb.pool.configurer.model.ConfigModel;
import io.cockroachdb.pool.configurer.model.DataSourceCreatedEvent;
import io.cockroachdb.pool.configurer.model.DataSourceModel;
import io.cockroachdb.pool.configurer.model.HikariConfigModel;

/**
 * Manager for background workloads and time series data points for call metrics.
 */
@Component
public class WorkloadManager {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final List<WorkloadModel> workloadModels = Collections.synchronizedList(new LinkedList<>());

    private final List<MetricsDataPoint> dataPoints = Collections.synchronizedList(new ArrayList<>());

    @Autowired
    private WorkloadExecutor workloadExecutor;

    @Autowired
    private Function<DataSourceModel, ClosableDataSource> dataSourceFactory;

    @Autowired
    private Function<DataSource, PlatformTransactionManager> transactionManagerFactory;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    public void submitWorkloads(WorkloadForm workloadForm,
                                ConfigModel configModel) {

        try (ClosableDataSource dataSource = dataSourceFactory.apply(DataSourceModel.builder()
                .withHikariConfig(HikariConfigModel.toHikariModel(configModel))
                .withDriverClassName("org.postgresql.Driver")
                .withUrl(configModel.getUrl())
                .withUsername(configModel.getUserName())
                .withPassword(configModel.getPassword())
                .withTraceLogging(false)
                .withName(configModel.getPoolName())
                .build())) {
            initSchema(dataSource);
        }

        final DataSource dataSource = dataSourceFactory.apply(DataSourceModel.builder()
                .withHikariConfig(HikariConfigModel.toHikariModel(configModel))
                .withDriverClassName("org.postgresql.Driver")
                .withUrl(configModel.getUrl())
                .withUsername(configModel.getUserName())
                .withPassword(configModel.getPassword())
                .withTraceLogging(false)
                .withName(configModel.getPoolName())
                .withProbability(workloadForm.getProbability())
                .withWaitTime(workloadForm.getWaitTime())
                .withWaitTimeVariation(workloadForm.getWaitTimeVariation())
                .build());

        final TransactionTemplate transactionTemplate = new TransactionTemplate(
                transactionManagerFactory.apply(dataSource));
        transactionTemplate.setIsolationLevel(configModel.getIsolation().value());

        IntStream.rangeClosed(1, workloadForm.getCount())
                .forEach(value -> {
                    final WorkloadType workloadType = workloadForm.getWorkloadType();

                    final Runnable workloadTask = workloadType.createWorkload(
                            new JdbcTemplate(dataSource));

                    final Runnable transactionWrapper = () ->
                            transactionTemplate.executeWithoutResult(
                                    transactionStatus -> workloadTask.run());

                    WorkloadModel workloadModel = workloadExecutor
                            .submitWorkloadTask(
                                    workloadType.getDescription(),
                                    configModel.getPoolName(),
                                    Duration.ofSeconds(workloadForm.getDuration()),
                                    transactionWrapper);

                    workloadModels.add(workloadModel);

                    logger.info("Submitted %d/%d workloads".formatted(value, workloadForm.getCount()));
                });

        fireUpdatedEvent();

        applicationEventPublisher.publishEvent(new DataSourceCreatedEvent(this, dataSource));
    }

    private void initSchema(DataSource dataSource) {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.setCommentPrefix("--");
        populator.setIgnoreFailedDrops(true);
        populator.addScript(new ClassPathResource("db/create.sql"));

        DatabasePopulatorUtils.execute(populator, dataSource);
    }

    public List<WorkloadModel> getWorkloads() {
        return Collections.unmodifiableList(new LinkedList<>(workloadModels));
    }

    public List<WorkloadModel> getWorkloads(Predicate<WorkloadModel> predicate) {
        return new ArrayList<>(workloadModels.stream()
                .filter(predicate)
                .toList());
    }

    public Page<WorkloadModel> getWorkloads(Pageable pageable, Predicate<WorkloadModel> predicate) {
        List<WorkloadModel> copy = getWorkloads();
        List<WorkloadModel> content = new ArrayList<>(copy.stream()
                .filter(predicate)
                .skip(pageable.getOffset())
                .limit(pageable.getPageSize())
                .toList());
        int total = copy.size();
        return PageableExecutionUtils.getPage(content, pageable, () -> total);
    }

    public WorkloadModel getWorkloadById(Integer id) {
        return workloadModels
                .stream()
                .filter(workload -> Objects.equals(workload.getId(), id))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException("No workload with id: " + id));
    }

    public void deleteWorkloads() {
        workloadModels.removeIf(workload -> !workload.isRunning());

        fireUpdatedEvent();
    }

    public void deleteWorkload(Integer id) {
        WorkloadModel workloadModel = getWorkloadById(id);
        if (workloadModel.isRunning()) {
            throw new IllegalStateException("Workload is running: " + id);
        }

        workloadModels.remove(workloadModel);

        fireUpdatedEvent();
    }

    public void cancelWorkloads() {
        workloadModels.forEach(workload -> cancelWorkload(workload.getId()));
    }

    public void cancelWorkload(Integer id) {
        getWorkloadById(id).cancel();
    }

    private void fireUpdatedEvent() {
        applicationEventPublisher.publishEvent(new WorkloadUpdatedEvent(this));
    }

    // Time series functions

    public Metrics getMetricsAggregate(Pageable page) {
        List<Metrics> metrics = getWorkloads(page, workloadModel -> true)
                .stream()
                .map(WorkloadModel::getMetrics).toList();
        return Metrics.builder()
                .withUpdateTime(Instant.now())
                .withMeanTimeMillis(metrics.stream()
                        .mapToDouble(Metrics::getMeanTimeMillis).average().orElse(0))
                .withOps(metrics.stream().mapToDouble(Metrics::getOpsPerSec).sum(),
                        metrics.stream().mapToDouble(Metrics::getOpsPerMin).sum())
                .withP50(metrics.stream().mapToDouble(Metrics::getP50).average().orElse(0))
                .withP90(metrics.stream().mapToDouble(Metrics::getP90).average().orElse(0))
                .withP95(metrics.stream().mapToDouble(Metrics::getP95).average().orElse(0))
                .withP99(metrics.stream().mapToDouble(Metrics::getP99).average().orElse(0))
                .withP999(metrics.stream().mapToDouble(Metrics::getP999).average().orElse(0))
                .withMeanTimeMillis(metrics.stream().mapToDouble(Metrics::getMeanTimeMillis).average().orElse(0))
                .withSuccessful(metrics.stream().mapToInt(Metrics::getSuccess).sum())
                .withFails(metrics.stream().mapToInt(Metrics::getTransientFail).sum(),
                        metrics.stream().mapToInt(Metrics::getNonTransientFail).sum())
                .build();
    }

    public void snapshotDataPoints(Duration samplePeriod) {
        // Purge old data points older than sample period
        dataPoints.removeIf(item -> item.getInstant()
                .isBefore(Instant.now().minusSeconds(samplePeriod.toSeconds())));

        // Add new datapoint by sampling all workload metrics
        MetricsDataPoint dataPoint = new MetricsDataPoint(Instant.now());

        // Add datapoint if still running
        getWorkloads()
                .stream()
                .filter(WorkloadModel::isRunning)
                .forEach(workload -> dataPoint.putValue(workload.getId(), workload.getMetrics()));

        dataPoints.add(dataPoint);
    }

    public List<Map<String, Object>> getDataPoints(Function<Metrics, Double> mapper, Pageable page) {
        final List<Map<String, Object>> columnData = new ArrayList<>();

        {
            final Map<String, Object> headerElement = new HashMap<>();
            List<Long> labels = dataPoints
                    .stream()
                    .map(MetricsDataPoint::getInstant)
                    .toList()
                    .stream()
                    .map(Instant::toEpochMilli)
                    .toList();
            headerElement.put("data", labels.toArray());
            columnData.add(headerElement);
        }

        getWorkloads(page, (x) -> true)
                .forEach(workload -> {
                    Map<String, Object> dataElement = new HashMap<>();

                    List<Metrics> metrics = new ArrayList<>();

                    dataPoints.forEach(dataPoint -> metrics.add(
                            dataPoint.getValue(workload.getId(), Metrics.empty())));

                    List<Double> data = metrics
                            .stream()
                            .map(mapper)
                            .toList();

                    dataElement.put("id", workload.getId());
                    dataElement.put("name", "#%d".formatted(workload.getId()));
                    dataElement.put("data", data.toArray());

                    columnData.add(dataElement);
                });

        return columnData;
    }
}
