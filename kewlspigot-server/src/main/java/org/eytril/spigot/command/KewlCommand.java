package org.eytril.spigot.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.eytril.spigot.KewlSpigot;

import java.util.Collections;
import java.util.stream.Stream;

public class KewlCommand extends Command {

    private final String[] message = Stream.of(
                    "",
                    "§b§lKEWL SPIGOT §7(" + KewlSpigot.version + "):",
                    "",
                    "§cMade with §4❤ §cby our contributors:", // The real heroes...
                    " * §6Creator: §fKaboomB52",
                    " * §eContributor: §fzanon7",
                    "",
                    "§3Give us money: §ahttps://ko-fi.com/ianrich",
                    "§3Visit us: §bhttps://github.com/KaboomB52/KewlSpigot",
                    ""
            )
            .toArray(String[]::new);
    public KewlCommand() {
        super("kewl");
        this.setAliases(Collections.singletonList("kewlspigot"));
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {

        sender.sendMessage(message);

        return true;
    }
    // really kewl -- kab
}
