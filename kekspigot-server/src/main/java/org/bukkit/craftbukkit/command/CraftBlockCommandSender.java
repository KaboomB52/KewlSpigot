package org.bukkit.craftbukkit.command;

import net.minecraft.server.CommandBlockListenerAbstract;
import net.minecraft.server.IChatBaseComponent;
import net.minecraft.server.ICommandListener;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.craftbukkit.util.CraftChatMessage;

import java.text.MessageFormat;

/**
 * Represents input from a command block
 */
public class CraftBlockCommandSender extends ServerCommandSender implements BlockCommandSender {
	private final CommandBlockListenerAbstract commandBlock;

	public CraftBlockCommandSender(CommandBlockListenerAbstract commandBlockListenerAbstract) {
		super();
		this.commandBlock = commandBlockListenerAbstract;
	}

	public Block getBlock() {
		return commandBlock.getWorld().getWorld().getBlockAt(commandBlock.getChunkCoordinates().getX(), commandBlock.getChunkCoordinates().getY(), commandBlock.getChunkCoordinates().getZ());
	}

	public void sendMessage(String message) {
		for (IChatBaseComponent component : CraftChatMessage.fromString(message)) {
			commandBlock.sendMessage(component);
		}
	}

	public void sendMessage(String[] messages) {
		for (String message : messages) {
			sendMessage(message);
		}
	}

	public void sendFormattedMessage(final String message, final Object... args) {
		this.sendMessage(MessageFormat.format(message, args));
	}

	public String getName() {
		return commandBlock.getName();
	}

	@Override
	public String getDisplayName() {
		return ChatColor.DARK_RED + "Command Block";
	}

	public boolean isOp() {
		return true;
	}

	public void setOp(boolean value) {
		throw new UnsupportedOperationException("Cannot change operator status of a block");
	}

	public ICommandListener getTileEntity() {
		return commandBlock;
	}
}
