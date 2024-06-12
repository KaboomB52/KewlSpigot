package org.eytril.spigot.handler;

import net.minecraft.server.Packet;
import net.minecraft.server.PlayerConnection;

public interface PacketHandler {

    void handleReceivedPacket(PlayerConnection connection, Packet packet);

    void handleSentPacket(PlayerConnection connection, Packet packet);

}
