package com.zaryx.framework.core.cache;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * CacheManager: single authoritative cache implementation.
 * Merges improvements from the previous V2 implementation while
 * keeping the original constructor signature for compatibility.
 */
public class CacheManager {

    private final Map<String, CacheEntry<?>> cache;
    private static final Logger logger = Logger.getLogger(CacheManager.class.getName());
    private final long defaultTTL;
    private final int maxSize;
    private final boolean compressionEnabled;

    // Metrics
    private final AtomicLong hits;
    private final AtomicLong misses;
    private final AtomicLong puts;
    private final AtomicLong evictions;

    public CacheManager(long defaultTTLMillis, int maxSize, boolean compressionEnabled) {
        this.cache = new ConcurrentHashMap<>();
        this.defaultTTL = defaultTTLMillis;
        this.compressionEnabled = compressionEnabled;
        this.maxSize = maxSize;

        this.hits = new AtomicLong(0);
        this.misses = new AtomicLong(0);
        this.puts = new AtomicLong(0);
        this.evictions = new AtomicLong(0);
    }

    // Backwards-compatible constructor
    public CacheManager(long defaultTTLMillis, int maxSize) {
        this(defaultTTLMillis, maxSize, false);
    }

    // ============ Cache Operations ============

    public <V> void put(String key, V value) {
        put(key, value, defaultTTL);
    }

    public <V> void put(String key, V value, long ttlMillis) {
        if (key == null || value == null) {
            logger.warning("Cannot cache null key or value");
            return;
        }

        if (cache.size() >= maxSize && !cache.containsKey(key)) {
            evictOldest();
        }

        cache.put(key, new CacheEntry<>(value, ttlMillis, System.currentTimeMillis()));
        puts.incrementAndGet();
    }

    @SuppressWarnings("unchecked")
    public <V> V get(String key, Class<V> type) {
        if (key == null) return null;
        CacheEntry<?> entry = cache.get(key);
        if (entry == null) {
            misses.incrementAndGet();
            return null;
        }

        if (entry.isExpired()) {
            cache.remove(key);
            misses.incrementAndGet();
            return null;
        }

        hits.incrementAndGet();
        if (type.isInstance(entry.getValue())) {
            return (V) entry.getValue();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public <V> V get(String key) {
        if (key == null) return null;
        CacheEntry<?> entry = cache.get(key);
        if (entry == null) {
            misses.incrementAndGet();
            return null;
        }

        if (entry.isExpired()) {
            cache.remove(key);
            misses.incrementAndGet();
            return null;
        }

        hits.incrementAndGet();
        return (V) entry.getValue();
    }

    public boolean contains(String key) {
        CacheEntry<?> entry = cache.get(key);
        if (entry == null) return false;
        if (entry.isExpired()) {
            cache.remove(key);
            return false;
        }
        return true;
    }

    public void remove(String key) {
        cache.remove(key);
    }

    public void clear() {
        cache.clear();
        logger.info("Cache cleared");
    }

    public int size() {
        return cache.size();
    }

    public int cleanup() {
        int removed = 0;
        Iterator<String> iterator = cache.keySet().iterator();
        while (iterator.hasNext()) {
            String key = iterator.next();
            CacheEntry<?> entry = cache.get(key);
            if (entry != null && entry.isExpired()) {
                iterator.remove();
                removed++;
            }
        }
        return removed;
    }

    private void evictOldest() {
        if (cache.isEmpty()) return;

        String oldestKey = null;
        long oldestTime = Long.MAX_VALUE;

        for (Map.Entry<String, CacheEntry<?>> entry : cache.entrySet()) {
            if (entry.getValue().getCreatedAt() < oldestTime) {
                oldestTime = entry.getValue().getCreatedAt();
                oldestKey = entry.getKey();
            }
        }

        if (oldestKey != null) {
            cache.remove(oldestKey);
            evictions.incrementAndGet();
        }
    }

    // ============ Metrics ============

    public long getHits() { return hits.get(); }
    public long getMisses() { return misses.get(); }
    public double getHitRate() {
        long total = hits.get() + misses.get();
        if (total == 0) return 0;
        return (double) hits.get() / total * 100;
    }
    public long getPuts() { return puts.get(); }
    public long getEvictions() { return evictions.get(); }
    public int getSize() { return cache.size(); }
    public int getMaxSize() { return maxSize; }
    public double getUsagePercentage() { return (double) cache.size() / maxSize * 100; }

    public CacheStats getStats() {
        return new CacheStats(
                cache.size(), maxSize, hits.get(), misses.get(), puts.get(), evictions.get(), defaultTTL, compressionEnabled
        );
    }

    private static class CacheEntry<V> {
        private final V value;
        private final long expiresAt;
        private final long createdAt;

        CacheEntry(V value, long ttlMillis, long createdAt) {
            this.value = value;
            this.expiresAt = ttlMillis <= 0 ? Long.MAX_VALUE : System.currentTimeMillis() + ttlMillis;
            this.createdAt = createdAt;
        }

        V getValue() { return value; }
        boolean isExpired() { return System.currentTimeMillis() > expiresAt; }
        long getCreatedAt() { return createdAt; }
    }

    public static class CacheStats {
        public final int size;
        public final int maxSize;
        public final long hits;
        public final long misses;
        public final long puts;
        public final long evictions;
        public final long ttl;
        public final boolean compressionEnabled;

        public CacheStats(int size, int maxSize, long hits, long misses, long puts, long evictions, long ttl, boolean compressionEnabled) {
            this.size = size;
            this.maxSize = maxSize;
            this.hits = hits;
            this.misses = misses;
            this.puts = puts;
            this.evictions = evictions;
            this.ttl = ttl;
            this.compressionEnabled = compressionEnabled;
        }

        public double getHitRate() {
            long total = hits + misses;
            if (total == 0) return 0;
            return (double) hits / total * 100;
        }

        public double getUsagePercentage() {
            return (double) size / maxSize * 100;
        }

        @Override
        public String toString() {
            return "CacheStats{" +
                    "size=" + size + "/" + maxSize +
                    ", usage=" + String.format("%.2f%%", getUsagePercentage()) +
                    ", hits=" + hits +
                    ", misses=" + misses +
                    ", hitRate=" + String.format("%.2f%%", getHitRate()) +
                    ", puts=" + puts +
                    ", evictions=" + evictions +
                    '}';
        }
    }
}
