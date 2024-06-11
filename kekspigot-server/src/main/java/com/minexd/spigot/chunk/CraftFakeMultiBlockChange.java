package com.minexd.spigot.chunk;

import com.minexd.spigot.chunk.FakeMultiBlockChange;
import net.minecraft.server.PacketPlayOutMultiBlockChange;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

public class CraftFakeMultiBlockChange implements FakeMultiBlockChange {

	private final PacketPlayOutMultiBlockChange packet;

	public CraftFakeMultiBlockChange(PacketPlayOutMultiBlockChange packet) {
		this.packet = packet;
	}

	@Override
	public void sendTo(Player player) {
		((CraftPlayer) player).getHandle().playerConnection.sendPacket(this.packet);
	}
}
