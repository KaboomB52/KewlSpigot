package org.eytril.spigot.command;

import org.eytril.spigot.KeKSpigot;
import org.eytril.spigot.knockback.CraftKnockbackProfile;
import org.eytril.spigot.knockback.KnockbackProfile;

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
                ChatColor.DARK_AQUA + "Knockback Commands:",
                ChatColor.AQUA + "/kb list" + ChatColor.GRAY + " - " + ChatColor.WHITE + "List all profiles",
                ChatColor.AQUA + "/kb create <name>" + ChatColor.GRAY + " - " + ChatColor.WHITE + "Create new profile",
                ChatColor.AQUA + "/kb delete <name>" + ChatColor.GRAY + " - " + ChatColor.WHITE + "Delete a profile",
                ChatColor.AQUA + "/kb load <name>" + ChatColor.GRAY + " - " + ChatColor.WHITE + "Load existing profile",
                ChatColor.AQUA + "/kb friction <name> <double>" + ChatColor.GRAY + " - " + ChatColor.WHITE + "Set friction",
                ChatColor.AQUA + "/kb horizontal <name> <double>" + ChatColor.GRAY + " - " + ChatColor.WHITE + "Set horizontal",
                ChatColor.AQUA + "/kb vertical <name> <double>" + ChatColor.GRAY + " - " + ChatColor.WHITE + "Set vertical",
                ChatColor.AQUA + "/kb extrahorizontal <name> <double>" + ChatColor.GRAY + " - " + ChatColor.WHITE + "Set extra horizontal",
                ChatColor.AQUA + "/kb extravertical <name> <double>" + ChatColor.GRAY + " - " + ChatColor.WHITE + "Set extra vertical",
                ChatColor.AQUA + "/kb limit <name> <double>" + ChatColor.GRAY + " - " + ChatColor.WHITE + "Set vertical limit"
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

                for (KnockbackProfile profile : KeKSpigot.INSTANCE.getConfig().getKbProfiles()) {
                    boolean current = KeKSpigot.INSTANCE.getConfig().getCurrentKb().getName().equals(profile.getName());

                    messages.add(ChatColor.AQUA + profile.getName() + (current ? ChatColor.GREEN + " [Active]" : ""));

                    for (String value : profile.getValues()) {
                        messages.add(ChatColor.WHITE + " * " + value);
                    }
                }

                sender.sendMessage(ChatColor.DARK_AQUA + "Knockback Profiles:");
                sender.sendMessage(StringUtils.join(messages, "\n"));
            }
            break;
            case "create": {
                if (args.length > 1) {
                    String name = args[1];

                    for (KnockbackProfile profile : KeKSpigot.INSTANCE.getConfig().getKbProfiles()) {
                        if (profile.getName().equalsIgnoreCase(name)) {
                            sender.sendMessage(ChatColor.RED + "A knockback profile with that name already exists.");
                            return true;
                        }
                    }

                    CraftKnockbackProfile profile = new CraftKnockbackProfile(name);

                    profile.save();

                    KeKSpigot.INSTANCE.getConfig().getKbProfiles().add(profile);

                    sender.sendMessage(ChatColor.AQUA + "You created a new profile " + ChatColor.WHITE + name + ChatColor.AQUA + ".");
                } else {
                    sender.sendMessage(ChatColor.RED + "Usage: /kb create <name>");
                }
            }
            break;
            case "delete": {
                if (args.length > 1) {
                    final String name = args[1];

                    if (KeKSpigot.INSTANCE.getConfig().getCurrentKb().getName().equalsIgnoreCase(name)) {
                        sender.sendMessage(ChatColor.RED + "You cannot delete the profile that is being used.");
                        return true;
                    } else {
                        if (KeKSpigot.INSTANCE.getConfig().getKbProfiles().removeIf(profile -> profile.getName().equalsIgnoreCase(name))) {
                            KeKSpigot.INSTANCE.getConfig().set("knockback.profiles." + name, null);
                            sender.sendMessage(ChatColor.RED + "You deleted the profile " + ChatColor.WHITE + name + ChatColor.RED + ".");
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
                    KnockbackProfile profile = KeKSpigot.INSTANCE.getConfig().getKbProfileByName(args[1]);

                    if (profile == null) {
                        sender.sendMessage(ChatColor.RED + "A profile with that name could not be found.");
                        return true;
                    }

                    KeKSpigot.INSTANCE.getConfig().setCurrentKb(profile);
                    KeKSpigot.INSTANCE.getConfig().set("knockback.current", profile.getName());
                    KeKSpigot.INSTANCE.getConfig().save();

                    sender.sendMessage(ChatColor.AQUA + "You loaded the profile " + ChatColor.WHITE + profile.getName() + ChatColor.AQUA + ".");
                    return true;
                }
            }
            case "friction": {
                if (args.length == 3 && NumberUtils.isNumber(args[2])) {
                    KnockbackProfile profile = KeKSpigot.INSTANCE.getConfig().getKbProfileByName(args[1]);

                    if (profile == null) {
                        sender.sendMessage(ChatColor.RED + "A profile with that name could not be found.");
                        return true;
                    }

                    profile.setFriction(Double.parseDouble(args[2]));
                    profile.save();

                    sender.sendMessage(ChatColor.AQUA + "You have updated " + ChatColor.WHITE + profile.getName() + ChatColor.AQUA + "'s values to:");

                    for (String value : profile.getValues()) {
                        sender.sendMessage(ChatColor.WHITE + "* " + value);
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "Wrong syntax.");
                }
            }
            break;
            case "horizontal": {
                if (args.length == 3 && NumberUtils.isNumber(args[2])) {
                    KnockbackProfile profile = KeKSpigot.INSTANCE.getConfig().getKbProfileByName(args[1]);

                    if (profile == null) {
                        sender.sendMessage(ChatColor.RED + "A profile with that name could not be found.");
                        return true;
                    }

                    profile.setHorizontal(Double.parseDouble(args[2]));
                    profile.save();

                    sender.sendMessage(ChatColor.AQUA + "You have updated " + ChatColor.WHITE + profile.getName() + ChatColor.AQUA + "'s values to:");

                    for (String value : profile.getValues()) {
                        sender.sendMessage(ChatColor.WHITE + "* " + value);
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "Wrong syntax.");
                }
            }
            break;
            case "vertical": {
                if (args.length == 3 && NumberUtils.isNumber(args[2])) {
                    KnockbackProfile profile = KeKSpigot.INSTANCE.getConfig().getKbProfileByName(args[1]);

                    if (profile == null) {
                        sender.sendMessage(ChatColor.RED + "A profile with that name could not be found.");
                        return true;
                    }

                    profile.setVertical(Double.parseDouble(args[2]));
                    profile.save();

                    sender.sendMessage(ChatColor.AQUA + "You have updated " + ChatColor.WHITE + profile.getName() + ChatColor.AQUA + "'s values to:");

                    for (String value : profile.getValues()) {
                        sender.sendMessage(ChatColor.WHITE + "* " + value);
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "Wrong syntax.");
                }
            }
            break;
            case "extrahorizontal": {
                if (args.length == 3 && NumberUtils.isNumber(args[2])) {
                    KnockbackProfile profile = KeKSpigot.INSTANCE.getConfig().getKbProfileByName(args[1]);

                    if (profile == null) {
                        sender.sendMessage(ChatColor.RED + "A profile with that name could not be found.");
                        return true;
                    }

                    profile.setExtraHorizontal(Double.parseDouble(args[2]));
                    profile.save();

                    sender.sendMessage(ChatColor.AQUA + "You have updated " + ChatColor.WHITE + profile.getName() + ChatColor.AQUA + "'s values to:");

                    for (String value : profile.getValues()) {
                        sender.sendMessage(ChatColor.WHITE + "* " + value);
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "Wrong syntax.");
                }
            }
            break;
            case "extravertical": {
                if (args.length == 3 && NumberUtils.isNumber(args[2])) {
                    KnockbackProfile profile = KeKSpigot.INSTANCE.getConfig().getKbProfileByName(args[1]);

                    if (profile == null) {
                        sender.sendMessage(ChatColor.RED + "A profile with that name could not be found.");
                        return true;
                    }

                    profile.setExtraVertical(Double.parseDouble(args[2]));
                    profile.save();

                    sender.sendMessage(ChatColor.AQUA + "You have updated " + ChatColor.WHITE + profile.getName() + ChatColor.AQUA + "'s values to:");

                    for (String value : profile.getValues()) {
                        sender.sendMessage(ChatColor.WHITE + "* " + value);
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "Wrong syntax.");
                }
            }
            break;
            case "limit": {
                if (args.length == 3 && NumberUtils.isNumber(args[2])) {
                    KnockbackProfile profile = KeKSpigot.INSTANCE.getConfig().getKbProfileByName(args[1]);

                    if (profile == null) {
                        sender.sendMessage(ChatColor.RED + "A profile with that name could not be found.");
                        return true;
                    }

                    profile.setVerticalLimit(Double.parseDouble(args[2]));
                    profile.save();

                    sender.sendMessage(ChatColor.AQUA + "You have updated " + ChatColor.WHITE + profile.getName() + ChatColor.AQUA + "'s values to:");

                    for (String value : profile.getValues()) {
                        sender.sendMessage(ChatColor.WHITE + "* " + value);
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
