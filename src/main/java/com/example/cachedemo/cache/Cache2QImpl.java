package com.example.cachedemo.cache;

import java.util.*;

public class Cache2QImpl<K, V> implements Cache<K, V> {
    private final HashMap<K, V> map;
    private final LinkedHashSet<K> mapIn, mapOut, mapHot;

    private int sizeIn;
    private int sizeOut;
    private int sizeHot;

    private int maxSizeIn;
    private int maxSizeOut;
    private int maxSizeHot;

    //variables for statistic
    private int hitCount;
    private int missCount;

    public Cache2QImpl(int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSize <= 0");
        }

        calcMaxSizes(maxSize);

        map = new HashMap<>(0, 0.75f);

        mapIn = new LinkedHashSet<>();
        mapOut = new LinkedHashSet<>();
        mapHot = new LinkedHashSet<>();
    }

    /**
     * Sets the size of the cache.
     */
    @Override
    public void resize(int maxSize) {

        calcMaxSizes(maxSize);
        synchronized (this) {
            HashMap<K, V> copy = new HashMap<>(map);
            evictAll();
            for (K key : copy.keySet()) {
                put(key, copy.get(key));
            }
        }
    }

    /**
     * Returns the value if it exists in the cache.
     * This returns null if a value is not cached.
     */
    @Override
    public V get(K key) {
        if (key == null) {
            throw new NullPointerException("key == null");
        }

        V mapValue;
        synchronized (this) {
            mapValue = map.get(key);
            if (mapValue != null) {
                hitCount++;
                if (mapHot.contains(key)) {
                    // add & trim (LRU)
                    mapHot.remove(key);
                    mapHot.add(key);
                } else {
                    if (mapOut.contains(key)) {
                        mapHot.add(key);
                        sizeHot += safeSizeOf(key, mapValue);
                        trimMapHot();
                        sizeOut -= safeSizeOf(key, mapValue);
                        mapOut.remove(key);
                    }
                }
                return mapValue;
            }
            missCount++;
        }
        return null;
    }

    /**
     *  Cache value
     */
    @Override
    public V put(K key, V value) {
        if (key == null || value == null) {
            throw new NullPointerException("key == null || value == null");
        }

        if (map.containsKey(key)) {
            synchronized (this) {
                V oldValue = map.get(key);
                if (mapIn.contains(key)) {
                    sizeIn -= safeSizeOf(key, oldValue);
                    sizeIn += safeSizeOf(key, value);
                }
                if (mapOut.contains(key)) {
                    sizeOut -= safeSizeOf(key, oldValue);
                    sizeOut += safeSizeOf(key, value);
                }
                if (mapHot.contains(key)) {
                    sizeHot -= safeSizeOf(key, oldValue);
                    sizeHot += safeSizeOf(key, value);
                }
            }
            return map.put(key, value);
        }
        V result;
        synchronized (this) {
            final int sizeOfValue = safeSizeOf(key, value);
            //if there are free page slots then put value into a free page slot
            boolean hasFreeSlot = add2slot(key, safeSizeOf(key, value));
            if (hasFreeSlot) {
                map.put(key, value);
                result = value;
            } else {
                // no free slot, go to trim mapIn/mapOut
                if (trimMapIn(sizeOfValue)) {
                    map.put(key, value);
                    mapIn.add(key);
                    result = value;
                } else {
                    map.put(key, value);
                    mapHot.add(key);
                    sizeHot += safeSizeOf(key, value);
                    trimMapHot();
                    result = value;
                }
            }

        }
        return result;
    }

    /**
     * Removes the entry if it exists.
     */
    public V remove(K key) {
        if (key == null) {
            throw new NullPointerException("key == null");
        }

        V previous;
        synchronized (this) {
            previous = map.remove(key);
            if (previous != null) {
                if (mapIn.contains(key)) {
                    sizeIn -= safeSizeOf(key, previous);
                    mapIn.remove(key);
                }
                if (mapOut.contains(key)) {
                    sizeOut -= safeSizeOf(key, previous);
                    mapOut.remove(key);
                }
                if (mapHot.contains(key)) {
                    sizeHot -= safeSizeOf(key, previous);
                    mapHot.remove(key);
                }
            }
        }

        return previous;
    }

    /**
     * Removes all entries
     */
    @Override
    public synchronized final void evictAll() {
        Iterator<K> it = map.keySet().iterator();
        while (it.hasNext()) {
            K key = it.next();
            it.remove();
            remove(key);
        }
        mapIn.clear();
        mapOut.clear();
        mapHot.clear();
        sizeIn = 0;
        sizeOut = 0;
        sizeHot = 0;
    }

    @Override
    public synchronized final int size() {
        return sizeIn + sizeOut + sizeHot;
    }

    @Override
    public synchronized final int maxSize() {
        return maxSizeIn + maxSizeOut + maxSizeHot;
    }

    /**
     * Sets sizes:
     * mapIn  ~ 25%
     * mapOut ~ 50%
     * mapHot ~ 25%
     */
    private void calcMaxSizes(int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSize <= 0");
        }
        synchronized (this) {
            maxSizeIn = (int) (maxSize * .25);
            maxSizeOut = maxSizeIn * 2;
            maxSizeHot = maxSize - maxSizeOut - maxSizeIn;
        }
    }

    /**
     * Remove items by LRU from mapHot
     */
    private void trimMapHot() {
        while (true) {
            K key;
            V value;
            if (sizeHot < 0 || (mapHot.isEmpty() && sizeHot != 0)) {
                throw new IllegalStateException(getClass().getName()
                        + ".sizeOf() is reporting inconsistent results!");
            }

            if (sizeHot <= maxSizeHot || mapHot.isEmpty()) {
                break;
            }
            key = mapHot.iterator().next();
            mapHot.remove(key);
            value = map.remove(key);
            sizeHot -= safeSizeOf(key, value);
        }
    }

    /**
     * Remove items by FIFO from mapIn & mapOut
     */
    private boolean trimMapIn(final int sizeOfValue) {
        boolean result = false;
        if (maxSizeIn < sizeOfValue) {
            return result;
        } else {
            while (mapIn.iterator().hasNext()) {
                K keyIn;
                V valueIn;
                if (!mapIn.iterator().hasNext()) {
                    System.out.print("err");
                }
                keyIn = mapIn.iterator().next();
                valueIn = map.get(keyIn);
                if ((sizeIn + sizeOfValue) <= maxSizeIn || mapIn.isEmpty()) {
                    if (keyIn == null) {
                        System.out.print("err");
                    }
                    sizeIn += sizeOfValue;
                    result = true;
                    break;
                }
                mapIn.remove(keyIn);
                final int removedItemSize = safeSizeOf(keyIn, valueIn);
                sizeIn -= removedItemSize;

                while (mapOut.iterator().hasNext()) {
                    K keyOut;
                    V valueOut;
                    if ((sizeOut + removedItemSize) <= maxSizeOut || mapOut.isEmpty()) {
                        mapOut.add(keyIn);
                        sizeOut += removedItemSize;
                        break;
                    }
                    keyOut = mapOut.iterator().next();
                    mapOut.remove(keyOut);
                    valueOut = map.remove(keyOut);
                    sizeOut -= safeSizeOf(keyOut, valueOut);
                }
            }
        }
        return result;
    }

    /**
     * Check for free slot in any container and add if exists
     */
    private boolean add2slot(final K key, final int sizeOfValue) {
        boolean hasFreeSlot = false;
        if (maxSizeIn >= sizeIn + sizeOfValue) {
            mapIn.add(key);
            sizeIn += sizeOfValue;
            hasFreeSlot = true;
        }
        if (!hasFreeSlot && maxSizeOut >= sizeOut + sizeOfValue) {
            mapOut.add(key);
            sizeOut += sizeOfValue;
            hasFreeSlot = true;
        }
        if (!hasFreeSlot && maxSizeHot >= sizeHot + sizeOfValue) {
            mapHot.add(key);
            sizeHot += sizeOfValue;
            hasFreeSlot = true;
        }
        return hasFreeSlot;
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

    public synchronized final int hitCount() {
        return hitCount;
    }

    public synchronized final int missCount() {
        return missCount;
    }

    @Override
    public synchronized final String toString() {
        int accesses = hitCount + missCount;
        int hitPercent = accesses != 0 ? (100 * hitCount / accesses) : 0;
        return String.format("Cache[size=%d,maxSize=%d,hits=%d,misses=%d,hitRate=%d%%," +
                "sizeIn=%d,sizeOut=%d,sizeHot=%d," +
                        "]",
                size(), maxSize(), hitCount, missCount, hitPercent, sizeIn, sizeOut, sizeHot)
                + "\n map:" + map.toString();
    }
}


