package dev.kuroinu.nova.module;

import dev.kuroinu.nova.NovaClient;
import dev.kuroinu.nova.event.EventBus;
import dev.kuroinu.nova.setting.Setting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Base class for every feature. To add a feature: subclass this, declare settings with
 * {@link #add}, subscribe to events with {@link #on}, and register it in
 * {@link dev.kuroinu.nova.module.impl.Modules}. Subscriptions are only active while enabled.
 */
public abstract class Module {
    private final String id;
    private final String name;
    private final String description;
    private final Category category;
    private final List<Setting<?>> settings = new ArrayList<Setting<?>>();
    private final List<Subscription<?>> subscriptions = new ArrayList<Subscription<?>>();
    private boolean enabled;
    private EventBus bus;
    /** Toggle key in the platform's key space, or {@link #NO_KEY}. Set from the ClickGUI. */
    private int bindKey = NO_KEY;
    private boolean favorite;

    public static final int NO_KEY = -1;

    protected Module(String id, String name, String description, Category category) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
    }

    protected final <S extends Setting<?>> S add(S setting) {
        settings.add(setting);
        return setting;
    }

    /** Declares an event handler that is active only while this module is enabled. */
    protected final <T> void on(Class<T> type, EventBus.Listener<T> listener) {
        Subscription<T> sub = new Subscription<T>(type, listener);
        subscriptions.add(sub);
        if (enabled && bus != null) {
            bus.subscribe(type, listener);
        }
    }

    final void attach(EventBus bus) {
        this.bus = bus;
    }

    public final void setEnabled(boolean enabled) {
        if (this.enabled == enabled) {
            return;
        }
        this.enabled = enabled;
        if (bus != null) {
            for (Subscription<?> sub : subscriptions) {
                if (enabled) {
                    sub.subscribe(bus);
                } else {
                    bus.unsubscribe(sub.type, sub.listener);
                }
            }
        }
        if (enabled) {
            onEnable();
        } else {
            onDisable();
        }
    }

    public final void toggle() {
        setEnabled(!enabled);
    }

    protected void onEnable() {
    }

    protected void onDisable() {
    }

    /** Whether the module starts enabled on first launch (before any config exists). */
    public boolean isEnabledByDefault() {
        return false;
    }

    /** False for always-on modules (e.g. client settings) that only exist to hold settings. */
    public boolean isToggleable() {
        return true;
    }

    /** Whether the module starts in Favorites on first launch. */
    public boolean isFavoriteByDefault() {
        return false;
    }

    public boolean isFavorite() {
        return favorite;
    }

    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }

    public int getBindKey() {
        return bindKey;
    }

    public void setBindKey(int bindKey) {
        this.bindKey = bindKey;
    }

    protected static NovaClient nova() {
        return NovaClient.get();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Category getCategory() {
        return category;
    }

    public List<Setting<?>> getSettings() {
        return Collections.unmodifiableList(settings);
    }

    private static final class Subscription<T> {
        final Class<T> type;
        final EventBus.Listener<T> listener;

        Subscription(Class<T> type, EventBus.Listener<T> listener) {
            this.type = type;
            this.listener = listener;
        }

        void subscribe(EventBus bus) {
            bus.subscribe(type, listener);
        }
    }
}
