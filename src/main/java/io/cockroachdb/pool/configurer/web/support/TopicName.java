package io.cockroachdb.pool.configurer.web.support;

public enum TopicName {
    TOAST_MESSAGE("/topic/toast"),

    EDITOR_REFRESH("/topic/editor/refresh"),

    WORKLOAD_UPDATE("/topic/workload/update"),
    WORKLOAD_REFRESH("/topic/workload/refresh"),

    CHART_WORKLOAD_UPDATE("/topic/chart/workload/update"),
    CHART_VM_UPDATE("/topic/chart/vm/update"),
    CHART_POOL_UPDATE("/topic/chart/pool/update");

    final String value;

    TopicName(java.lang.String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
