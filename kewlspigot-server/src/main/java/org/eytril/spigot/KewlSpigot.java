package org.eytril.spigot;

import org.eytril.spigot.command.KewlCommand;
import org.eytril.spigot.command.PotionCommand;
import org.eytril.spigot.command.TPSCommand;
import org.eytril.spigot.handler.MovementHandler;
import org.eytril.spigot.command.KnockbackCommand;
import org.eytril.spigot.handler.PacketHandler;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import net.minecraft.server.MinecraftServer;

import org.bukkit.command.Command;

public enum KewlSpigot {

	INSTANCE;

    private KewlConfig config;

    public static String version = "1.0.4";
    private Set<PacketHandler> packetHandlers = new HashSet<>();
    private Set<MovementHandler> movementHandlers = new HashSet<>();

    public static final ExecutorService asyncExecutor = Executors.newFixedThreadPool(8); // adjust as needed

    public KewlConfig getConfig() {
        return this.config;
    }

    public Set<PacketHandler> getPacketHandlers() {
        return this.packetHandlers;
    }

    public Set<MovementHandler> getMovementHandlers() {
        return this.movementHandlers;
    }

    public void setConfig(KewlConfig config) {
        this.config = config;
    }

	public void addPacketHandler(PacketHandler handler) {
		this.packetHandlers.add(handler);
	}

	public void addMovementHandler(MovementHandler handler) {
		this.movementHandlers.add(handler);
	}

	public void registerCommands() {
		Map<String, Command> commands = new HashMap<>();

		commands.put("knockback", new KnockbackCommand());
        commands.put("kewl", new KewlCommand());
        commands.put("potion", new PotionCommand());
        commands.put("tps", new TPSCommand());

		for (Map.Entry<String, Command> entry : commands.entrySet()) {
			MinecraftServer.getServer().server.getCommandMap().register(entry.getKey(), "Spigot", entry.getValue());
		}
	}

}
