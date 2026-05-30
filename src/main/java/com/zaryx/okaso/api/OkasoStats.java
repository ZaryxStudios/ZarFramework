package com.zaryx.okaso.api;

public class OkasoStats {

    private long uptime;
    private int moduleCount;
    private int enabledModuleCount;
    private int cacheSize;
    private int cacheHits;
    private int cacheMisses;
    private int eventCount;
    private long memoryUsage;
    private long lastCollectionTime;
    private String lastErrorMessage;

    public OkasoStats() {
        this.lastCollectionTime = System.currentTimeMillis();
    }

    public long getUptime() {
        return uptime;
    }

    public void setUptime(long uptime) {
        this.uptime = uptime;
    }

    public int getModuleCount() {
        return moduleCount;
    }

    public void setModuleCount(int moduleCount) {
        this.moduleCount = moduleCount;
    }

    public int getEnabledModuleCount() {
        return enabledModuleCount;
    }

    public void setEnabledModuleCount(int enabledModuleCount) {
        this.enabledModuleCount = enabledModuleCount;
    }

    public int getCacheSize() {
        return cacheSize;
    }

    public void setCacheSize(int cacheSize) {
        this.cacheSize = cacheSize;
    }

    public int getCacheHits() {
        return cacheHits;
    }

    public void setCacheHits(int cacheHits) {
        this.cacheHits = cacheHits;
    }

    public int getCacheMisses() {
        return cacheMisses;
    }

    public void setCacheMisses(int cacheMisses) {
        this.cacheMisses = cacheMisses;
    }

    public double getCacheHitRate() {
        if (cacheHits + cacheMisses == 0) return 0;
        return (double) cacheHits / (cacheHits + cacheMisses) * 100;
    }

    public int getEventCount() {
        return eventCount;
    }

    public void setEventCount(int eventCount) {
        this.eventCount = eventCount;
    }

    public long getMemoryUsage() {
        return memoryUsage;
    }

    public void setMemoryUsage(long memoryUsage) {
        this.memoryUsage = memoryUsage;
    }

    public long getLastCollectionTime() {
        return lastCollectionTime;
    }

    public void setLastCollectionTime(long lastCollectionTime) {
        this.lastCollectionTime = lastCollectionTime;
    }

    public String getLastErrorMessage() {
        return lastErrorMessage;
    }

    public void setLastErrorMessage(String lastErrorMessage) {
        this.lastErrorMessage = lastErrorMessage;
    }

    public void incrementCacheHits() {
        this.cacheHits++;
    }

    public void incrementCacheMisses() {
        this.cacheMisses++;
    }

    public void incrementEventCount() {
        this.eventCount++;
    }

    @Override
    public String toString() {
        return "OkasoStats{" +
                "uptime=" + uptime + "ms" +
                ", modules=" + enabledModuleCount + "/" + moduleCount +
                ", cache=" + cacheSize + " items" +
                ", hitRate=" + String.format("%.2f%%", getCacheHitRate()) +
                ", events=" + eventCount +
                ", memory=" + (memoryUsage / 1024 / 1024) + "MB" +
                '}';
    }
}
