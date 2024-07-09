package org.spigotmc;


import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.eytril.spigot.util.DateUtil;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;

import net.minecraft.server.MinecraftServer;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class TicksPerSecondCommand extends Command {

	public TicksPerSecondCommand(String name) {
		super(name);
		this.description = "Gets the current ticks per second for the server";
		this.usageMessage = "/tps";
		this.setPermission("bukkit.command.tps");
	}

	private static String format(final double tps) {
		return String.format("%s%s%s", (tps > 18.0 ? ChatColor.YELLOW : tps > 16.0 ? ChatColor.YELLOW : ChatColor.RED), tps > 20.0 ? "*" : "", Math.min(Math.round(tps * 100.0) / 100.0, 20.0));
	}

	@Override
	public boolean execute(final CommandSender sender, final String currentAlias, final String[] args) {
		if (!this.testPermission(sender)) {
			return true;
		}

		final double[] tps = Bukkit.spigot().getTPS();
		final double tpsNow = Bukkit.spigot().getTPS()[0];
		final double roundTPS = Math.round(tpsNow * 100.0) / 100.0;
		final String[] tpsAvg = new String[tps.length];
		final String[] tps2 = new String[3];

		for (int i = 0; i < tps2.length; ++i) {
			tps2[i] = format(tps[i]);
		}

		for (int i = 0; i < tps.length; ++i) {
			tpsAvg[i] = format(tps[i]);
		}

		int totalEntities = 0;
		for (final World world : Bukkit.getServer().getWorlds()) {
			totalEntities += world.getEntities().size();
		}

		final double lag = (double)Math.round((1.0 - tpsNow / 20.0) * 100.0);
		final long usedMemory = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 2L / 1048576L;
		final long allocatedMemory = Runtime.getRuntime().totalMemory() / 1048576L;
		final OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
		final World world2 = (sender instanceof Player) ? ((Player)sender).getWorld() : Bukkit.getWorlds().get(0);
		final Chunk[] loadedChunks = world2.getLoadedChunks();

		sender.sendMessage("");
		sender.sendMessage("§3Performance §7[" + Bukkit.getOnlinePlayers().size() + " Online]§3:");
		sender.sendMessage("");
		sender.sendMessage("§bUptime: §f" + DateUtil.formatDateDiff(ManagementFactory.getRuntimeMXBean().getStartTime()));
		sender.sendMessage("§bTPS: §f" + format(tpsNow));
		sender.sendMessage("§bLag: §f" + Math.round(lag * 10000.0) / 10000.0);
		sender.sendMessage("");
		sender.sendMessage("§bEntities: §f" + totalEntities);
		sender.sendMessage("§bChunks: §f" + loadedChunks.length);
		sender.sendMessage("");
		sender.sendMessage("§bMemory: §f" + usedMemory + "/" + allocatedMemory + "MB");
		sender.sendMessage("§bFull tick: §f" + MinecraftServer.LAST_TICK_TIME);
		sender.sendMessage("");

		return true;
	}
}
