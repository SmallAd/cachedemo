package com.example.cachedemo.configuration;

import com.example.cachedemo.cache.Cache;
import com.example.cachedemo.cache.Cache2QImpl;
import com.example.cachedemo.cache.CacheLRUImpl;
import com.example.cachedemo.cache.FilmCache2Q;
import com.example.cachedemo.model.Film;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class CacheConfig {

    @Value("${app.cache.size}")
    private int cacheSize;

    @Bean
    @Profile({"default", "2q"})
    public Cache<Integer, Film> get2QCache() {
        return new FilmCache2Q(cacheSize);
    }

    @Bean
    @Profile("lru")
    public Cache<Integer, Film> getLruCache() {
        return new CacheLRUImpl<>(cacheSize);
    }
}
