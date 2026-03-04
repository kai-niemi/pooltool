package io.cockroachdb.pool.configurer.model;

import org.springframework.context.ApplicationEvent;

public class DataSourceConfigPinnedEvent extends ApplicationEvent {
    private final ConfigModel configModel;

    public DataSourceConfigPinnedEvent(Object source, ConfigModel configModel) {
        super(source);
        this.configModel = configModel;
    }

    public ConfigModel getConfigModel() {
        return configModel;
    }
}
