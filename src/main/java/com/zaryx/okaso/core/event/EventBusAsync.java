package com.zaryx.okaso.core.event;

import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

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

    @SuppressWarnings("unchecked")
    public <E extends FrameworkEvent> void subscribe(Class<E> eventType, EventListener<E> listener) {
        if (eventType == null || listener == null) {
            logger.warning("Cannot register a null listener");
            return;
        }

        listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>())
                .add((EventListener) listener);
    }

    @SuppressWarnings("unchecked")
    public <E extends FrameworkEvent> void unsubscribe(Class<E> eventType, EventListener<E> listener) {
        List<EventListener> eventListeners = listeners.get(eventType);
        if (eventListeners != null) {
            eventListeners.remove((EventListener) listener);
        }
    }

    public <E extends FrameworkEvent> void publish(E event) {
        publishSync(event);
    }

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
                    logger.log(Level.WARNING, "Error in synchronous listener: " + event.getClass().getSimpleName(), e);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    public <E extends FrameworkEvent> void publishAsync(E event) {
        if (event == null) {
            logger.warning("Cannot publish a null event");
            return;
        }

        if (!asyncEnabled || executorService == null || executorService.isShutdown()) {

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
                        logger.log(Level.WARNING, "Error in asynchronous listener: " + event.getClass().getSimpleName(), e);
                    }
                });
            }
        }
    }

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
                        logger.log(Level.WARNING, "Error en listener asincrónico: " + event.getClass().getSimpleName(), e);
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

    public int getEventCount() {
        return eventCounter;
    }

    public int getListenerCount() {
        return listeners.values().stream().mapToInt(List::size).sum();
    }

    public int getListenerCount(Class<?> eventType) {
        List<EventListener> list = listeners.get(eventType);
        return list != null ? list.size() : 0;
    }

    public void clear() {
        listeners.clear();
    }

    @FunctionalInterface
    public interface EventListener<E extends FrameworkEvent> {
        void onEvent(E event);
    }

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
