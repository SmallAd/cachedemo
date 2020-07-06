package com.example.cachedemo.cache;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;

public class Cache2QImpl<K, V> implements Cache<K, V> {

    final HashMap<K, V> map;
    /**
     * Sets for 2Q algorithm
     */
    private final LinkedHashSet<K> mapIn, mapOut, mapHot;

    protected float quarter = .25f;
    /**
     * Size of this cache in units. Not necessarily the number of elements.
     */
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

        map = new HashMap<K, V>(0, 0.75f);

        mapIn = new LinkedHashSet<K>();
        mapOut = new LinkedHashSet<K>();
        mapHot = new LinkedHashSet<K>();
    }

    /**
     * Returns the value if it exists.
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
                        sizeHot++;
                        trimMapHot();
                        sizeOut--;
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
     * Caches value.
     */
    @Override
    public V put(K key, V value) {
        if (key == null || value == null) {
            throw new NullPointerException("key == null || value == null");
        }

        if (map.containsKey(key)) {
            // if already have - replace it.
            return map.put(key, value);
        }
        V result;
        synchronized (this) {
            //if there are free page slots then put value into a free page slot
            boolean hasFreeSlot = add2slot(key);
            if (hasFreeSlot) {
                // add 2 free slot & exit
                map.put(key, value);
                result = value;
            } else {
                // no free slot, go to trim mapIn/mapOut
                if (trimMapIn()) {
                    //put X into the reclaimed page slot
                    map.put(key, value);
                    result = value;
                } else {
                    map.put(key, value);
                    mapHot.add(key);
                    sizeHot++;
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
    @Override
    public V remove(K key) {
        if (key == null) {
            throw new NullPointerException("key == null");
        }

        V previous;
        synchronized (this) {
            previous = map.remove(key);
            if (previous != null) {
                if (mapIn.contains(key)) {
                    sizeIn--;
                    mapIn.remove(key);
                }
                if (mapOut.contains(key)) {
                    sizeOut--;
                    mapOut.remove(key);
                }
                if (mapHot.contains(key)) {
                    sizeHot--;
                    mapHot.remove(key);
                }
            }
        }
        return previous;
    }

    /**
     * Sets the size of the cache.
     */
    @Override
    public void resize(int maxSize) {

        calcMaxSizes(maxSize);
        synchronized (this) {
            HashMap<K, V> copy = new HashMap<K, V>(map);
            evictAll();
            Iterator<K> it = copy.keySet().iterator();
            while (it.hasNext()) {
                K key = it.next();
                put(key, copy.get(key));
            }
        }
    }

    /**
     * Clear the cache.
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
     * mapIn  ~ 25% // 1st lvl - store for input keys, FIFO
     * mapOut ~ 50% // 2nd lvl - store for keys goes from input to output, FIFO
     * mapHot ~ 25% // hot lvl - store for keys goes from output to hot, LRU
     */
    private void calcMaxSizes(int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSize <= 0");
        }
        synchronized (this) {
            //sizes
            maxSizeIn = (int) (maxSize * quarter);
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
            synchronized (this) {
                if (sizeHot < 0 || (mapHot.isEmpty() && sizeHot != 0)) {
                    throw new IllegalStateException(getClass().getName()
                            + ".sizeOf() is reporting inconsistent results!");
                }

                if (sizeHot <= maxSizeHot || mapHot.isEmpty()) {
                    break;
                }
                // we add new item before, so next return first (LRU) item
                key = mapHot.iterator().next();
                mapHot.remove(key);
                sizeHot--;
                map.remove(key);
            }
        }
    }

    /**
     * Remove items by FIFO from mapIn & mapOut
     */
    private boolean trimMapIn() {
        boolean result = false;
        if (maxSizeIn < 1) {
            return result;
        } else {
            while (mapIn.iterator().hasNext()) {
                K keyIn;
                if (!mapIn.iterator().hasNext()) {
                    System.out.print("err");
                }
                keyIn = mapIn.iterator().next();
                if (sizeIn  < maxSizeIn || mapIn.isEmpty()) {
                    //put X into the reclaimed page slot
                    if (keyIn == null) {
                        System.out.print("err");
                    }
                    mapIn.add(keyIn);
                    sizeIn++;
                    result = true;
                    break;
                }
                //page out the tail of mapIn, call it Y
                mapIn.remove(keyIn);
                sizeIn--;

                // add identifier of Y to the head of mapOut
                while (mapOut.iterator().hasNext()) {
                    K keyOut;
                    if (sizeOut < maxSizeOut || mapOut.isEmpty()) {
                        // put Y into the reclaimed page slot
                        mapOut.add(keyIn);
                        sizeOut++;
                        break;
                    }
                    //remove identifier of Z from the tail of mapOut
                    keyOut = mapOut.iterator().next();
                    mapOut.remove(keyOut);
                    sizeOut--;
                }
            }
        }
        return result;
    }

    /**
     * Check for free slot in any container and add if exists
     */
    private boolean add2slot(final K key) {
        boolean hasFreeSlot = false;
        if (!hasFreeSlot && maxSizeIn >= sizeIn + 1) {
            mapIn.add(key);
            sizeIn++;
            hasFreeSlot = true;
        }
        if (!hasFreeSlot && maxSizeOut >= sizeOut + 1) {
            mapOut.add(key);
            sizeOut++;
            hasFreeSlot = true;
        }
        if (!hasFreeSlot && maxSizeHot >= sizeHot + 1) {
            mapHot.add(key);
            sizeHot++;
            hasFreeSlot = true;
        }
        return hasFreeSlot;
    }

    public synchronized final int hitCount() {
        return hitCount;
    }

    public synchronized final int missCount() {
        return missCount;
    }

    @Override
    public synchronized final String toString() {
        int accesses = hitCount() + missCount();
        int hitPercent = accesses != 0 ? (100 * hitCount() / accesses) : 0;
        return String.format("Cache[size=%d,maxSize=%d,hits=%d,misses=%d,hitRate=%d%%," +
                        "]",
                size(), maxSize(), hitCount(), missCount(), hitPercent)
                + "\n map:" + map.toString();
    }

}

