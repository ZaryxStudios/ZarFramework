package com.zaryx.okaso.api;

/**
 * Okaso internal runtime configuration.
 * Defines runtime parameters and optimization presets (no public YAML file).
 * Intended to be provided by code when creating OkasoAPI.
 */
public class OkasoConfig {

    // ============ Cache Config ============
    private long cacheTTL = 3600000; // 1 hora
    private int cacheMaxSize = 1000;

    // ============ Async Config ============
    private int threadPoolSize = 4;
    private boolean asyncEventsEnabled = true;

    // ============ Logging Config ============
    private boolean debugMode = false;
    private boolean detailedLogging = false;

    // ============ Module Config ============
    private boolean lazyModuleLoading = true;
    private boolean strictModuleValidation = false;

    // ============ Performance Config ============
    private boolean enableMetrics = true;
    private boolean enableCacheCompression = false;
    private int metricsCollectionIntervalMs = 60000; // 1 minuto

    // ============ HTTP Config ============
    private int requestTimeoutMs = 10000;
    private int connectTimeoutMs = 5000;

    // ============ Constructores ============

    /**
     * Default constructor with sane defaults
     */
    public OkasoConfig() {
    }

    /**
     * Copy constructor
     */
    public OkasoConfig(OkasoConfig other) {
        this.cacheTTL = other.cacheTTL;
        this.cacheMaxSize = other.cacheMaxSize;
        this.threadPoolSize = other.threadPoolSize;
        this.asyncEventsEnabled = other.asyncEventsEnabled;
        this.debugMode = other.debugMode;
        this.detailedLogging = other.detailedLogging;
        this.lazyModuleLoading = other.lazyModuleLoading;
        this.strictModuleValidation = other.strictModuleValidation;
        this.enableMetrics = other.enableMetrics;
        this.enableCacheCompression = other.enableCacheCompression;
        this.metricsCollectionIntervalMs = other.metricsCollectionIntervalMs;
        this.requestTimeoutMs = other.requestTimeoutMs;
        this.connectTimeoutMs = other.connectTimeoutMs;
    }

    // ============ Getters & Setters ============

    public long getCacheTTL() {
        return cacheTTL;
    }

    public OkasoConfig setCacheTTL(long cacheTTL) {
        if (cacheTTL > 0) this.cacheTTL = cacheTTL;
        return this;
    }

    public int getCacheMaxSize() {
        return cacheMaxSize;
    }

    public OkasoConfig setCacheMaxSize(int cacheMaxSize) {
        if (cacheMaxSize > 0) this.cacheMaxSize = cacheMaxSize;
        return this;
    }

    public int getThreadPoolSize() {
        return threadPoolSize;
    }

    public OkasoConfig setThreadPoolSize(int threadPoolSize) {
        if (threadPoolSize > 0) this.threadPoolSize = threadPoolSize;
        return this;
    }

    public boolean isAsyncEventsEnabled() {
        return asyncEventsEnabled;
    }

    public OkasoConfig setAsyncEventsEnabled(boolean asyncEventsEnabled) {
        this.asyncEventsEnabled = asyncEventsEnabled;
        return this;
    }

    public boolean isDebugMode() {
        return debugMode;
    }

    public OkasoConfig setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
        return this;
    }

    public boolean isDetailedLogging() {
        return detailedLogging;
    }

    public OkasoConfig setDetailedLogging(boolean detailedLogging) {
        this.detailedLogging = detailedLogging;
        return this;
    }

    public boolean isLazyModuleLoading() {
        return lazyModuleLoading;
    }

    public OkasoConfig setLazyModuleLoading(boolean lazyModuleLoading) {
        this.lazyModuleLoading = lazyModuleLoading;
        return this;
    }

    public boolean isStrictModuleValidation() {
        return strictModuleValidation;
    }

    public OkasoConfig setStrictModuleValidation(boolean strictModuleValidation) {
        this.strictModuleValidation = strictModuleValidation;
        return this;
    }

    public boolean isEnableMetrics() {
        return enableMetrics;
    }

    public OkasoConfig setEnableMetrics(boolean enableMetrics) {
        this.enableMetrics = enableMetrics;
        return this;
    }

    public boolean isEnableCacheCompression() {
        return enableCacheCompression;
    }

    public OkasoConfig setEnableCacheCompression(boolean enableCacheCompression) {
        this.enableCacheCompression = enableCacheCompression;
        return this;
    }

    public int getMetricsCollectionIntervalMs() {
        return metricsCollectionIntervalMs;
    }

    public OkasoConfig setMetricsCollectionIntervalMs(int metricsCollectionIntervalMs) {
        if (metricsCollectionIntervalMs > 0) this.metricsCollectionIntervalMs = metricsCollectionIntervalMs;
        return this;
    }

    public int getRequestTimeoutMs() {
        return requestTimeoutMs;
    }

    public OkasoConfig setRequestTimeoutMs(int requestTimeoutMs) {
        if (requestTimeoutMs > 0) this.requestTimeoutMs = requestTimeoutMs;
        return this;
    }

    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public OkasoConfig setConnectTimeoutMs(int connectTimeoutMs) {
        if (connectTimeoutMs > 0) this.connectTimeoutMs = connectTimeoutMs;
        return this;
    }

    // ============ Presets ============

    /**
     * Preset optimized for development environments
     */
    public static OkasoConfig development() {
        return new OkasoConfig()
                .setDebugMode(true)
                .setDetailedLogging(true)
                .setCacheTTL(300000) // 5 minutos
                .setCacheMaxSize(100)
                .setThreadPoolSize(2);
    }

    /**
     * Preset optimized for production environments
     */
    public static OkasoConfig production() {
        return new OkasoConfig()
                .setDebugMode(false)
                .setDetailedLogging(false)
                .setCacheTTL(3600000) // 1 hora
                .setCacheMaxSize(5000)
                .setThreadPoolSize(8)
                .setEnableMetrics(true)
                .setRequestTimeoutMs(10000)
                .setConnectTimeoutMs(5000);
    }

    /**
     * Preset for low-performance servers
     */
    public static OkasoConfig lowPerformance() {
        return new OkasoConfig()
                .setDebugMode(false)
                .setDetailedLogging(false)
                .setCacheTTL(600000) // 10 minutos
                .setCacheMaxSize(256)
                .setThreadPoolSize(1)
                .setAsyncEventsEnabled(false)
                .setEnableMetrics(false)
                .setRequestTimeoutMs(5000)
                .setConnectTimeoutMs(3000);
    }

    /**
     * Preset for high-performance servers
     */
    public static OkasoConfig highPerformance() {
        return new OkasoConfig()
                .setDebugMode(false)
                .setDetailedLogging(false)
                .setCacheTTL(7200000) // 2 horas
                .setCacheMaxSize(10000)
                .setThreadPoolSize(16)
                .setAsyncEventsEnabled(true)
                .setEnableMetrics(true)
                .setEnableCacheCompression(true)
                .setRequestTimeoutMs(15000)
                .setConnectTimeoutMs(7000);
    }

    @Override
    public String toString() {
        return "OkasoConfig{" +
                "cacheTTL=" + cacheTTL +
                ", cacheMaxSize=" + cacheMaxSize +
                ", threadPoolSize=" + threadPoolSize +
                ", asyncEventsEnabled=" + asyncEventsEnabled +
                ", debugMode=" + debugMode +
                ", detailedLogging=" + detailedLogging +
                ", lazyModuleLoading=" + lazyModuleLoading +
                ", strictModuleValidation=" + strictModuleValidation +
                ", enableMetrics=" + enableMetrics +
                ", enableCacheCompression=" + enableCacheCompression +
                ", metricsCollectionIntervalMs=" + metricsCollectionIntervalMs +
                ", requestTimeoutMs=" + requestTimeoutMs +
                ", connectTimeoutMs=" + connectTimeoutMs +
                '}';
    }
}

