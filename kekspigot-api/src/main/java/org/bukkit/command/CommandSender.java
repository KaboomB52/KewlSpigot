package org.bukkit.command;

import org.bukkit.Server;
import org.bukkit.permissions.Permissible;

public interface CommandSender extends Permissible {

	/**
	 * Sends this sender a message
	 *
	 * @param message Message to be displayed
	 */
	void sendMessage(String message);

	/**
	 * Sends this sender multiple messages
	 *
	 * @param messages An array of messages to be displayed
	 */
	void sendMessage(String[] messages);

	/**
	 * Returns the server instance that this command is running on
	 *
	 * @return Server instance
	 */
	Server getServer();

	/**
	 * Gets the name of this command sender
	 *
	 * @return Name of the sender
	 */
	String getName();

	/**
	 * Gets the display name of this command sender
	 *
	 * @return Display name of the sender
	 */
	String getDisplayName();

	void sendFormattedMessage(String message, Object... parameters);
}
