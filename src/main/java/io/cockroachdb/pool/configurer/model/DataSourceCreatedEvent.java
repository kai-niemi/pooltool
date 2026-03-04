package io.cockroachdb.pool.configurer.model;

import javax.sql.DataSource;

import org.springframework.context.ApplicationEvent;

public class DataSourceCreatedEvent extends ApplicationEvent {
    private final DataSource dataSource;

    public DataSourceCreatedEvent(Object source, DataSource dataSource) {
        super(source);
        this.dataSource = dataSource;
    }

    public DataSource getDataSource() {
        return dataSource;
    }
}

