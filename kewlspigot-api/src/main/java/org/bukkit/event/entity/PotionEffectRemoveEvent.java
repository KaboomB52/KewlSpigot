package org.bukkit.event.entity;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.potion.PotionEffect;

/**
 * Called when a potion effect is removed from an entity for whatever reason
 */
public class PotionEffectRemoveEvent extends PotionEffectEvent implements Cancellable {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    private boolean cancelled = false;

    public PotionEffectRemoveEvent(LivingEntity entity, PotionEffect effect) {
        super(entity, effect);
    }

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override public HandlerList getHandlers() { return PotionEffectRemoveEvent.HANDLER_LIST; }

    public static HandlerList getHandlerList() { return PotionEffectRemoveEvent.HANDLER_LIST; }

}
