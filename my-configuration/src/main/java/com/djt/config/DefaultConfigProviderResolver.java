package com.djt.config;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class DefaultConfigProviderResolver extends ConfigProviderResolver {

    private ConcurrentMap<ClassLoader, Config> configsRepository = new ConcurrentHashMap<>();

    @Override
    public Config getConfig() {
        return getConfig(null);
    }

    @Override
    public Config getConfig(ClassLoader loader) {
        return configsRepository.computeIfAbsent(loader, this::newConfig);
    }

    private ClassLoader resolveClassLoader(ClassLoader classLoader) {
        return classLoader == null ? this.getClass().getClassLoader() : classLoader;
    }


    @Override
    public ConfigBuilder getBuilder() {
        return newConfigBuilder(null);
    }

    protected ConfigBuilder newConfigBuilder(ClassLoader classLoader) {
        return new DefaultConfigBuilder(resolveClassLoader(classLoader));
    }

    protected Config newConfig(ClassLoader classLoader) {
        return newConfigBuilder(classLoader).build();
    }

    @Override
    public void registerConfig(Config config, ClassLoader classLoader) {
        configsRepository.put(classLoader, config);
    }

    @Override
    public void releaseConfig(Config config) {
        List<ClassLoader> targetKeys = new LinkedList<>();
        for (Map.Entry<ClassLoader, Config> entry : configsRepository.entrySet()) {
            if (Objects.equals(config, entry.getValue())) {
                targetKeys.add(entry.getKey());
            }
        }
        targetKeys.forEach(configsRepository::remove);
    }
}
