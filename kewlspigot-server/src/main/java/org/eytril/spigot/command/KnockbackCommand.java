package org.eytril.spigot.command;

import net.md_5.bungee.api.chat.TextComponent;
import org.apache.commons.lang3.math.NumberUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.eytril.spigot.KewlSpigot;
import org.eytril.spigot.knockback.CraftKnockbackProfile;
import org.eytril.spigot.knockback.KnockbackProfile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class KnockbackCommand extends Command {

    private final String separator = "§7§m-----------------------------";

    private final String[] help = Stream.of(
            "",
                    "§3Knockback Commands:",
                    " * §b/knockback §flist",
                    " * §b/knockback §fcreate §7<profile>",
                    " * §b/knockback §fdelete §7<profile>",
                    " * §b/knockback §fload §7<profile>",
                    " * §b/knockback §fview §7<profile>",
                    " * §b/knockback §fedit §7<profile> <variable> <value>", // far better than the old system
                    " * §b/knockback §fset §7<profile> <player>",
                    ""
            )
            .toArray(String[]::new);
    private final List<String> SUB_COMMANDS = Arrays.asList(
            "list",
            "create",
            "delete",
            "load",
            "view",
            "edit",
            "set"
    );

    public KnockbackCommand() {
        super("knockback");
        this.setAliases(Collections.singletonList("kb"));
        this.setPermission("kewl.kb");
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!testPermission(sender)) return true;

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender);
            return true;
        }

        switch (args.length) {
            case 2: {
                switch (args[0].toLowerCase()) {
                    case "create": {
                        if (!isProfileName(args[1])) {
                            CraftKnockbackProfile profile = new CraftKnockbackProfile(args[1]);
                            KewlSpigot.INSTANCE.getConfig().getKbProfiles().add(profile);
                            profile.save();
                            knockbackCommandMain(sender);
                            sender.sendMessage("§aThe profile §e" + args[1] + " §ahas been created.");
                            return true;
                        } else {
                            sender.sendMessage("§cA knockback profile with that name already exists.");
                        }
                        break;
                    }
                    case "delete": {
                        if (KewlSpigot.INSTANCE.getConfig().getCurrentKb().getName().equalsIgnoreCase(args[1])) {
                            knockbackCommandMain(sender);
                            sender.sendMessage("§cYou cannot delete the profile that is being used.");
                            return false;
                        }
                        if (KewlSpigot.INSTANCE.getConfig().getKbProfiles().removeIf(profile -> profile.getName().equalsIgnoreCase(args[1]))) {
                            KewlSpigot.INSTANCE.getConfig().set("knockback.profiles." + args[1], null);
                            knockbackCommandMain(sender);
                            sender.sendMessage("§aThe profile §e" + args[1] + " §ahas been removed.");
                            return true;
                        } else {
                            sender.sendMessage("§cThis profile doesn't exist.");
                        }
                        break;
                    }
                    case "load": {
                        KnockbackProfile profile = KewlSpigot.INSTANCE.getConfig().getKbProfileByName(args[1]);
                        if (profile != null) {
                            if (KewlSpigot.INSTANCE.getConfig().getCurrentKb().getName().equalsIgnoreCase(args[1])) {
                                sender.sendMessage("§cThis profile is loaded.");
                                return false;
                            }
                            KewlSpigot.INSTANCE.getConfig().setCurrentKb(profile);
                            KewlSpigot.INSTANCE.getConfig().set("knockback.current", profile.getName());
                            KewlSpigot.INSTANCE.getConfig().save();
                            knockbackCommandMain(sender);
                            sender.sendMessage("§aThe profile §e" + args[1] + " §ahas been loaded.");
                            return true;
                        } else {
                            sender.sendMessage("§cThis profile doesn't exist.");
                        }
                        break;
                    }
                    default: {
                        knockbackCommandMain(sender);
                    }
                }
                break;
            }
            case 3: {
                switch (args[0].toLowerCase()) {
                    case "set": {
                        KnockbackProfile profile = KewlSpigot.INSTANCE.getConfig().getKbProfileByName(args[1]);
                        if (profile == null) {
                            sender.sendMessage("§cA profile with that name could not be found.");
                            return false;
                        }
                        Player target = Bukkit.getPlayer(args[2]);
                        if (target == null) {
                            sender.sendMessage("§cThat player is not online.");
                            return false;
                        }
                        target.setKnockbackProfile(profile);
                        break;
                    }
                }
                break;
            }
            case 4: {
                if (args[0].equalsIgnoreCase("edit")) {
                    KnockbackProfile profile = KewlSpigot.INSTANCE.getConfig().getKbProfileByName(args[1].toLowerCase());
                    if (profile == null) {
                        sender.sendMessage("§cThis profile doesn't exist.");
                        return false;
                    }
                    switch (args[2].toLowerCase()) {
                        case "horfriction": case "horizontalfriction": {
                            if (!org.apache.commons.lang3.math.NumberUtils.isNumber(args[3])) {
                                sender.sendMessage("§f" + args[3] + " §c is not a number.");
                                return false;
                            }
                            double value = Double.parseDouble(args[3]);
                            profile.setHorizontalFriction(value);
                            profile.save();
                            sender.sendMessage("§aChanged §f\"" + profile.getName() + "\"§a's §fhorizontal-friction §asetting to §f" + args[3] + "§a.");
                            break;
                        }
                        case "verfriction": case "verticalfriction": {
                            if (!org.apache.commons.lang3.math.NumberUtils.isNumber(args[3])) {
                                sender.sendMessage("§f" + args[3] + " §c is not a number.");
                                return false;
                            }
                            double value = Double.parseDouble(args[3]);
                            profile.setVerticalFriction(value);
                            profile.save();
                            sender.sendMessage("§aChanged §f\"" + profile.getName() + "\"§a's §fvertical-friction §asetting to §f" + args[3] + "§a.");
                            break;
                        }
                        case "hor": case "horizontal": {
                            if (!org.apache.commons.lang3.math.NumberUtils.isNumber(args[3])) {
                                sender.sendMessage("§f" + args[3] + " §c is not a number.");
                                return false;
                            }
                            double value = Double.parseDouble(args[3]);
                            profile.setHorizontal(value);
                            profile.save();
                            sender.sendMessage("§aChanged §f\"" + profile.getName() + "\"§a's §fhorizontal §asetting to §f" + args[3] + "§a.");
                            break;
                        }
                        case "vert": case "vertical": {
                            if (!org.apache.commons.lang3.math.NumberUtils.isNumber(args[3])) {
                                sender.sendMessage("§f" + args[3] + " §c is not a number.");
                                return false;
                            }
                            double value = Double.parseDouble(args[3]);
                            profile.setVertical(value);
                            profile.save();
                            sender.sendMessage("§aChanged §f\"" + profile.getName() + "\"§a's §fvertical §asetting to §f" + args[3] + "§a.");
                            break;
                        }
                        case "extrahor": case "extrahorizontal": {
                            if (!org.apache.commons.lang3.math.NumberUtils.isNumber(args[3])) {
                                sender.sendMessage("§f" + args[3] + " §c is not a number.");
                                return false;
                            }
                            double value = Double.parseDouble(args[3]);
                            profile.setExtraHorizontal(value);
                            profile.save();
                            sender.sendMessage("§aChanged §f\"" + profile.getName() + "\"§a's §fextra-horizontal §asetting to §f" + args[3] + "§a.");
                            break;
                        }
                        case "extravert": case "extravertical": {
                            if (!org.apache.commons.lang3.math.NumberUtils.isNumber(args[3])) {
                                sender.sendMessage("§f" + args[3] + " §c is not a number.");
                                return false;
                            }
                            double value = Double.parseDouble(args[3]);
                            profile.setExtraVertical(value);
                            profile.save();
                            sender.sendMessage("§aChanged §f\"" + profile.getName() + "\"§a's §fextra-vertical §asetting to §f" + args[3] + "§a.");
                            break;
                        }
                        case "vertmax": case "verticalmax": {
                            if (!NumberUtils.isNumber(args[3])) {
                                sender.sendMessage("§f" + args[3] + " §c is not a number.");
                                return false;
                            }
                            double value = Double.parseDouble(args[3]);
                            profile.setVerticalLimit(value);
                            profile.save();
                            sender.sendMessage("§aChanged §f\"" + profile.getName() + "\"§a's §fvertical-max §asetting to §f" + args[3] + "§a.");
                            break;
                        }
                    }
                }
                break;
            }
            default: {
                knockbackCommandMain(sender);
            }
        }
        return false;
    }

    private void knockbackCommandMain(CommandSender sender) {
        sender.sendMessage("" + "\n" + "§3Knockback Profiles:\n\n"); // most people make this smaller/simpler but for a lot of people its easier to just see them all

        for (KnockbackProfile profile : KewlSpigot.INSTANCE.getConfig().getKbProfiles()) {
            boolean current = KewlSpigot.INSTANCE.getConfig().getCurrentKb().getName().equals(profile.getName());
            sender.sendMessage(profile.getName() + (current ? ChatColor.GREEN + " [Active]" : ""));

            for (String values : profile.getValues()) {
                sender.sendMessage(" * §b" + values);
            }
        }
        sender.sendMessage("");
    }

    private boolean isProfileName(String name) {
        for (KnockbackProfile profile : KewlSpigot.INSTANCE.getConfig().getKbProfiles()) {
            if (profile.getName().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
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

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(help);
    }

}
