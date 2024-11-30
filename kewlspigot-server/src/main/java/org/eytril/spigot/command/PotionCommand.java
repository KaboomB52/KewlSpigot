package org.eytril.spigot.command;

import net.md_5.bungee.api.ChatColor;
import org.apache.commons.lang3.text.WordUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.eytril.spigot.KewlConfig;
import org.eytril.spigot.KewlSpigot;
import org.eytril.spigot.knockback.KnockbackProfile;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PotionCommand extends Command {

    private final String separator = "§7§m-----------------------------";

    private final String[] help = Stream.of(
                    "",
                    "§3Potion Commands:",
                    " * §b/potion §fspeed §7<speed>",
                    " * §b/potion §multiplier §7<distance>",
                    " * §b/potion §foffset §7<offset>",
                    " * §b/potion §fsmooth §7<true|false>",
                    ""
            )
            .toArray(String[]::new);
    private final List<String> SUB_COMMANDS = Arrays.asList(
            "list",
            "speed",
            "multiplier",
            "offset",
            "smooth"
    );

    public PotionCommand() {
        super("potion");
        setDescription("Set potion values");
        setAliases(Collections.singletonList("pot"));
        setPermission("kewl.pot");
    }

    @Override
    public boolean execute(CommandSender s, String currentAlias, String[] args) {
        if (testPermission(s)) {
            if(args.length == 0) {
                sendHelp(s);
                return true;
            } else if(args.length >= 2) {
                switch (args[0].toLowerCase()) {
                    case "multiplier": {
                        KewlSpigot.INSTANCE.getConfig().set("potion-throw-multiplier", Float.valueOf(args[1]));
                        KewlSpigot.INSTANCE.getConfig().setPotionFallSpeed(Float.valueOf(args[1]));
                        s.sendMessage(ChatColor.WHITE + "You've set potion throw multiplier to: " + ChatColor.AQUA + Float.valueOf(args[1]));
                        break;
                    }
                    case "offset": {
                        KewlSpigot.INSTANCE.getConfig().set("potion-throw-offset", Float.valueOf(args[1]));
                        KewlSpigot.INSTANCE.getConfig().setPotionThrowOffset(Float.valueOf(args[1]));
                        s.sendMessage(ChatColor.WHITE + "You've set potion throw offset to: " + ChatColor.AQUA + Float.valueOf(args[1]));
                        break;
                    }
                    case "speed": {
                        KewlSpigot.INSTANCE.getConfig().set("potion-fall-speed", Float.valueOf(args[1]));
                        KewlSpigot.INSTANCE.getConfig().setPotionFallSpeed(Float.valueOf(args[1]));
                        s.sendMessage(ChatColor.WHITE + "You've set potion fall speed to: " + ChatColor.AQUA + Float.valueOf(args[1]));
                        break;
                    }
                    case "smooth": {
                        KewlSpigot.INSTANCE.getConfig().set("smooth-heal-potions", Boolean.valueOf(args[1]));
                        KewlSpigot.INSTANCE.getConfig().setSmoothHealPotions(Boolean.valueOf(args[1]));
                        s.sendMessage(ChatColor.WHITE + "You've set smooth heal potions to: " + ChatColor.AQUA + Boolean.valueOf(args[1]));
                        break;
                    }
                    default: {
                        sendHelp(s);
                        return true;
                    }
                }
                KewlSpigot.INSTANCE.getConfig().save();
            } else {
                sendHelp(s);
            }
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(help);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) throws IllegalArgumentException {
        if (args.length > 0
                && SUB_COMMANDS.contains(args[0].toLowerCase())) {
            if (args.length == 2) {
                return KewlSpigot.INSTANCE.getConfig().getKbProfiles()
                        .stream()
                        .sorted((o1, o2) -> String.CASE_INSENSITIVE_ORDER.compare(o1.getName(), o2.getName()))
                        .map(KnockbackProfile::getName)
                        .collect(Collectors.toList());
            }
        } else {
            return SUB_COMMANDS;
        }

        return super.tabComplete(sender, alias, args);
    }

}

