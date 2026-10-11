package org.bukkit.plugin;

import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang.Validate;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.PluginCommandYamlParser;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.util.FileUtil;
import org.eytril.spigot.asm.ASMEventBus;
import org.eytril.spigot.asm.EventDispatchException;
import org.eytril.spigot.asm.ListenerHandlerPair;
import org.github.paperspigot.event.ServerExceptionEvent;
import org.github.paperspigot.exception.ServerEventException;
import org.github.paperspigot.exception.ServerPluginEnableDisableException;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles all plugin management from the Server.
 */
public class SimplePluginManager implements PluginManager {

    private final Server server;
    private final Map<Pattern, PluginLoader> fileAssociations =
            new HashMap<Pattern, PluginLoader>();

    private final List<Plugin> plugins = new ArrayList<Plugin>();
    private final Map<String, Plugin> lookupNames =
            new HashMap<String, Plugin>();

    private static File updateDirectory = null;

    private final SimpleCommandMap commandMap;

    private final Map<String, Permission> permissions =
            new HashMap<String, Permission>();

    private final Map<Boolean, Set<Permission>> defaultPerms =
            new LinkedHashMap<Boolean, Set<Permission>>();

    private final Map<String, Map<Permissible, Boolean>> permSubs =
            new HashMap<String, Map<Permissible, Boolean>>();

    private final Map<Boolean, Map<Permissible, Boolean>> defSubs =
            new HashMap<Boolean, Map<Permissible, Boolean>>();

    private boolean useTimings = false;

    // ASM EVENT BUS BEGIN
    private final ASMEventBus eventBus = new ASMEventBus();
    // ASM EVENT BUS END

    public SimplePluginManager(Server instance, SimpleCommandMap commandMap) {
        server = instance;
        this.commandMap = commandMap;

        defaultPerms.put(true, new HashSet<Permission>());
        defaultPerms.put(false, new HashSet<Permission>());
    }

    public void registerInterface(Class<? extends PluginLoader> loader)
            throws IllegalArgumentException {
        PluginLoader instance;

        if (PluginLoader.class.isAssignableFrom(loader)) {
            Constructor<? extends PluginLoader> constructor;

            try {
                constructor = loader.getConstructor(Server.class);
                instance = constructor.newInstance(server);
            } catch (NoSuchMethodException ex) {
                String className = loader.getName();

                throw new IllegalArgumentException(
                        String.format(
                                "Class %s does not have a public %s(Server) constructor",
                                className,
                                className
                        ),
                        ex
                );
            } catch (Exception ex) {
                throw new IllegalArgumentException(
                        String.format(
                                "Unexpected exception %s while attempting to construct a new instance of %s",
                                ex.getClass().getName(),
                                loader.getName()
                        ),
                        ex
                );
            }
        } else {
            throw new IllegalArgumentException(
                    String.format(
                            "Class %s does not implement interface PluginLoader",
                            loader.getName()
                    )
            );
        }

        Pattern[] patterns = instance.getPluginFileFilters();

        synchronized (this) {
            for (Pattern pattern : patterns) {
                fileAssociations.put(pattern, instance);
            }
        }
    }

    public Plugin[] loadPlugins(File directory) {
        Validate.notNull(directory, "Directory cannot be null");
        Validate.isTrue(directory.isDirectory(), "Directory must be a directory");

        List<Plugin> result = new ArrayList<Plugin>();
        Set<Pattern> filters = fileAssociations.keySet();

        if (!server.getUpdateFolder().equals("")) {
            updateDirectory = new File(directory, server.getUpdateFolder());
        }

        Map<String, File> plugins = new HashMap<String, File>();
        Set<String> loadedPlugins = new HashSet<String>();
        Map<String, Collection<String>> dependencies =
                new HashMap<String, Collection<String>>();
        Map<String, Collection<String>> softDependencies =
                new HashMap<String, Collection<String>>();

        for (File file : directory.listFiles()) {
            PluginLoader loader = null;

            for (Pattern filter : filters) {
                Matcher match = filter.matcher(file.getName());

                if (match.find()) {
                    loader = fileAssociations.get(filter);
                }
            }

            if (loader == null) {
                continue;
            }

            PluginDescriptionFile description = null;

            try {
                description = loader.getPluginDescription(file);
                String name = description.getName();

                if (name.equalsIgnoreCase("bukkit")
                        || name.equalsIgnoreCase("minecraft")
                        || name.equalsIgnoreCase("mojang")) {
                    server.getLogger().log(
                            Level.SEVERE,
                            "Could not load '"
                                    + file.getPath()
                                    + "' in folder '"
                                    + directory.getPath()
                                    + "': Restricted Name"
                    );
                    continue;
                } else if (description.rawName.indexOf(' ') != -1) {
                    server.getLogger().warning(
                            String.format(
                                    "Plugin `%s' uses the space-character (0x20) in its name `%s' - this is discouraged",
                                    description.getFullName(),
                                    description.rawName
                            )
                    );
                }
            } catch (InvalidDescriptionException ex) {
                server.getLogger().log(
                        Level.SEVERE,
                        "Could not load '"
                                + file.getPath()
                                + "' in folder '"
                                + directory.getPath()
                                + "'",
                        ex
                );
                continue;
            }

            File replacedFile = plugins.put(description.getName(), file);

            if (replacedFile != null) {
                server.getLogger().severe(
                        String.format(
                                "Ambiguous plugin name `%s' for files `%s' and `%s' in `%s'",
                                description.getName(),
                                file.getPath(),
                                replacedFile.getPath(),
                                directory.getPath()
                        )
                );
            }

            Collection<String> softDependencySet = description.getSoftDepend();

            if (softDependencySet != null && !softDependencySet.isEmpty()) {
                if (softDependencies.containsKey(description.getName())) {
                    softDependencies.get(description.getName())
                            .addAll(softDependencySet);
                } else {
                    softDependencies.put(
                            description.getName(),
                            new LinkedList<String>(softDependencySet)
                    );
                }
            }

            Collection<String> dependencySet = description.getDepend();

            if (dependencySet != null && !dependencySet.isEmpty()) {
                dependencies.put(
                        description.getName(),
                        new LinkedList<String>(dependencySet)
                );
            }

            Collection<String> loadBeforeSet = description.getLoadBefore();

            if (loadBeforeSet != null && !loadBeforeSet.isEmpty()) {
                for (String loadBeforeTarget : loadBeforeSet) {
                    if (softDependencies.containsKey(loadBeforeTarget)) {
                        softDependencies.get(loadBeforeTarget)
                                .add(description.getName());
                    } else {
                        Collection<String> shortSoftDependency =
                                new LinkedList<String>();

                        shortSoftDependency.add(description.getName());
                        softDependencies.put(
                                loadBeforeTarget,
                                shortSoftDependency
                        );
                    }
                }
            }
        }

        while (!plugins.isEmpty()) {
            boolean missingDependency = true;
            Iterator<String> pluginIterator = plugins.keySet().iterator();

            while (pluginIterator.hasNext()) {
                String plugin = pluginIterator.next();

                if (dependencies.containsKey(plugin)) {
                    Iterator<String> dependencyIterator =
                            dependencies.get(plugin).iterator();

                    while (dependencyIterator.hasNext()) {
                        String dependency = dependencyIterator.next();

                        if (loadedPlugins.contains(dependency)) {
                            dependencyIterator.remove();
                        } else if (!plugins.containsKey(dependency)) {
                            missingDependency = false;
                            File file = plugins.get(plugin);

                            pluginIterator.remove();
                            softDependencies.remove(plugin);
                            dependencies.remove(plugin);

                            server.getLogger().log(
                                    Level.SEVERE,
                                    "Could not load '"
                                            + file.getPath()
                                            + "' in folder '"
                                            + directory.getPath()
                                            + "'",
                                    new UnknownDependencyException(dependency)
                            );
                            break;
                        }
                    }

                    if (dependencies.containsKey(plugin)
                            && dependencies.get(plugin).isEmpty()) {
                        dependencies.remove(plugin);
                    }
                }

                if (softDependencies.containsKey(plugin)) {
                    Iterator<String> softDependencyIterator =
                            softDependencies.get(plugin).iterator();

                    while (softDependencyIterator.hasNext()) {
                        String softDependency = softDependencyIterator.next();

                        if (!plugins.containsKey(softDependency)) {
                            softDependencyIterator.remove();
                        }
                    }

                    if (softDependencies.get(plugin).isEmpty()) {
                        softDependencies.remove(plugin);
                    }
                }

                if (!dependencies.containsKey(plugin)
                        && !softDependencies.containsKey(plugin)
                        && plugins.containsKey(plugin)) {
                    File file = plugins.get(plugin);
                    pluginIterator.remove();
                    missingDependency = false;

                    try {
                        result.add(loadPlugin(file));
                        loadedPlugins.add(plugin);
                        continue;
                    } catch (InvalidPluginException ex) {
                        server.getLogger().log(
                                Level.SEVERE,
                                "Could not load '"
                                        + file.getPath()
                                        + "' in folder '"
                                        + directory.getPath()
                                        + "'",
                                ex
                        );
                    }
                }
            }

            if (missingDependency) {
                pluginIterator = plugins.keySet().iterator();

                while (pluginIterator.hasNext()) {
                    String plugin = pluginIterator.next();

                    if (!dependencies.containsKey(plugin)) {
                        softDependencies.remove(plugin);
                        missingDependency = false;

                        File file = plugins.get(plugin);
                        pluginIterator.remove();

                        try {
                            result.add(loadPlugin(file));
                            loadedPlugins.add(plugin);
                            break;
                        } catch (InvalidPluginException ex) {
                            server.getLogger().log(
                                    Level.SEVERE,
                                    "Could not load '"
                                            + file.getPath()
                                            + "' in folder '"
                                            + directory.getPath()
                                            + "'",
                                    ex
                            );
                        }
                    }
                }

                if (missingDependency) {
                    softDependencies.clear();
                    dependencies.clear();

                    Iterator<File> failedPluginIterator =
                            plugins.values().iterator();

                    while (failedPluginIterator.hasNext()) {
                        File file = failedPluginIterator.next();
                        failedPluginIterator.remove();

                        server.getLogger().log(
                                Level.SEVERE,
                                "Could not load '"
                                        + file.getPath()
                                        + "' in folder '"
                                        + directory.getPath()
                                        + "': circular dependency detected"
                        );
                    }
                }
            }
        }

        return result.toArray(new Plugin[result.size()]);
    }

    public synchronized Plugin loadPlugin(File file)
            throws InvalidPluginException, UnknownDependencyException {
        Validate.notNull(file, "File cannot be null");

        checkUpdate(file);

        Set<Pattern> filters = fileAssociations.keySet();
        Plugin result = null;

        for (Pattern filter : filters) {
            String name = file.getName();
            Matcher match = filter.matcher(name);

            if (match.find()) {
                PluginLoader loader = fileAssociations.get(filter);
                result = loader.loadPlugin(file);
            }
        }

        if (result != null) {
            plugins.add(result);
            lookupNames.put(
                    result.getDescription().getName().toLowerCase(),
                    result
            );
        }

        return result;
    }

    private void checkUpdate(File file) {
        if (updateDirectory == null || !updateDirectory.isDirectory()) {
            return;
        }

        File updateFile = new File(updateDirectory, file.getName());

        if (updateFile.isFile() && FileUtil.copy(updateFile, file)) {
            updateFile.delete();
        }
    }

    public synchronized Plugin getPlugin(String name) {
        return lookupNames.get(
                name.replace(' ', '_').toLowerCase()
        );
    }

    public synchronized Plugin[] getPlugins() {
        return plugins.toArray(new Plugin[0]);
    }

    public boolean isPluginEnabled(String name) {
        Plugin plugin = getPlugin(name);
        return isPluginEnabled(plugin);
    }

    public boolean isPluginEnabled(Plugin plugin) {
        if (plugin != null && plugins.contains(plugin)) {
            return plugin.isEnabled();
        }

        return false;
    }

    public void enablePlugin(final Plugin plugin) {
        if (!plugin.isEnabled()) {
            List<Command> pluginCommands =
                    PluginCommandYamlParser.parse(plugin);

            if (!pluginCommands.isEmpty()) {
                commandMap.registerAll(
                        plugin.getDescription().getName(),
                        pluginCommands
                );
            }

            try {
                plugin.getPluginLoader().enablePlugin(plugin);
            } catch (Throwable ex) {
                handlePluginException(
                        "Error occurred (in the plugin loader) while enabling "
                                + plugin.getDescription().getFullName()
                                + " (Is it up to date?)",
                        ex,
                        plugin
                );
            }

            HandlerList.bakeAll();
        }
    }

    public void disablePlugins() {
        Plugin[] plugins = getPlugins();

        for (int i = plugins.length - 1; i >= 0; i--) {
            disablePlugin(plugins[i]);
        }
    }

    public void disablePlugin(final Plugin plugin) {
        if (plugin.isEnabled()) {
            try {
                plugin.getPluginLoader().disablePlugin(plugin);
            } catch (Throwable ex) {
                handlePluginException(
                        "Error occurred (in the plugin loader) while disabling "
                                + plugin.getDescription().getFullName()
                                + " (Is it up to date?)",
                        ex,
                        plugin
                );
            }

            try {
                server.getScheduler().cancelTasks(plugin);
            } catch (Throwable ex) {
                handlePluginException(
                        "Error occurred (in the plugin loader) while cancelling tasks for "
                                + plugin.getDescription().getFullName()
                                + " (Is it up to date?)",
                        ex,
                        plugin
                );
            }

            try {
                server.getServicesManager().unregisterAll(plugin);
            } catch (Throwable ex) {
                handlePluginException(
                        "Error occurred (in the plugin loader) while unregistering services for "
                                + plugin.getDescription().getFullName()
                                + " (Is it up to date?)",
                        ex,
                        plugin
                );
            }

            try {
                HandlerList.unregisterAll(plugin);
            } catch (Throwable ex) {
                handlePluginException(
                        "Error occurred while unregistering Bukkit events for "
                                + plugin.getDescription().getFullName()
                                + " (Is it up to date?)",
                        ex,
                        plugin
                );
            }

            // ASM EVENT BUS BEGIN
            try {
                eventBus.unregisterPlugin(plugin);
            } catch (Throwable ex) {
                handlePluginException(
                        "Error occurred while unregistering ASM events for "
                                + plugin.getDescription().getFullName()
                                + " (Is it up to date?)",
                        ex,
                        plugin
                );
            }
            // ASM EVENT BUS END

            try {
                server.getMessenger()
                        .unregisterIncomingPluginChannel(plugin);

                server.getMessenger()
                        .unregisterOutgoingPluginChannel(plugin);
            } catch (Throwable ex) {
                handlePluginException(
                        "Error occurred while unregistering plugin channels for "
                                + plugin.getDescription().getFullName()
                                + " (Is it up to date?)",
                        ex,
                        plugin
                );
            }
        }
    }

    private void handlePluginException(
            String message,
            Throwable exception,
            Plugin plugin
    ) {
        server.getLogger().log(Level.SEVERE, message, exception);

        callEvent(
                new ServerExceptionEvent(
                        new ServerPluginEnableDisableException(
                                message,
                                exception,
                                plugin
                        )
                )
        );
    }

    public void clearPlugins() {
        synchronized (this) {
            disablePlugins();

            // ASM EVENT BUS BEGIN
            eventBus.clear();
            // ASM EVENT BUS END

            plugins.clear();
            lookupNames.clear();
            HandlerList.unregisterAll();
            fileAssociations.clear();
            permissions.clear();
            defaultPerms.get(true).clear();
            defaultPerms.get(false).clear();
        }
    }

    @Override
    public void callEvent(Event event) {
        if (event.isAsynchronous()) {
            if (Thread.holdsLock(this)) {
                throw new IllegalStateException(
                        event.getEventName()
                                + " cannot be triggered asynchronously from inside synchronized code."
                );
            }

            if (server.isPrimaryThread()) {
                throw new IllegalStateException(
                        event.getEventName()
                                + " cannot be triggered asynchronously from primary server thread."
                );
            }

            dispatchASMEvent(event);
        } else {
            synchronized (this) {
                dispatchASMEvent(event);
            }
        }
    }

    private void dispatchASMEvent(Event event) {
        try {
            eventBus.dispatchEvent(event);
        } catch (EventDispatchException dispatchException) {
            ListenerHandlerPair registration =
                    dispatchException.getRegistration();

            Plugin plugin = registration.getPlugin();
            Throwable cause = dispatchException.getCause();

            String message =
                    "Could not pass event "
                            + event.getEventName()
                            + " to "
                            + plugin.getDescription().getFullName();

            server.getLogger().log(Level.SEVERE, message, cause);

            if (!(event instanceof ServerExceptionEvent)) {
                callEvent(
                        new ServerExceptionEvent(
                                new ServerEventException(
                                        message,
                                        cause,
                                        plugin,
                                        registration.getListener(),
                                        event
                                )
                        )
                );
            }
        }
    }

    @Override
    public void registerEvents(Listener listener, Plugin plugin) {
        if (listener == null) {
            throw new IllegalArgumentException("Listener cannot be null");
        }

        if (plugin == null) {
            throw new IllegalArgumentException("Plugin cannot be null");
        }

        if (!plugin.isEnabled()) {
            throw new IllegalPluginAccessException(
                    "Plugin attempted to register "
                            + listener
                            + " while not enabled"
            );
        }

        // ASM EVENT BUS BEGIN
        eventBus.registerListener(listener, plugin);
        // ASM EVENT BUS END
    }

    @Override
    public void registerEvent(
            Class<? extends Event> event,
            Listener listener,
            EventPriority priority,
            EventExecutor executor,
            Plugin plugin
    ) {
        registerEvent(
                event,
                listener,
                priority,
                executor,
                plugin,
                false
        );
    }

    @Override
    public void registerEvent(
            Class<? extends Event> event,
            Listener listener,
            EventPriority priority,
            EventExecutor executor,
            Plugin plugin,
            boolean ignoreCancelled
    ) {
        Validate.notNull(event, "Event cannot be null");
        Validate.notNull(listener, "Listener cannot be null");
        Validate.notNull(priority, "Priority cannot be null");
        Validate.notNull(executor, "Executor cannot be null");
        Validate.notNull(plugin, "Plugin cannot be null");

        if (!plugin.isEnabled()) {
            throw new IllegalPluginAccessException(
                    "Plugin attempted to register "
                            + event
                            + " while not enabled"
            );
        }

        EventExecutor timedExecutor =
                new co.aikar.timings.TimedEventExecutor(
                        executor,
                        plugin,
                        null,
                        event
                );

        // ASM EVENT BUS BEGIN
        eventBus.registerExecutor(
                event,
                listener,
                priority,
                timedExecutor,
                plugin,
                ignoreCancelled
        );
        // ASM EVENT BUS END
    }

    public Permission getPermission(String name) {
        return permissions.get(name.toLowerCase());
    }

    public void addPermission(Permission perm) {
        String name = perm.getName().toLowerCase();

        if (permissions.containsKey(name)) {
            throw new IllegalArgumentException(
                    "The permission " + name + " is already defined!"
            );
        }

        permissions.put(name, perm);
        calculatePermissionDefault(perm);
    }

    public Set<Permission> getDefaultPermissions(boolean op) {
        return ImmutableSet.copyOf(defaultPerms.get(op));
    }

    public void removePermission(Permission perm) {
        removePermission(perm.getName());
    }

    public void removePermission(String name) {
        permissions.remove(name.toLowerCase());
    }

    public void recalculatePermissionDefaults(Permission perm) {
        if (perm != null
                && permissions.containsKey(perm.getName().toLowerCase())) {
            defaultPerms.get(true).remove(perm);
            defaultPerms.get(false).remove(perm);

            calculatePermissionDefault(perm);
        }
    }

    private void calculatePermissionDefault(Permission perm) {
        if (perm.getDefault() == PermissionDefault.OP
                || perm.getDefault() == PermissionDefault.TRUE) {
            defaultPerms.get(true).add(perm);
            dirtyPermissibles(true);
        }

        if (perm.getDefault() == PermissionDefault.NOT_OP
                || perm.getDefault() == PermissionDefault.TRUE) {
            defaultPerms.get(false).add(perm);
            dirtyPermissibles(false);
        }
    }

    private void dirtyPermissibles(boolean op) {
        Set<Permissible> permissibles =
                getDefaultPermSubscriptions(op);

        for (Permissible permissible : permissibles) {
            permissible.recalculatePermissions();
        }
    }

    public void subscribeToPermission(
            String permission,
            Permissible permissible
    ) {
        String name = permission.toLowerCase();
        Map<Permissible, Boolean> map = permSubs.get(name);

        if (map == null) {
            map = new WeakHashMap<Permissible, Boolean>();
            permSubs.put(name, map);
        }

        map.put(permissible, true);
    }

    public void unsubscribeFromPermission(
            String permission,
            Permissible permissible
    ) {
        String name = permission.toLowerCase();
        Map<Permissible, Boolean> map = permSubs.get(name);

        if (map != null) {
            map.remove(permissible);

            if (map.isEmpty()) {
                permSubs.remove(name);
            }
        }
    }

    public Set<Permissible> getPermissionSubscriptions(String permission) {
        String name = permission.toLowerCase();
        Map<Permissible, Boolean> map = permSubs.get(name);

        if (map == null) {
            return ImmutableSet.of();
        }

        return ImmutableSet.copyOf(map.keySet());
    }

    public void subscribeToDefaultPerms(
            boolean op,
            Permissible permissible
    ) {
        Map<Permissible, Boolean> map = defSubs.get(op);

        if (map == null) {
            map = new WeakHashMap<Permissible, Boolean>();
            defSubs.put(op, map);
        }

        map.put(permissible, true);
    }

    public void unsubscribeFromDefaultPerms(
            boolean op,
            Permissible permissible
    ) {
        Map<Permissible, Boolean> map = defSubs.get(op);

        if (map != null) {
            map.remove(permissible);

            if (map.isEmpty()) {
                defSubs.remove(op);
            }
        }
    }

    public Set<Permissible> getDefaultPermSubscriptions(boolean op) {
        Map<Permissible, Boolean> map = defSubs.get(op);

        if (map == null) {
            return ImmutableSet.of();
        }

        return ImmutableSet.copyOf(map.keySet());
    }

    public Set<Permission> getPermissions() {
        return new HashSet<Permission>(permissions.values());
    }

    public boolean useTimings() {
        return co.aikar.timings.Timings.isTimingsEnabled();
    }

    public void useTimings(boolean use) {
        co.aikar.timings.Timings.setTimingsEnabled(use);
    }
}