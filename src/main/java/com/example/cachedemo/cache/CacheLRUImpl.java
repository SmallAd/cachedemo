package com.example.cachedemo.cache;

import java.util.LinkedHashMap;

public class CacheLRUImpl<K, V> implements Cache<K, V> {

    private final LinkedHashMap<K, V> map;
    private int maxSize;
    private int size;

    //variables for statistic
    private int hitCount;
    private int missCount;

    public CacheLRUImpl(int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSize <= 0");
        }

        this.map = new LinkedHashMap<>();
        this.maxSize = maxSize;
        size = 0;
    }

    @Override
    public V get(K key) {
        if (key == null) {
            throw new NullPointerException("key == null");
        }

        synchronized (this) {
            V value = map.get(key);
            if (value != null) {
                hitCount++;
                // add & trim (LRU)
                map.remove(key);
                map.put(key, value);
                return value;
            }

            missCount++;
        }
        return null;
    }

    @Override
    public V put(K key, V value) {
        if (key == null || value == null) {
            throw new NullPointerException("key == null || value == null");
        }
        V result;
        synchronized (this) {

            if (map.containsKey(key)) {
                V oldValue = map.remove(key);
                size -= safeSizeOf(key, oldValue);
            }
                result = map.put(key, value);
                size += safeSizeOf(key, value);
                trimMap();
        }
        return result;
    }

    @Override
    public V remove(K key) {
        if (key == null) {
            throw new NullPointerException("key == null");
        }

        synchronized (this) {
            V removedValue =  map.remove(key);
            size -= safeSizeOf(key, removedValue);
            return removedValue;
        }
    }

    @Override
    public synchronized int size() {
        return size;
    }

    @Override
    public synchronized int maxSize() {
        return maxSize;
    }

    @Override
    public void resize(int maxSize) {
        this.maxSize = maxSize;

        synchronized (this) {
            LinkedHashMap<K, V> copy = new LinkedHashMap<>(map);
            evictAll();
            for (K key : copy.keySet()) {
                put(key, copy.get(key));
            }
        }
    }

    @Override
    public synchronized void evictAll() {
        map.clear();
        size = 0;
    }

    private void trimMap() {
        while (true) {

            if (size <= maxSize || map.isEmpty()) {
                break;
            }

            K key = map.keySet().iterator().next();
            V oldValue = map.remove(key);
            size -= safeSizeOf(key, oldValue);
        }
    }

    private int safeSizeOf(K key, V value) {
        int result = sizeOf(key, value);
        if (result < 0) {
            throw new IllegalStateException("Negative size: " + key + "=" + value);
        }
        return result;
    }

    /**
     * Returns the size of the entry in user-defined units.  The default
     * implementation returns 1 so that size is the number of entries and
     * max size is the maximum number of entries.
     */
    protected int sizeOf(K key, V value) {
        return 1;
    }

    public int hitCount() {
        return hitCount;
    }

    public int missCount() {
        return missCount;
    }

    @Override
    public synchronized final String toString() {
        int accesses = hitCount() + missCount();
        int hitPercent = accesses != 0 ? (100 * hitCount() / accesses) : 0;
        return String.format("LRU Cache[size=%d,maxSize=%d,hits=%d,misses=%d,hitRate=%d%%," +
                        "]",
                size(), maxSize(), hitCount(), missCount(), hitPercent)
                + "\n map:" + map.toString();
    }
}
