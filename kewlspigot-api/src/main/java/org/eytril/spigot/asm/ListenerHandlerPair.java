package org.eytril.spigot.asm;

import org.bukkit.event.Listener;

public class ListenerHandlerPair {
    private final Listener listener;
    private final ASMEventHandler handler;

    public ListenerHandlerPair(Listener listener, ASMEventHandler handler) {
        this.listener = listener;
        this.handler = handler;
    }

    public Listener getListener() {
        return listener;
    }

    public ASMEventHandler getHandler() {
        return handler;
    }
}
