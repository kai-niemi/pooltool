package io.cockroachdb.pool.configurer.model;

import org.springframework.validation.annotation.Validated;

import com.fasterxml.jackson.annotation.JsonProperty;

@Validated
public class RootModel {
    public static RootModel of(DataSourceModel dataSourceModel) {
        SpringModel springModel = new SpringModel(dataSourceModel);
        return new RootModel(springModel);
    }

    @JsonProperty("spring")
    private SpringModel springModel;

    public RootModel(SpringModel springModel) {
        this.springModel = springModel;
    }
}
