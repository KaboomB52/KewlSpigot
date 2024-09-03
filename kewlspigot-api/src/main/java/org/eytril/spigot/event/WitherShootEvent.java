package org.eytril.spigot.event;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Wither;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class WitherShootEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Wither wither;
    private final Entity target;

    private boolean cancelled;

    public WitherShootEvent(Wither wither, Entity target) {
        this.wither = wither;
        this.target = target;
    }

    public Wither getWither() {
        return wither;
    }

    public Entity getTarget() {
        return target;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

}
