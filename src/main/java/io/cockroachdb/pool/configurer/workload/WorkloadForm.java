package io.cockroachdb.pool.configurer.workload;

import java.util.EnumSet;

import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import io.cockroachdb.pool.configurer.model.Slot;

@Validated
public class WorkloadForm {
    @NotNull(message = "Workload type must be selected")
    private WorkloadType workloadType;

    @Min(value = 10, message = "Duration must be > 10s")
    private long duration;

    @Min(value = 0, message = "Wait time must be >= 0")
    @Max(value = 1, message = "Wait time must be <= 1")
    private double probability;

    @Min(value = 0, message = "Wait time must be >= 0")
    private long waitTime;

    @Min(value = 0, message = "Wait time variation must be >= 0")
    private long waitTimeVariation;

    @Min(value = 1, message = "Thread count must be > 0")
    private int count = 1;

    private EnumSet<Slot> slots = EnumSet.noneOf(Slot.class);

    private Slot slot;

    public double getProbability() {
        return probability;
    }

    public void setProbability(double probability) {
        this.probability = probability;
    }

    public long getWaitTime() {
        return waitTime;
    }

    public void setWaitTime(long waitTime) {
        this.waitTime = waitTime;
    }

    public long getWaitTimeVariation() {
        return waitTimeVariation;
    }

    public void setWaitTimeVariation(long waitTimeVariation) {
        this.waitTimeVariation = waitTimeVariation;
    }

    public EnumSet<Slot> getSlots() {
        return slots;
    }

    public void setSlots(EnumSet<Slot> slots) {
        this.slots = slots;
    }

    public Slot getSlot() {
        return slot;
    }

    public void setSlot(Slot slot) {
        this.slot = slot;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public WorkloadType getWorkloadType() {
        return workloadType;
    }

    public void setWorkloadType(WorkloadType workloadType) {
        this.workloadType = workloadType;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
