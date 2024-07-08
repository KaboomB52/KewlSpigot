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
                    "§8§m----------------------------------------------------",
                    "§b§lKnockback Usage",
                    " §7- §b/knockback §flist",
                    " §7- §b/knockback §fcreate §7<profile>",
                    " §7- §b/knockback §fdelete §7<profile>",
                    " §7- §b/knockback §fload §7<profile>",
                    " §7- §b/knockback §fview §7<profile>",
                    " §7- §b/knockback §fedit §7<profile> <variable> <value>",
                    " §7- §b/knockback §fset §7<profile> <player>",
                    "§8§m----------------------------------------------------"
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
        if(!(sender instanceof Player)) {
            sender.sendMessage("You must be a player to use this command."); // TODO
            return false;
        }
        Player player = (Player)sender;

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
                            knockbackCommandMain(player);
                            player.sendMessage("§aThe profile §e" + args[1] + " §ahas been created.");
                            return true;
                        } else {
                            player.sendMessage("§cA knockback profile with that name already exists.");
                        }
                        break;
                    }
                    case "delete": {
                        if (KewlSpigot.INSTANCE.getConfig().getCurrentKb().getName().equalsIgnoreCase(args[1])) {
                            knockbackCommandMain(player);
                            player.sendMessage("§cYou cannot delete the profile that is being used.");
                            return false;
                        }
                        if (KewlSpigot.INSTANCE.getConfig().getKbProfiles().removeIf(profile -> profile.getName().equalsIgnoreCase(args[1]))) {
                            KewlSpigot.INSTANCE.getConfig().set("knockback.profiles." + args[1], null);
                            knockbackCommandMain(player);
                            player.sendMessage("§aThe profile §e" + args[1] + " §ahas been removed.");
                            return true;
                        } else {
                            player.sendMessage("§cThis profile doesn't exist.");
                        }
                        break;
                    }
                    case "load": {
                        KnockbackProfile profile = KewlSpigot.INSTANCE.getConfig().getKbProfileByName(args[1]);
                        if (profile != null) {
                            if (KewlSpigot.INSTANCE.getConfig().getCurrentKb().getName().equalsIgnoreCase(args[1])) {
                                player.sendMessage("§cThis profile is loaded.");
                                return false;
                            }
                            KewlSpigot.INSTANCE.getConfig().setCurrentKb(profile);
                            KewlSpigot.INSTANCE.getConfig().set("knockback.current", profile.getName());
                            KewlSpigot.INSTANCE.getConfig().save();
                            knockbackCommandMain(player);
                            player.sendMessage("§aThe profile §e" + args[1] + " §ahas been loaded.");
                            return true;
                        } else {
                            player.sendMessage("§cThis profile doesn't exist.");
                        }
                        break;
                    }
                    case "view": {
                        KnockbackProfile profile = KewlSpigot.INSTANCE.getConfig().getKbProfileByName(args[1]);
                        if (profile != null) {
                            knockbackCommandView(player, profile);
                            return true;
                        }
                        player.sendMessage("§cThis profile doesn't exist.");
                        break;
                    }
                    default: {
                        knockbackCommandMain(player);
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
                        player.sendMessage("§cThis profile doesn't exist.");
                        return false;
                    }
                    switch (args[2].toLowerCase()) {
                        case "horfriction": {
                            if (!org.apache.commons.lang3.math.NumberUtils.isNumber(args[3])) {
                                player.sendMessage("§4" + args[3] + " §c is not a number.");
                                return false;
                            }
                            double value = Double.parseDouble(args[3]);
                            profile.setHorizontalFriction(value);
                            profile.save();
                            knockbackCommandView(player, profile);
                            player.sendMessage("§aValue edited and saved.");
                            break;
                        }
                        case "verfriction": {
                            if (!org.apache.commons.lang3.math.NumberUtils.isNumber(args[3])) {
                                player.sendMessage("§4" + args[3] + " §c is not a number.");
                                return false;
                            }
                            double value = Double.parseDouble(args[3]);
                            profile.setVerticalFriction(value);
                            profile.save();
                            knockbackCommandView(player, profile);
                            player.sendMessage("§aValue edited and saved.");
                            break;
                        }
                        case "horizontal": {
                            if (!org.apache.commons.lang3.math.NumberUtils.isNumber(args[3])) {
                                player.sendMessage("§4" + args[3] + " §c is not a number.");
                                return false;
                            }
                            double value = Double.parseDouble(args[3]);
                            profile.setHorizontal(value);
                            profile.save();
                            knockbackCommandView(player, profile);
                            player.sendMessage("§aValue edited and saved.");
                            break;
                        }
                        case "vertical": {
                            if (!org.apache.commons.lang3.math.NumberUtils.isNumber(args[3])) {
                                sender.sendMessage("§4" + args[3] + " §c is not a number.");
                                return false;
                            }
                            double value = Double.parseDouble(args[3]);
                            profile.setVertical(value);
                            profile.save();
                            knockbackCommandView(player, profile);
                            player.sendMessage("§aValue edited and saved.");
                            break;
                        }
                        case "extrahorizontal": {
                            if (!org.apache.commons.lang3.math.NumberUtils.isNumber(args[3])) {
                                player.sendMessage("§4" + args[3] + " §c is not a number.");
                                return false;
                            }
                            double value = Double.parseDouble(args[3]);
                            profile.setExtraHorizontal(value);
                            profile.save();
                            knockbackCommandView(player, profile);
                            player.sendMessage("§aValue edited and saved.");
                            break;
                        }
                        case "extravertical": {
                            if (!org.apache.commons.lang3.math.NumberUtils.isNumber(args[3])) {
                                player.sendMessage("§4" + args[3] + " §c is not a number.");
                                return false;
                            }
                            double value = Double.parseDouble(args[3]);
                            profile.setExtraVertical(value);
                            profile.save();
                            knockbackCommandView(player, profile);
                            player.sendMessage("§aValue edited and saved.");
                            break;
                        }
                        case "vertmax": {
                            if (!NumberUtils.isNumber(args[3])) {
                                player.sendMessage("§4" + args[3] + " §c is not a number.");
                                return false;
                            }
                            double value = Double.parseDouble(args[3]);
                            profile.setVerticalLimit(value);
                            profile.save();
                            knockbackCommandView(player, profile);
                            player.sendMessage("§aValue edited and saved.");
                            break;
                        }
                    }
                }
                break;
            }
            default: {
                knockbackCommandMain(player);
            }
        }
        return false;
    }

    private void knockbackCommandMain(Player player) {
        player.sendMessage(separator + "\n" + "§bKnockback profile list:\n");

        for (KnockbackProfile profile : KewlSpigot.INSTANCE.getConfig().getKbProfiles()) {
            boolean current = KewlSpigot.INSTANCE.getConfig().getCurrentKb().getName().equals(profile.getName());
            player.sendMessage("§7" + profile.getName() + (current ? ChatColor.GREEN + " [Active]" : ""));
        }
        player.sendMessage(separator);
    }
    private void knockbackCommandView(Player player, KnockbackProfile profile) {
        player.sendMessage(separator + "\n" + "§bKnockback values:\n \n");
        for (String values : profile.getValues()) {
            TextComponent value = new TextComponent("§6• §b" + values);
            player.spigot().sendMessage(value);
        }
        player.sendMessage(separator);
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
