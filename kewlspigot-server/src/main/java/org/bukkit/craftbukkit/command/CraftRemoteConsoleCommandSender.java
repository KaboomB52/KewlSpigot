package org.bukkit.craftbukkit.command;

import net.minecraft.server.ChatComponentText;
import net.minecraft.server.RemoteControlCommandListener;
import org.bukkit.ChatColor;
import org.bukkit.command.RemoteConsoleCommandSender;

import java.text.MessageFormat;

public class CraftRemoteConsoleCommandSender extends ServerCommandSender implements RemoteConsoleCommandSender {
	public CraftRemoteConsoleCommandSender() {
		super();
	}

	@Override
	public void sendMessage(String message) {
		RemoteControlCommandListener.getInstance().sendMessage(new ChatComponentText(message + "\n")); // Send a newline after each message, to preserve formatting.
	}

	@Override
	public void sendMessage(String[] messages) {
		for (String message : messages) {
			sendMessage(message);
		}
	}

	public void sendFormattedMessage(final String message, final Object... args) {
		this.sendMessage(MessageFormat.format(message, args));
	}

	@Override
	public String getName() {
		return "RCON";
	}

	@Override
	public String getDisplayName() {
		return ChatColor.DARK_RED + "RCON";
	}

	@Override
	public boolean isOp() {
		return true;
	}

	@Override
	public void setOp(boolean value) {
		throw new UnsupportedOperationException("Cannot change operator status of remote controller.");
	}
}
