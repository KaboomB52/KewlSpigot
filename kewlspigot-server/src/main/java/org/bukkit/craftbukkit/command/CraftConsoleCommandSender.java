package org.bukkit.craftbukkit.command;

import org.bukkit.ChatColor;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.conversations.Conversation;
import org.bukkit.conversations.ConversationAbandonedEvent;
import org.bukkit.conversations.ManuallyAbandonedConversationCanceller;
import org.bukkit.craftbukkit.conversations.ConversationTracker;
import org.bukkit.permissions.Permission;

import java.text.MessageFormat;

/**
 * Represents CLI input from a console
 */
public class CraftConsoleCommandSender extends ServerCommandSender implements ConsoleCommandSender {

	protected final ConversationTracker conversationTracker = new ConversationTracker();

	protected CraftConsoleCommandSender() {
		super();
	}

	public void sendMessage(String message) {
		sendRawMessage(message);
	}

	public void sendRawMessage(String message) {
		System.out.println(ChatColor.stripColor(message));
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
		return "Console";
	}

	@Override
	public String getDisplayName() {
		return ChatColor.DARK_RED + "Console";
	}

	public boolean isOp() {
		return true;
	}

	public void setOp(boolean value) {
		throw new UnsupportedOperationException("Cannot change operator status of server console");
	}

	@Override
	public boolean hasPermission(String name) {
		return true;
	}

	@Override
	public boolean hasPermission(Permission permission) {
		return true;
	}

	public boolean beginConversation(Conversation conversation) {
		return conversationTracker.beginConversation(conversation);
	}

	public void abandonConversation(Conversation conversation) {
		conversationTracker.abandonConversation(conversation, new ConversationAbandonedEvent(conversation, new ManuallyAbandonedConversationCanceller()));
	}

	public void abandonConversation(Conversation conversation, ConversationAbandonedEvent details) {
		conversationTracker.abandonConversation(conversation, details);
	}

	public void acceptConversationInput(String input) {
		conversationTracker.acceptConversationInput(input);
	}

	public boolean isConversing() {
		return conversationTracker.isConversing();
	}
}
