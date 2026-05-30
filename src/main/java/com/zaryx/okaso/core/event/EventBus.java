package com.zaryx.okaso.core.event;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EventBus {

    private final Logger logger;
    private final Map<Class<?>, List<EventListener>> listeners;

    public EventBus(Logger logger) {
        this.logger = logger;
        this.listeners = Collections.synchronizedMap(new HashMap<>());
    }

    @SuppressWarnings("unchecked")
    public <E extends FrameworkEvent> void subscribe(Class<E> eventType, EventListener<E> listener) {
        if (eventType == null || listener == null) {
            logger.warning("Cannot register a null listener");
            return;
        }

        listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>())
                .add((EventListener) listener);
        logger.fine("Listener registered for event: " + eventType.getSimpleName());
    }

    @SuppressWarnings("unchecked")
    public <E extends FrameworkEvent> void unsubscribe(Class<E> eventType, EventListener<E> listener) {
        List<EventListener> eventListeners = listeners.get(eventType);
        if (eventListeners != null) {
            eventListeners.remove((EventListener) listener);
            logger.fine("Listener removed from event: " + eventType.getSimpleName());
        }
    }

    @SuppressWarnings("unchecked")
    public <E extends FrameworkEvent> void publish(E event) {
        if (event == null) {
            logger.warning("Cannot publish a null event");
            return;
        }

        List<EventListener> eventListeners = listeners.get(event.getClass());
        if (eventListeners != null && !eventListeners.isEmpty()) {
            for (EventListener<E> listener : eventListeners) {
                try {
                    listener.onEvent(event);
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Error processing event: " + event.getClass().getSimpleName(), e);
                }
            }
        }
    }

    public int getListenerCount() {
        return listeners.values().stream().mapToInt(List::size).sum();
    }

    public void clear() {
        listeners.clear();
        logger.info("Event bus cleared");
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

        public abstract String getEventName();
    }
}
