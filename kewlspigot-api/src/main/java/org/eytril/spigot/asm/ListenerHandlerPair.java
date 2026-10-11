package org.eytril.spigot.asm;

import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;

/**
 * Stores one registered event handler.
 */
public final class ListenerHandlerPair {

    private final Listener listener;
    private final Plugin plugin;
    private final Class<? extends Event> eventType;
    private final EventPriority priority;
    private final boolean ignoreCancelled;
    private final ASMEventHandler handler;
    private final EventExecutor executor;
    private final long registrationOrder;

    public ListenerHandlerPair(
            Listener listener,
            Plugin plugin,
            Class<? extends Event> eventType,
            EventPriority priority,
            boolean ignoreCancelled,
            ASMEventHandler handler,
            EventExecutor executor,
            long registrationOrder
    ) {
        this.listener = listener;
        this.plugin = plugin;
        this.eventType = eventType;
        this.priority = priority;
        this.ignoreCancelled = ignoreCancelled;
        this.handler = handler;
        this.executor = executor;
        this.registrationOrder = registrationOrder;
    }

    public Listener getListener() {
        return listener;
    }

    public Plugin getPlugin() {
        return plugin;
    }

    public Class<? extends Event> getEventType() {
        return eventType;
    }

    public EventPriority getPriority() {
        return priority;
    }

    public boolean isIgnoreCancelled() {
        return ignoreCancelled;
    }

    public ASMEventHandler getHandler() {
        return handler;
    }

    public EventExecutor getExecutor() {
        return executor;
    }

    public long getRegistrationOrder() {
        return registrationOrder;
    }

    public boolean accepts(Event event) {
        return eventType.isAssignableFrom(event.getClass());
    }
}