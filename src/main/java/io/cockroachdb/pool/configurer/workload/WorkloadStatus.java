package io.cockroachdb.pool.configurer.workload;

public enum WorkloadStatus {
    RUNNING("text-bg-info"),
    COMPLETED("text-bg-success"),
    CANCELLED("text-bg-warning"),
    FAILED("text-bg-danger");

    final String badge;

    WorkloadStatus(String badge) {
        this.badge = badge;
    }

    public String getBadge() {
        return badge;
    }
}
