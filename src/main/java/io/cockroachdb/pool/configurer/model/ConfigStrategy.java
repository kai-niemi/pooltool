package io.cockroachdb.pool.configurer.model;

@FunctionalInterface
public interface ConfigStrategy {
    ConfigModel applySettings(ConfigModel configModel);
}
