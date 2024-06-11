package com.minexd.spigot.command;

import com.minexd.spigot.SpigotX;
import com.minexd.spigot.knockback.CraftKnockbackProfile;
import com.minexd.spigot.knockback.KnockbackProfile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class KnockbackCommand extends Command {

    public KnockbackCommand() {
        super("knockback");

        this.setAliases(Collections.singletonList("kb"));
        this.setUsage(StringUtils.join(new String[]{
                ChatColor.GOLD + "Knockback commands:",
                ChatColor.YELLOW + "/kb list" + ChatColor.GRAY + " - " + ChatColor.GREEN + "List all profiles",
                ChatColor.YELLOW + "/kb create <name>" + ChatColor.GRAY + " - " + ChatColor.GREEN + "Create new profile",
                ChatColor.YELLOW + "/kb delete <name>" + ChatColor.GRAY + " - " + ChatColor.GREEN + "Delete a profile",
                ChatColor.YELLOW + "/kb load <name>" + ChatColor.GRAY + " - " + ChatColor.GREEN + "Load existing profile",
                ChatColor.YELLOW + "/kb friction <name> <double>" + ChatColor.GRAY + " - " + ChatColor.GREEN + "Set friction",
                ChatColor.YELLOW + "/kb horizontal <name> <double>" + ChatColor.GRAY + " - " + ChatColor.GREEN + "Set horizontal",
                ChatColor.YELLOW + "/kb vertical <name> <double>" + ChatColor.GRAY + " - " + ChatColor.GREEN + "Set vertical",
                ChatColor.YELLOW + "/kb extrahorizontal <name> <double>" + ChatColor.GRAY + " - " + ChatColor.GREEN + "Set extra horizontal",
                ChatColor.YELLOW + "/kb extravertical <name> <double>" + ChatColor.GRAY + " - " + ChatColor.GREEN + "Set extra vertical",
                ChatColor.YELLOW + "/kb limit <name> <double>" + ChatColor.GRAY + " - " + ChatColor.GREEN + "Set vertical limit"
        }, "\n"));
    }

    @Override
    public boolean execute(CommandSender sender, String alias, String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage(ChatColor.RED + "Unknown command.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(usageMessage);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "list": {
                List<String> messages = new ArrayList<>();

                for (KnockbackProfile profile : SpigotX.INSTANCE.getConfig().getKbProfiles()) {
                    boolean current = SpigotX.INSTANCE.getConfig().getCurrentKb().getName().equals(profile.getName());

                    messages.add((current ? ChatColor.GRAY + "-> " : "") + ChatColor.GOLD + profile.getName());

                    for (String value : profile.getValues()) {
                        messages.add(ChatColor.GRAY + " * " + value);
                    }
                }

                sender.sendMessage(ChatColor.GOLD + "Knockback profiles:");
                sender.sendMessage(StringUtils.join(messages, "\n"));
            }
            break;
            case "create": {
                if (args.length > 1) {
                    String name = args[1];

                    for (KnockbackProfile profile : SpigotX.INSTANCE.getConfig().getKbProfiles()) {
                        if (profile.getName().equalsIgnoreCase(name)) {
                            sender.sendMessage(ChatColor.RED + "A knockback profile with that name already exists.");
                            return true;
                        }
                    }

                    CraftKnockbackProfile profile = new CraftKnockbackProfile(name);

                    profile.save();

                    SpigotX.INSTANCE.getConfig().getKbProfiles().add(profile);

                    sender.sendMessage(ChatColor.GOLD + "You created a new profile " + ChatColor.GREEN + name + ChatColor.GOLD + ".");
                } else {
                    sender.sendMessage(ChatColor.RED + "Usage: /kb create <name>");
                }
            }
            break;
            case "delete": {
                if (args.length > 1) {
                    final String name = args[1];

                    if (SpigotX.INSTANCE.getConfig().getCurrentKb().getName().equalsIgnoreCase(name)) {
                        sender.sendMessage(ChatColor.RED + "You cannot delete the profile that is being used.");
                        return true;
                    } else {
                        if (SpigotX.INSTANCE.getConfig().getKbProfiles().removeIf(profile -> profile.getName().equalsIgnoreCase(name))) {
                            SpigotX.INSTANCE.getConfig().set("knockback.profiles." + name, null);
                            sender.sendMessage(ChatColor.GOLD + "You deleted the profile " + ChatColor.GREEN + name + ChatColor.GOLD + ".");
                        } else {
                            sender.sendMessage(ChatColor.RED + "A profile with that name could not be found.");
                        }

                        return true;
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "Usage: /kb delete <name>");
                }
            }
            break;
            case "load": {
                if (args.length > 1) {
                    KnockbackProfile profile = SpigotX.INSTANCE.getConfig().getKbProfileByName(args[1]);

                    if (profile == null) {
                        sender.sendMessage(ChatColor.RED + "A profile with that name could not be found.");
                        return true;
                    }

                    SpigotX.INSTANCE.getConfig().setCurrentKb(profile);
                    SpigotX.INSTANCE.getConfig().set("knockback.current", profile.getName());
                    SpigotX.INSTANCE.getConfig().save();

                    sender.sendMessage(ChatColor.GOLD + "You loaded the profile " + ChatColor.GREEN + profile.getName() + ChatColor.GOLD + ".");
                    return true;
                }
            }
            case "friction": {
                if (args.length == 3 && NumberUtils.isNumber(args[2])) {
                    KnockbackProfile profile = SpigotX.INSTANCE.getConfig().getKbProfileByName(args[1]);

                    if (profile == null) {
                        sender.sendMessage(ChatColor.RED + "A profile with that name could not be found.");
                        return true;
                    }

                    profile.setFriction(Double.parseDouble(args[2]));
                    profile.save();

                    sender.sendMessage(ChatColor.GOLD + "You have updated " + ChatColor.GREEN + profile.getName() + ChatColor.GOLD + "'s values to:");

                    for (String value : profile.getValues()) {
                        sender.sendMessage(ChatColor.GRAY + "* " + value);
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "Wrong syntax.");
                }
            }
            break;
            case "horizontal": {
                if (args.length == 3 && NumberUtils.isNumber(args[2])) {
                    KnockbackProfile profile = SpigotX.INSTANCE.getConfig().getKbProfileByName(args[1]);

                    if (profile == null) {
                        sender.sendMessage(ChatColor.RED + "A profile with that name could not be found.");
                        return true;
                    }

                    profile.setHorizontal(Double.parseDouble(args[2]));
                    profile.save();

                    sender.sendMessage(ChatColor.GOLD + "You have updated " + ChatColor.GREEN + profile.getName() + ChatColor.GOLD + "'s values to:");

                    for (String value : profile.getValues()) {
                        sender.sendMessage(ChatColor.GRAY + "* " + value);
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "Wrong syntax.");
                }
            }
            break;
            case "vertical": {
                if (args.length == 3 && NumberUtils.isNumber(args[2])) {
                    KnockbackProfile profile = SpigotX.INSTANCE.getConfig().getKbProfileByName(args[1]);

                    if (profile == null) {
                        sender.sendMessage(ChatColor.RED + "A profile with that name could not be found.");
                        return true;
                    }

                    profile.setVertical(Double.parseDouble(args[2]));
                    profile.save();

                    sender.sendMessage(ChatColor.GOLD + "You have updated " + ChatColor.GREEN + profile.getName() + ChatColor.GOLD + "'s values to:");

                    for (String value : profile.getValues()) {
                        sender.sendMessage(ChatColor.GRAY + "* " + value);
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "Wrong syntax.");
                }
            }
            break;
            case "extrahorizontal": {
                if (args.length == 3 && NumberUtils.isNumber(args[2])) {
                    KnockbackProfile profile = SpigotX.INSTANCE.getConfig().getKbProfileByName(args[1]);

                    if (profile == null) {
                        sender.sendMessage(ChatColor.RED + "A profile with that name could not be found.");
                        return true;
                    }

                    profile.setExtraHorizontal(Double.parseDouble(args[2]));
                    profile.save();

                    sender.sendMessage(ChatColor.GOLD + "You have updated " + ChatColor.GREEN + profile.getName() + ChatColor.GOLD + "'s values to:");

                    for (String value : profile.getValues()) {
                        sender.sendMessage(ChatColor.GRAY + "* " + value);
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "Wrong syntax.");
                }
            }
            break;
            case "extravertical": {
                if (args.length == 3 && NumberUtils.isNumber(args[2])) {
                    KnockbackProfile profile = SpigotX.INSTANCE.getConfig().getKbProfileByName(args[1]);

                    if (profile == null) {
                        sender.sendMessage(ChatColor.RED + "A profile with that name could not be found.");
                        return true;
                    }

                    profile.setExtraVertical(Double.parseDouble(args[2]));
                    profile.save();

                    sender.sendMessage(ChatColor.GOLD + "You have updated " + ChatColor.GREEN + profile.getName() + ChatColor.GOLD + "'s values to:");

                    for (String value : profile.getValues()) {
                        sender.sendMessage(ChatColor.GRAY + "* " + value);
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "Wrong syntax.");
                }
            }
            break;
            case "limit": {
                if (args.length == 3 && NumberUtils.isNumber(args[2])) {
                    KnockbackProfile profile = SpigotX.INSTANCE.getConfig().getKbProfileByName(args[1]);

                    if (profile == null) {
                        sender.sendMessage(ChatColor.RED + "A profile with that name could not be found.");
                        return true;
                    }

                    profile.setVerticalLimit(Double.parseDouble(args[2]));
                    profile.save();

                    sender.sendMessage(ChatColor.GOLD + "You have updated " + ChatColor.GREEN + profile.getName() + ChatColor.GOLD + "'s values to:");

                    for (String value : profile.getValues()) {
                        sender.sendMessage(ChatColor.GRAY + "* " + value);
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "Wrong syntax.");
                }
            }
            break;
            default: {
                sender.sendMessage(usageMessage);
            }
        }

        return true;
    }

}
