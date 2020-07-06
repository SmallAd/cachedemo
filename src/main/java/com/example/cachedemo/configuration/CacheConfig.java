package com.example.cachedemo.configuration;

import com.example.cachedemo.cache.Cache;
import com.example.cachedemo.cache.Cache2QImpl;
import com.example.cachedemo.model.Film;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CacheConfig {

    @Value("${app.cache.size}")
    private int cacheSize;

    @Bean
    public Cache<Integer, Film> getCache() {
        return new Cache2QImpl<>(cacheSize);
    }
}
