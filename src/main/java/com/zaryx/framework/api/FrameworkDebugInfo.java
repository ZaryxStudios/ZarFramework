package com.zaryx.framework.api;

import java.util.Map;

/**
 * Debug information for ZarFramework.
 * Provides details for troubleshooting and monitoring.
 */
public class FrameworkDebugInfo {

    private String frameworkVersion;
    private String state;
    private long startTime;
    private long initializationTime;
    private Map<String, Object> components;
    private Map<String, String> moduleStatus;
    private Throwable lastError;
    private String lastErrorMessage;

    // ============ Constructores ============

    public FrameworkDebugInfo() {
    }

    // ============ Getters & Setters ============

    public String getFrameworkVersion() {
        return frameworkVersion;
    }

    public void setFrameworkVersion(String frameworkVersion) {
        this.frameworkVersion = frameworkVersion;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getInitializationTime() {
        return initializationTime;
    }

    public void setInitializationTime(long initializationTime) {
        this.initializationTime = initializationTime;
    }

    public Map<String, Object> getComponents() {
        return components;
    }

    public void setComponents(Map<String, Object> components) {
        this.components = components;
    }

    public Map<String, String> getModuleStatus() {
        return moduleStatus;
    }

    public void setModuleStatus(Map<String, String> moduleStatus) {
        this.moduleStatus = moduleStatus;
    }

    public Throwable getLastError() {
        return lastError;
    }

    public void setLastError(Throwable lastError) {
        this.lastError = lastError;
    }

    public String getLastErrorMessage() {
        return lastErrorMessage;
    }

    public void setLastErrorMessage(String lastErrorMessage) {
        this.lastErrorMessage = lastErrorMessage;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("FrameworkDebugInfo{\n");
        sb.append("  version=").append(frameworkVersion).append(",\n");
        sb.append("  state=").append(state).append(",\n");
        sb.append("  startTime=").append(startTime).append(",\n");
        sb.append("  initTime=").append(initializationTime).append("ms,\n");
        sb.append("  components=").append(components != null ? components.size() : 0).append(",\n");
        sb.append("  modules=").append(moduleStatus != null ? moduleStatus.size() : 0).append(",\n");
        if (lastError != null) {
            sb.append("  error=").append(lastErrorMessage).append("\n");
        }
        sb.append("}");
        return sb.toString();
    }
}
