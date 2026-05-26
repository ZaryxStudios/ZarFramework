package com.zaryx.framework.core.event;

import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Enhanced EventBus with asynchronous event support.
 * Optimized for use as a dependency.
 */
public class EventBusAsync {

    private final Logger logger;
    private final Map<Class<?>, List<EventListener>> listeners;
    private final ExecutorService executorService;
    private final boolean asyncEnabled;
    private volatile int eventCounter;

    public EventBusAsync(Logger logger, ExecutorService executor, boolean asyncEnabled) {
        this.logger = logger;
        this.listeners = Collections.synchronizedMap(new HashMap<>());
        this.executorService = executor;
        this.asyncEnabled = asyncEnabled;
        this.eventCounter = 0;
    }

    /**
     * Registers a listener for an event type
     */
    @SuppressWarnings("unchecked")
    public <E extends FrameworkEvent> void subscribe(Class<E> eventType, EventListener<E> listener) {
        if (eventType == null || listener == null) {
            logger.warning("Cannot register a null listener");
            return;
        }

        listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>())
                .add((EventListener) listener);
    }

    /**
     * Unregisters a listener
     */
    @SuppressWarnings("unchecked")
    public <E extends FrameworkEvent> void unsubscribe(Class<E> eventType, EventListener<E> listener) {
        List<EventListener> eventListeners = listeners.get(eventType);
        if (eventListeners != null) {
            eventListeners.remove((EventListener) listener);
        }
    }

    /**
     * Publishes an event synchronously
     */
    public <E extends FrameworkEvent> void publish(E event) {
        publishSync(event);
    }

    /**
     * Publishes an event synchronously
     */
    @SuppressWarnings("unchecked")
    public <E extends FrameworkEvent> void publishSync(E event) {
        if (event == null) {
            logger.warning("Cannot publish a null event");
            return;
        }

        eventCounter++;
        List<EventListener> eventListeners = listeners.get(event.getClass());
        if (eventListeners != null && !eventListeners.isEmpty()) {
            for (EventListener<E> listener : eventListeners) {
                try {
                    listener.onEvent(event);
                } catch (Exception e) {
                    logger.log(Level.WARNING, 
                        "Error in synchronous listener: " + event.getClass().getSimpleName(), e);
                }
            }
        }
    }

    /**
     * Publishes an event asynchronously
     */
    @SuppressWarnings("unchecked")
    public <E extends FrameworkEvent> void publishAsync(E event) {
        if (event == null) {
            logger.warning("Cannot publish a null event");
            return;
        }

        if (!asyncEnabled || executorService == null || executorService.isShutdown()) {
            // Fallback to synchronous mode
            publishSync(event);
            return;
        }

        eventCounter++;
        List<EventListener> eventListeners = listeners.get(event.getClass());
        if (eventListeners != null && !eventListeners.isEmpty()) {
            for (EventListener<E> listener : eventListeners) {
                executorService.submit(() -> {
                    try {
                        listener.onEvent(event);
                    } catch (Exception e) {
                        logger.log(Level.WARNING, 
                            "Error in asynchronous listener: " + event.getClass().getSimpleName(), e);
                    }
                });
            }
        }
    }

    /**
     * Publishes an event asynchronously with a callback
     */
    @SuppressWarnings("unchecked")
    public <E extends FrameworkEvent> void publishAsyncWithCallback(E event, Runnable callback) {
        if (event == null) {
            logger.warning("Cannot publish a null event");
            return;
        }

        if (!asyncEnabled || executorService == null || executorService.isShutdown()) {
            publishSync(event);
            if (callback != null) callback.run();
            return;
        }

        eventCounter++;
        List<EventListener> eventListeners = listeners.get(event.getClass());
        if (eventListeners != null && !eventListeners.isEmpty()) {
            int totalListeners = eventListeners.size();
            AtomicInteger completed = new AtomicInteger(0);

            for (EventListener<E> listener : eventListeners) {
                executorService.submit(() -> {
                    try {
                        listener.onEvent(event);
                    } catch (Exception e) {
                        logger.log(Level.WARNING, 
                            "Error en listener asincrónico: " + event.getClass().getSimpleName(), e);
                    } finally {
                        if (completed.incrementAndGet() == totalListeners && callback != null) {
                            callback.run();
                        }
                    }
                });
            }
        } else if (callback != null) {
            callback.run();
        }
    }

    /**
     * Returns the number of published events
     */
    public int getEventCount() {
        return eventCounter;
    }

    /**
     * Returns the number of registered listeners
     */
    public int getListenerCount() {
        return listeners.values().stream().mapToInt(List::size).sum();
    }

    /**
     * Returns listeners for an event type
     */
    public int getListenerCount(Class<?> eventType) {
        List<EventListener> list = listeners.get(eventType);
        return list != null ? list.size() : 0;
    }

    /**
     * Clears all listeners
     */
    public void clear() {
        listeners.clear();
    }

    /**
     * Interface for event listeners
     */
    @FunctionalInterface
    public interface EventListener<E extends FrameworkEvent> {
        void onEvent(E event);
    }

    /**
     * Base class for all framework events
     */
    public static abstract class FrameworkEvent {
        private final long timestamp;
        private final String source;

        public FrameworkEvent(String source) {
            this.timestamp = System.currentTimeMillis();
            this.source = source;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public String getSource() {
            return source;
        }

        @Override
        public String toString() {
            return this.getClass().getSimpleName() + 
                   "{source='" + source + '\'' + 
                   ", timestamp=" + timestamp + '}';
        }
    }

    /**
     * Atomic integer for thread-safe counters
     */
    private static class AtomicInteger {
        private volatile int value;

        AtomicInteger(int initial) {
            this.value = initial;
        }

        synchronized int incrementAndGet() {
            return ++value;
        }

        int get() {
            return value;
        }
    }
}
