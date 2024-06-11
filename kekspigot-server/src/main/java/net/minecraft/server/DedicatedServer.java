package net.minecraft.server;

import com.minexd.spigot.SpigotX;
import com.minexd.spigot.SpigotXConfig;

import com.google.common.collect.Lists;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Proxy;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.PrintStream;

import org.apache.logging.log4j.Level;
import org.bukkit.craftbukkit.LoggerOutputStream;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.craftbukkit.util.Waitable;
import org.bukkit.event.server.RemoteServerCommandEvent;

public class DedicatedServer extends MinecraftServer implements IMinecraftServer {

    private static final Logger LOGGER = LogManager.getLogger();
    private final List<ServerCommand> l = Collections.synchronizedList(Lists.<ServerCommand>newArrayList());
    private RemoteStatusListener m;
    private RemoteControlListener n;
    public PropertyManager propertyManager;
    private boolean generateStructures;
    private WorldSettings.EnumGamemode r;
    private boolean s;

    // CraftBukkit start - Signature changed
    public DedicatedServer(joptsimple.OptionSet options) {
        super(options, Proxy.NO_PROXY, DedicatedServer.a);
        // CraftBukkit end
        Thread thread = new Thread("Server Infinisleeper") {
            {
                this.setDaemon(true);
                this.start();
            }

            public void run() {
                while (true) {
                    try {
                        Thread.sleep(2147483647L);
                    } catch (InterruptedException interruptedexception) {
                        ;
                    }
                }
            }
        };
    }

    protected boolean init() throws IOException {
        Thread thread = new Thread("Server console handler") {
            public void run() {
                if (!org.bukkit.craftbukkit.Main.useConsole) {
                    return;
                }

                jline.console.ConsoleReader bufferedreader = reader;
                String s;

                try {
                    while (!isStopped() && isRunning()) {
                        if (org.bukkit.craftbukkit.Main.useJline) {
                            s = bufferedreader.readLine(">", null);
                        } else {
                            s = bufferedreader.readLine();
                        }
                        if (s != null && s.trim().length() > 0) {
                            issueCommand(s, DedicatedServer.this);
                        }
                    }
                } catch (IOException ioexception) {
                    DedicatedServer.LOGGER.error("Exception handling console input", ioexception);
                }

            }
        };

        // CraftBukkit start - TODO: handle command-line logging arguments
        java.util.logging.Logger global = java.util.logging.Logger.getLogger("");

        global.setUseParentHandlers(false);

        for (java.util.logging.Handler handler : global.getHandlers()) {
            global.removeHandler(handler);
        }

        global.addHandler(new org.bukkit.craftbukkit.util.ForwardLogHandler());

        final org.apache.logging.log4j.core.Logger logger = ((org.apache.logging.log4j.core.Logger) LogManager.getRootLogger());

        for (org.apache.logging.log4j.core.Appender appender : logger.getAppenders().values()) {
            if (appender instanceof org.apache.logging.log4j.core.appender.ConsoleAppender) {
                logger.removeAppender(appender);
            }
        }

        new Thread(new org.bukkit.craftbukkit.util.TerminalConsoleWriterThread(System.out, this.reader)).start();

        System.setOut(new PrintStream(new LoggerOutputStream(logger, Level.INFO), true));
        System.setErr(new PrintStream(new LoggerOutputStream(logger, Level.WARN), true));
        // CraftBukkit end

        thread.setDaemon(true);
        thread.start();

        DedicatedServer.LOGGER.info("Starting minecraft server version 1.8.8");

        this.propertyManager = new PropertyManager(this.options);

        if (this.T()) {
            this.setServerIp("127.0.0.1");
        } else {
            this.setOnlineMode(this.propertyManager.getBoolean("online-mode", true));
            this.setServerIp(this.propertyManager.getString("server-ip", ""));
        }

        this.setSpawnAnimals(this.propertyManager.getBoolean("spawn-animals", false));
        this.setSpawnNPCs(this.propertyManager.getBoolean("spawn-npcs", false));
        this.setPVP(this.propertyManager.getBoolean("pvp", true));
        this.setAllowFlight(this.propertyManager.getBoolean("allow-flight", false));
        this.setResourcePack(this.propertyManager.getString("resource-pack", ""), this.propertyManager.getString("resource-pack-hash", ""));
        this.setMotd(this.propertyManager.getString("motd", "A Minecraft Server"));
        this.setForceGamemode(this.propertyManager.getBoolean("force-gamemode", false));
        this.setIdleTimeout(this.propertyManager.getInt("player-idle-timeout", 0));

        if (this.propertyManager.getInt("difficulty", 1) < 0) {
            this.propertyManager.setProperty("difficulty", 0);
        } else if (this.propertyManager.getInt("difficulty", 1) > 3) {
            this.propertyManager.setProperty("difficulty", 3);
        }

        this.generateStructures = this.propertyManager.getBoolean("generate-structures", true);
        int i = this.propertyManager.getInt("gamemode", WorldSettings.EnumGamemode.SURVIVAL.getId());
        this.r = WorldSettings.a(i);

        InetAddress inetaddress = null;

        if (this.getServerIp().length() > 0) {
            inetaddress = InetAddress.getByName(this.getServerIp());
        }

        if (this.getPort() < 0) {
            this.setPort(this.propertyManager.getInt("server-port", 25565));
        }

        // Register player list (which starts CraftServer)
        this.a(new DedicatedPlayerList(this));

        // Load Spigot settings
        org.spigotmc.SpigotConfig.init((File) options.valueOf("spigot-settings"));
        org.spigotmc.SpigotConfig.registerCommands();

        // Load PaperSpigot settings
        org.github.paperspigot.PaperSpigotConfig.init((File) options.valueOf("spigot-settings"));
        org.github.paperspigot.PaperSpigotConfig.registerCommands();

        // Load SpigotX
        SpigotX.INSTANCE.setConfig(new SpigotXConfig());
        SpigotX.INSTANCE.registerCommands();

        DedicatedServer.LOGGER.info("* Allocated memory: " + (Runtime.getRuntime().maxMemory() / 1024L / 1024L));
        DedicatedServer.LOGGER.info("* Online Mode: " + this.getOnlineMode());
        DedicatedServer.LOGGER.info("* Server Address: " + this.getServerIp() + ":" + this.getPort());
        DedicatedServer.LOGGER.info("* View Distance: " + this.getPlayerList().getCserver().getViewDistance());

        this.a(MinecraftEncryption.b());

        if (!org.spigotmc.SpigotConfig.lateBind) {
            try {
                this.aq().a(inetaddress, this.getPort());
            } catch (IOException ioexception) {
                DedicatedServer.LOGGER.warn("** FAILED TO BIND PORT **");
                DedicatedServer.LOGGER.warn("The exception was: {}", ioexception.toString());
                DedicatedServer.LOGGER.warn("Perhaps a server is already running on that port?");
                return false;
            }
        }

        server.loadPlugins();
        server.enablePlugins(org.bukkit.plugin.PluginLoadOrder.STARTUP);

        if (this.aR()) {
            this.getUserCache().c();
        }

        if (!NameReferencingFileConverter.a(this.propertyManager)) {
            return false;
        } else {
            this.convertable = new WorldLoaderServer(server.getWorldContainer());
            long j = System.nanoTime();

            if (this.U() == null) {
                this.setWorld(this.propertyManager.getString("level-name", "world"));
            }

            final String levelSeed = this.propertyManager.getString("level-seed", "");
            final String levelType = this.propertyManager.getString("level-type", "DEFAULT");
            final String generatorSettings = this.propertyManager.getString("generator-settings", "");
            long actualSeed = (new Random()).nextLong();

            if (levelSeed.length() > 0) {
                try {
                    long l = Long.parseLong(levelSeed);

                    if (l != 0L) {
                        actualSeed = l;
                    }
                } catch (NumberFormatException numberformatexception) {
                    actualSeed = (long) levelSeed.hashCode();
                }
            }

            WorldType worldType = WorldType.getType(levelType);

            if (worldType == null) {
                worldType = WorldType.NORMAL;
            }

            this.aB();
            this.getEnableCommandBlock();
            this.p();
            this.getSnooperEnabled();
            this.aK();
            this.setMaxBuildHeight(this.propertyManager.getInt("max-build-height", 256));
            this.setMaxBuildHeight((this.getMaxBuildHeight() + 8) / 16 * 16);
            this.setMaxBuildHeight(MathHelper.clamp(this.getMaxBuildHeight(), 64, 256));
            this.propertyManager.setProperty("max-build-height", this.getMaxBuildHeight());
            this.a(this.U(), this.U(), actualSeed, worldType, generatorSettings);

            long i1 = System.nanoTime() - j;
            String s3 = String.format("%.3fs", (double) i1 / 1.0E9D);

            DedicatedServer.LOGGER.info("Finished loading the server in " + s3 + "...");

            if (this.propertyManager.getBoolean("enable-query", false)) {
                DedicatedServer.LOGGER.info("* Enabled GS4 Query");
                this.m = new RemoteStatusListener(this);
                this.m.a();
            }

            if (this.propertyManager.getBoolean("enable-rcon", false)) {
                DedicatedServer.LOGGER.info("* Enabled RCON");
                this.n = new RemoteControlListener(this);
                this.n.a();
                this.remoteConsole = new org.bukkit.craftbukkit.command.CraftRemoteConsoleCommandSender();
            }

            if (this.server.getBukkitSpawnRadius() > -1) {
                this.propertyManager.properties.remove("spawn-protection");
                this.propertyManager.getInt("spawn-protection", this.server.getBukkitSpawnRadius());
                this.server.removeBukkitSpawnRadius();
                this.propertyManager.savePropertiesFile();
            }

            if (org.spigotmc.SpigotConfig.lateBind) {
                try {
                    this.aq().a(inetaddress, this.getPort());
                } catch (IOException ioexception) {
                    DedicatedServer.LOGGER.warn("**** FAILED TO BIND TO PORT!");
                    DedicatedServer.LOGGER.warn("The exception was: {}", ioexception.toString());
                    DedicatedServer.LOGGER.warn("Perhaps a server is already running on that port?");
                    return false;
                }
            }

            if (false && this.aS() > 0L) {
                Thread watchdogThread = new Thread(new ThreadWatchdog(this));
                watchdogThread.setName("Server Watchdog");
                watchdogThread.setDaemon(true);
                watchdogThread.start();
            }

            return true;
        }
    }

    public PropertyManager getPropertyManager() {
        return this.propertyManager;
    }

    public void setGamemode(WorldSettings.EnumGamemode worldsettings_enumgamemode) {
        super.setGamemode(worldsettings_enumgamemode);
        this.r = worldsettings_enumgamemode;
    }

    public boolean getGenerateStructures() {
        return this.generateStructures;
    }

    public WorldSettings.EnumGamemode getGamemode() {
        return this.r;
    }

    public EnumDifficulty getDifficulty() {
        return EnumDifficulty.getById(this.propertyManager.getInt("difficulty", EnumDifficulty.NORMAL.a()));
    }

    public boolean isHardcore() {
        return this.propertyManager.getBoolean("hardcore", false);
    }

    protected void a(CrashReport crashreport) {
    }

    public CrashReport b(CrashReport crashreport) {
        crashreport = super.b(crashreport);

        crashreport.g().a("Is Modded", new Callable<String>() {
            @Override
            public String call() throws Exception {
                String s = DedicatedServer.this.getServerModName();
                return !s.equals("vanilla") ? "Definitely; Server brand changed to \'" + s + "\'" : "Unknown (can\'t tell)";
            }
        });

        crashreport.g().a("Type", new Callable<String>() {
            @Override
            public String call() throws Exception {
                return "Dedicated Server (map_server.txt)";
            }
        });

        return crashreport;
    }

    protected void z() {
        System.exit(0);
    }

    public void tickServer() { // CraftBukkit - fix decompile error
        super.tickServer();
        this.aO();
    }

    public boolean getAllowNether() {
        return this.propertyManager.getBoolean("allow-nether", true);
    }

    public boolean getSpawnMonsters() {
        return this.propertyManager.getBoolean("spawn-monsters", true);
    }

    public void a(MojangStatisticsGenerator mojangstatisticsgenerator) {
        mojangstatisticsgenerator.a("whitelist_enabled", this.aP().getHasWhitelist());
        mojangstatisticsgenerator.a("whitelist_count", this.aP().getWhitelisted().length);

        super.a(mojangstatisticsgenerator);
    }

    public boolean getSnooperEnabled() {
        return this.propertyManager.getBoolean("snooper-enabled", true);
    }

    public void issueCommand(String s, ICommandListener icommandlistener) {
        this.l.add(new ServerCommand(s, icommandlistener));
    }

    public void aO() {
        while (!this.l.isEmpty()) {
            ServerCommand servercommand = this.l.remove(0);

            ServerCommandEvent event = new ServerCommandEvent(console, servercommand.command);

            server.getPluginManager().callEvent(event);

            if (event.isCancelled()) {
                continue;
            }

            servercommand = new ServerCommand(event.getCommand(), servercommand.source);

            server.dispatchServerCommand(console, servercommand);
        }
    }

    public boolean ae() {
        return true;
    }

    public boolean ai() {
        return this.propertyManager.getBoolean("use-native-transport", true);
    }

    public DedicatedPlayerList aP() {
        return (DedicatedPlayerList) super.getPlayerList();
    }

    public int a(String s, int i) {
        return this.propertyManager.getInt(s, i);
    }

    public String a(String s, String s1) {
        return this.propertyManager.getString(s, s1);
    }

    public boolean a(String s, boolean flag) {
        return this.propertyManager.getBoolean(s, flag);
    }

    public void a(String s, Object object) {
        this.propertyManager.setProperty(s, object);
    }

    public void a() {
        this.propertyManager.savePropertiesFile();
    }

    public String b() {
        File file = this.propertyManager.c();

        return file != null ? file.getAbsolutePath() : "No settings file";
    }

    public void aQ() {
        ServerGUI.a(this);
        this.s = true;
    }

    public boolean as() {
        return this.s;
    }

    public String a(WorldSettings.EnumGamemode worldsettings_enumgamemode, boolean flag) {
        return "";
    }

    public boolean getEnableCommandBlock() {
        return this.propertyManager.getBoolean("enable-command-block", false);
    }

    public int getSpawnProtection() {
        return this.propertyManager.getInt("spawn-protection", super.getSpawnProtection());
    }

    public boolean a(World world, BlockPosition blockposition, EntityHuman entityhuman) {
        if (world.worldProvider.getDimension() != 0) {
            return false;
        } else if (this.aP().getOPs().isEmpty()) {
            return false;
        } else if (this.aP().isOp(entityhuman.getProfile())) {
            return false;
        } else if (this.getSpawnProtection() <= 0) {
            return false;
        } else {
            BlockPosition blockposition1 = world.getSpawn();
            int i = MathHelper.a(blockposition.getX() - blockposition1.getX());
            int j = MathHelper.a(blockposition.getZ() - blockposition1.getZ());
            int k = Math.max(i, j);

            return k <= this.getSpawnProtection();
        }
    }

    public int p() {
        return this.propertyManager.getInt("op-permission-level", 4);
    }

    public void setIdleTimeout(int i) {
        super.setIdleTimeout(i);
        this.propertyManager.setProperty("player-idle-timeout", i);
        this.a();
    }

    public boolean q() {
        return this.propertyManager.getBoolean("broadcast-rcon-to-ops", true);
    }

    public boolean r() {
        return this.propertyManager.getBoolean("broadcast-console-to-ops", true);
    }

    public boolean aB() {
        return this.propertyManager.getBoolean("announce-player-achievements", false);
    }

    public int aI() {
        int i = this.propertyManager.getInt("max-world-size", super.aI());

        if (i < 1) {
            i = 1;
        } else if (i > super.aI()) {
            i = super.aI();
        }

        return i;
    }

    public int aK() {
        return this.propertyManager.getInt("network-compression-threshold", super.aK());
    }

    protected boolean aR() {
        boolean flag = false;

        int i;

        for (i = 0; !flag && i <= 2; ++i) {
            if (i > 0) {
                DedicatedServer.LOGGER.warn("Encountered a problem while converting the user banlist, retrying in a few seconds");
                this.aU();
            }

            flag = NameReferencingFileConverter.a(this);
        }

        boolean flag1 = false;

        for (i = 0; !flag1 && i <= 2; ++i) {
            if (i > 0) {
                DedicatedServer.LOGGER.warn("Encountered a problem while converting the ip banlist, retrying in a few seconds");
                this.aU();
            }

            flag1 = NameReferencingFileConverter.b(this);
        }

        boolean flag2 = false;

        for (i = 0; !flag2 && i <= 2; ++i) {
            if (i > 0) {
                DedicatedServer.LOGGER.warn("Encountered a problem while converting the op list, retrying in a few seconds");
                this.aU();
            }

            flag2 = NameReferencingFileConverter.c(this);
        }

        boolean flag3 = false;

        for (i = 0; !flag3 && i <= 2; ++i) {
            if (i > 0) {
                DedicatedServer.LOGGER.warn("Encountered a problem while converting the whitelist, retrying in a few seconds");
                this.aU();
            }

            flag3 = NameReferencingFileConverter.d(this);
        }

        boolean flag4 = false;

        for (i = 0; !flag4 && i <= 2; ++i) {
            if (i > 0) {
                DedicatedServer.LOGGER.warn("Encountered a problem while converting the player save files, retrying in a few seconds");
                this.aU();
            }

            flag4 = NameReferencingFileConverter.a(this, this.propertyManager);
        }

        return flag || flag1 || flag2 || flag3 || flag4;
    }

    private void aU() {
        try {
            Thread.sleep(5000L);
        } catch (InterruptedException interruptedexception) {
            ;
        }
    }

    public long aS() {
        return this.propertyManager.getLong("max-tick-time", TimeUnit.MINUTES.toMillis(1L));
    }

    public String getPlugins() {
        // CraftBukkit start - Whole method
        StringBuilder result = new StringBuilder();
        org.bukkit.plugin.Plugin[] plugins = server.getPluginManager().getPlugins();

        result.append(server.getName());
        result.append(" on Bukkit ");
        result.append(server.getBukkitVersion());

        if (plugins.length > 0 && server.getQueryPlugins()) {
            result.append(": ");

            for (int i = 0; i < plugins.length; i++) {
                if (i > 0) {
                    result.append("; ");
                }

                result.append(plugins[i].getDescription().getName());
                result.append(" ");
                result.append(plugins[i].getDescription().getVersion().replaceAll(";", ","));
            }
        }

        return result.toString();
        // CraftBukkit end
    }

    // CraftBukkit start - fire RemoteServerCommandEvent
    public String executeRemoteCommand(final String s) {
        Waitable<String> waitable = new Waitable<String>() {
            @Override
            protected String evaluate() {
                RemoteControlCommandListener.getInstance().i();
                // Event changes start
                RemoteServerCommandEvent event = new RemoteServerCommandEvent(remoteConsole, s);
                server.getPluginManager().callEvent(event);
                if (event.isCancelled()) {
                    return "";
                }
                // Event change end
                ServerCommand serverCommand = new ServerCommand(event.getCommand(), RemoteControlCommandListener.getInstance());
                server.dispatchServerCommand(remoteConsole, serverCommand);
                return RemoteControlCommandListener.getInstance().j();
            }
        };

        processQueue.add(waitable);

        try {
            return waitable.get();
        } catch (java.util.concurrent.ExecutionException e) {
            throw new RuntimeException("Exception processing rcon command " + s, e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Maintain interrupted state
            throw new RuntimeException("Interrupted processing rcon command " + s, e);
        }
        // CraftBukkit end
    }

    public PlayerList getPlayerList() {
        return this.aP();
    }
}
