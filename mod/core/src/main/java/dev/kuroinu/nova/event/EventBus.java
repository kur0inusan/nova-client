package dev.kuroinu.nova.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Minimal type-keyed event bus. Listeners are invoked in subscription order on the posting thread.
 */
public final class EventBus {
    public interface Listener<T> {
        void handle(T event);
    }

    private final Map<Class<?>, List<Listener<?>>> listeners = new HashMap<Class<?>, List<Listener<?>>>();

    public synchronized <T> Listener<T> subscribe(Class<T> type, Listener<T> listener) {
        List<Listener<?>> list = listeners.get(type);
        if (list == null) {
            list = new CopyOnWriteArrayList<Listener<?>>();
            listeners.put(type, list);
        }
        list.add(listener);
        return listener;
    }

    public synchronized void unsubscribe(Class<?> type, Listener<?> listener) {
        List<Listener<?>> list = listeners.get(type);
        if (list != null) {
            list.remove(listener);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T post(T event) {
        List<Listener<?>> list;
        synchronized (this) {
            list = listeners.get(event.getClass());
        }
        if (list != null) {
            for (Listener<?> listener : list) {
                ((Listener<T>) listener).handle(event);
            }
        }
        return event;
    }

    public synchronized int listenerCount(Class<?> type) {
        List<Listener<?>> list = listeners.get(type);
        return list == null ? 0 : new ArrayList<Listener<?>>(list).size();
    }
}
