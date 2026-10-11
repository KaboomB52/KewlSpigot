package org.eytril.spigot.asm;

import org.bukkit.event.Event;

/**
 * Associates a listener failure with the listener and event that caused it.
 */
public final class EventDispatchException extends Exception {

    private final ListenerHandlerPair registration;
    private final Event event;

    public EventDispatchException(
            ListenerHandlerPair registration,
            Event event,
            Throwable cause
    ) {
        super(cause);
        this.registration = registration;
        this.event = event;
    }

    public ListenerHandlerPair getRegistration() {
        return registration;
    }

    public Event getEvent() {
        return event;
    }
}