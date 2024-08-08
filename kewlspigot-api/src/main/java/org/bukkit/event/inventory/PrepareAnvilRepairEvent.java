package org.bukkit.event.inventory;

import org.bukkit.block.Block;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

/**
 * @since 11/20/2017
 */
public class PrepareAnvilRepairEvent extends InventoryEvent implements Cancellable {

	private static final HandlerList handlers;

	static {
		handlers = new HandlerList();
	}

	private final ItemStack first;
	private final ItemStack second;
	private final HumanEntity repairer;
	private final Block anvil;
	private boolean cancelled;
	private ItemStack result;

	public PrepareAnvilRepairEvent(final HumanEntity repairer, final InventoryView view, final Block anvil,
								   final ItemStack first, final ItemStack second, final ItemStack result) {
		super(view);
		this.first = first;
		this.second = second;
		this.anvil = anvil;
		this.result = result;
		this.repairer = repairer;
	}

	public static HandlerList getHandlerList() {
		return PrepareAnvilRepairEvent.handlers;
	}

	public ItemStack getFirst() {
		return this.first;
	}

	public ItemStack getSecond() {
		return this.second;
	}

	public HumanEntity getRepairer() {
		return this.repairer;
	}

	public Block getAnvil() {
		return this.anvil;
	}

	public ItemStack getResult() {
		return this.result;
	}

	public void setResult(final ItemStack result) {
		this.result = result;
	}

	public boolean isCancelled() {
		return this.cancelled;
	}

	public void setCancelled(final boolean cancel) {
		this.cancelled = cancel;
	}

	public HandlerList getHandlers() {
		return PrepareAnvilRepairEvent.handlers;
	}
}
