package io.cockroachdb.pool.configurer.metrics;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.search.Search;

public class TimeSeries {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final MeterRegistry meterRegistry;

    private final List<Search> pendingSearches = Collections.synchronizedList(new ArrayList<>());

    private final List<DoubleDataPoint> dataPoints = Collections.synchronizedList(new ArrayList<>());

    private final List<Meter.Id> availableMeters = Collections.synchronizedList(new ArrayList<>());

    public TimeSeries(MeterRegistry meterRegistry, List<Search> searches) {
        this.meterRegistry = meterRegistry;
        this.pendingSearches.addAll(searches);
    }

    public void registerMeters() {
        this.pendingSearches.removeIf(search -> {
            Meter meter = search.meter();
            if (meter == null) {
                return false;
            } else {
                availableMeters.add(meter.getId());
                logger.debug("Meter '%s' registered!".formatted(meter.getId()));
                return true;
            }
        });
    }

    public void snapshotDataPoints(Duration samplePeriod) {
        registerMeters();

        // Purge old data points older than sample period
        dataPoints.removeIf(dataPoint -> dataPoint.getInstant()
                .isBefore(Instant.now().minusSeconds(samplePeriod.toSeconds())));

        // Add new datapoint by sampling all defined metrics
        DoubleDataPoint dataPoint = new DoubleDataPoint(Instant.now());

        meterRegistry.getMeters()
                .stream()
                .filter(meter -> {
                    final Meter.Id id = meter.getId();
                    return availableMeters.stream().anyMatch(x -> x.equals(id));
                })
                .forEach(meter -> {
                    final Meter.Id id = meter.getId();
                    meter.measure().forEach(measurement ->
                            dataPoint.putValue(id.getName(), measurement.getValue()));
                });

        dataPoints.add(dataPoint);
    }

    public List<Map<String, Object>> getDataPoints() {
        final List<Map<String, Object>> columnData = new ArrayList<>();

        {
            List<Long> labels = dataPoints.stream()
                    .map(DoubleDataPoint::getInstant)
                    .toList()
                    .stream()
                    .map(Instant::toEpochMilli)
                    .toList();

            Map<String, Object> headerElement = new HashMap<>();
            headerElement.put("data", labels.toArray());

            columnData.add(headerElement);
        }

        meterRegistry.getMeters()
                .stream()
                .filter(meter -> {
                    final Meter.Id id = meter.getId();
                    return availableMeters.stream().anyMatch(pair -> pair.equals(id));
                })
                .forEach(meter -> {
                    final Meter.Id id = meter.getId();

                    List<Double> data = dataPoints
                            .stream()
                            .filter(dataPoint -> !dataPoint.isExpired())
                            .map(dataPoint -> dataPoint.getValue(id.getName(), .0))
                            .toList();

                    Map<String, Object> dataElement = new HashMap<>();
                    dataElement.put("data", data.toArray());
                    dataElement.put("id", id.getName());
                    dataElement.put("name", "%s".formatted(id.getDescription()));

                    columnData.add(dataElement);
                });

        return columnData;
    }
}
