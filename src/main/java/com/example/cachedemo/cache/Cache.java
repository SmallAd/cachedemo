package com.example.cachedemo.cache;

public interface Cache<K, V> {
    V get(K key);

    V put(K key, V value);

    V remove(K key);

    int size();

    int maxSize();

    void resize(int maxSize);

    void evictAll();
}
