package org.bukkit.command;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.apache.commons.lang.Validate;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.command.defaults.HelpCommand;
import org.bukkit.command.defaults.PluginsCommand;
import org.bukkit.command.defaults.ReloadCommand;
import org.bukkit.command.defaults.VanillaCommand;
import org.bukkit.command.defaults.VersionCommand;
import org.bukkit.entity.Player;
import org.bukkit.util.Java15Compat;
import org.bukkit.util.StringUtil;

public class SimpleCommandMap implements CommandMap {
    private static final Pattern PATTERN_ON_SPACE = Pattern.compile(" ", 16);
    protected final Map knownCommands = new HashMap();
    private final Server server;

    public SimpleCommandMap(Server server) {
        this.server = server;
        this.setDefaultCommands();
    }

    private void setDefaultCommands() {
        this.register("bukkit", new ReloadCommand("reload"));
        this.register("bukkit", new PluginsCommand("plugins"));
        this.register("bukkit", new VersionCommand("version"));
    }

    public void setFallbackCommands() {
        this.register("bukkit", new HelpCommand());
    }

    public void registerAll(String fallbackPrefix, List commands) {
        if (commands != null) {
            Iterator var3 = commands.iterator();

            while(var3.hasNext()) {
                Command c = (Command)var3.next();
                this.register(fallbackPrefix, c);
            }
        }

    }

    public boolean register(String fallbackPrefix, Command command) {
        return this.register(command.getName(), fallbackPrefix, command);
    }

    public boolean register(String label, String fallbackPrefix, Command command) {
        label = label.toLowerCase().trim();
        fallbackPrefix = fallbackPrefix.toLowerCase().trim();
        boolean registered = this.register(label, command, false, fallbackPrefix);
        Iterator iterator = command.getAliases().iterator();

        while(iterator.hasNext()) {
            if (!this.register((String)iterator.next(), command, true, fallbackPrefix)) {
                iterator.remove();
            }
        }

        if (!registered) {
            command.setLabel(fallbackPrefix + ":" + label);
        }

        command.register(this);
        return registered;
    }

    private synchronized boolean register(String label, Command command, boolean isAlias, String fallbackPrefix) {
        this.knownCommands.put(fallbackPrefix + ":" + label, command);
        if ((command instanceof VanillaCommand || isAlias) && this.knownCommands.containsKey(label)) {
            return false;
        } else {
            Command conflict = (Command)this.knownCommands.get(label);
            if (conflict != null && conflict.getLabel().equals(label)) {
                return false;
            } else {
                if (!isAlias) {
                    command.setLabel(label);
                }

                this.knownCommands.put(label, command);
                return true;
            }
        }
    }

    public boolean dispatch(CommandSender sender, String commandLine) throws CommandException {
        String[] args = PATTERN_ON_SPACE.split(commandLine);
        if (args.length == 0) {
            return false;
        } else {
            String sentCommandLabel = args[0].toLowerCase();
            Command target = this.getCommand(sentCommandLabel);
            if (target == null) {
                return false;
            } else {
                try {
                    target.execute(sender, sentCommandLabel, (String[])Java15Compat.Arrays_copyOfRange(args, 1, args.length));
                    return true;
                } catch (CommandException var7) {
                    CommandException ex = var7;
                    throw ex;
                } catch (Throwable var8) {
                    Throwable ex = var8;
                    throw new CommandException("Unhandled exception executing '" + commandLine + "' in " + target, ex);
                }
            }
        }
    }

    public synchronized void clearCommands() {
        Iterator var1 = this.knownCommands.entrySet().iterator();

        while(var1.hasNext()) {
            Map.Entry entry = (Map.Entry)var1.next();
            ((Command)entry.getValue()).unregister(this);
        }

        this.knownCommands.clear();
        this.setDefaultCommands();
    }

    public Command getCommand(String name) {
        return (Command)this.knownCommands.get(name.toLowerCase());
    }

    public List tabComplete(CommandSender sender, String cmdLine) {
        return this.tabComplete(sender, cmdLine, (Location)null);
    }

    public List tabComplete(CommandSender sender, String cmdLine, Location location) {
        Validate.notNull(sender, "Sender cannot be null");
        Validate.notNull(cmdLine, "Command line cannot null");
        int spaceIndex = cmdLine.indexOf(32);
        String prefix;
        if (spaceIndex == -1) {
            ArrayList completions = new ArrayList();
            Map knownCommands = this.knownCommands;
            prefix = sender instanceof Player ? "/" : "";
            Iterator var16 = knownCommands.entrySet().iterator();

            while(var16.hasNext()) {
                Map.Entry commandEntry = (Map.Entry)var16.next();
                Command command = (Command)commandEntry.getValue();
                if (command.testPermissionSilent(sender)) {
                    String name = (String)commandEntry.getKey();
                    if (StringUtil.startsWithIgnoreCase(name, cmdLine)) {
                        completions.add(prefix + name);
                    }
                }
            }

            completions.sort(String.CASE_INSENSITIVE_ORDER);
            return completions;
        } else {
            String commandName = cmdLine.substring(0, spaceIndex);
            Command target = this.getCommand(commandName);
            if (target == null) {
                return null;
            } else if (!target.testPermissionSilent(sender)) {
                return null;
            } else {
                prefix = cmdLine.substring(spaceIndex + 1);
                String[] args = PATTERN_ON_SPACE.split(prefix, -1);

                try {
                    return target.tabComplete(sender, commandName, args, location);
                } catch (CommandException var12) {
                    CommandException ex = var12;
                    throw ex;
                } catch (Throwable var13) {
                    Throwable ex = var13;
                    throw new CommandException("Unhandled exception executing tab-completer for '" + cmdLine + "' in " + target, ex);
                }
            }
        }
    }

    public Collection<Command> getCommands() {
        return Collections.unmodifiableCollection(this.knownCommands.values());
    }

    public void registerServerAliases() {
        Map values = this.server.getCommandAliases();
        Iterator var2 = values.keySet().iterator();

        while(true) {
            String alias;
            do {
                do {
                    if (!var2.hasNext()) {
                        return;
                    }

                    alias = (String)var2.next();
                } while(alias.contains(":"));
            } while(alias.contains(" "));

            String[] commandStrings = (String[])values.get(alias);
            List targets = new ArrayList();
            StringBuilder bad = new StringBuilder();
            String[] var7 = commandStrings;
            int var8 = commandStrings.length;

            for(int var9 = 0; var9 < var8; ++var9) {
                String commandString = var7[var9];
                String[] commandArgs = commandString.split(" ");
                Command command = this.getCommand(commandArgs[0]);
                if (command == null) {
                    if (bad.length() > 0) {
                        bad.append(", ");
                    }

                    bad.append(commandString);
                } else {
                    targets.add(commandString);
                }
            }

            if (bad.length() <= 0) {
                if (targets.size() > 0) {
                    this.knownCommands.put(alias.toLowerCase(), new FormattedCommandAlias(alias.toLowerCase(), (String[])targets.toArray(new String[0])));
                } else {
                    this.knownCommands.remove(alias.toLowerCase());
                }
            }
        }
    }
}
