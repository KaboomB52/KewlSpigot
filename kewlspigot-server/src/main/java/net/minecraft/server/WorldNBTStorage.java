package net.minecraft.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.eytril.spigot.KewlSpigot;
import org.github.paperspigot.exception.ServerInternalException;

import java.io.*;
import java.util.UUID;
// CraftBukkit end

public class WorldNBTStorage implements IDataManager, IPlayerFileData {

    private static final Logger a = LogManager.getLogger();
    private final File baseDir;
    private final File playerDir;
    private final File dataDir;
    private final long sessionId = MinecraftServer.az();
    private final String f;
    private UUID uuid = null; // CraftBukkit

    public WorldNBTStorage(File file, String s, boolean flag) {
        this.baseDir = new File(file, s);
        this.baseDir.mkdirs();
        this.playerDir = new File(this.baseDir, "playerdata");
        this.dataDir = new File(this.baseDir, "data");
        this.dataDir.mkdirs();
        this.f = s;
        if (flag) {
            this.playerDir.mkdirs();
        }

        this.h();

        // manually check lock on startup
        try {
            checkSession0();
        } catch (Throwable t) {
            org.spigotmc.SneakyThrow.sneaky(t);
        }
    }

    private void h() {
        try {
            File file = new File(this.baseDir, "session.lock");
            DataOutputStream dataoutputstream = new DataOutputStream(new FileOutputStream(file));

            try {
                dataoutputstream.writeLong(this.sessionId);
            } finally {
                dataoutputstream.close();
            }

        } catch (IOException ioexception) {
            ioexception.printStackTrace();
            throw new RuntimeException("Failed to check session lock for world located at " + this.baseDir + ", aborting. Stop the server and delete the session.lock in this world to prevent further issues."); // Spigot
        }
    }

    public File getDirectory() {
        return this.baseDir;
    }

    public void checkSession() throws ExceptionWorldConflict {} // CraftBukkit - throws ExceptionWorldConflict

    private void checkSession0() throws ExceptionWorldConflict { // we can safely do so as the server will stop upon detecting a session conflict on startup
        try {
            File file = new File(this.baseDir, "session.lock");
            DataInputStream datainputstream = new DataInputStream(new FileInputStream(file));

            try {
                if (datainputstream.readLong() != this.sessionId) {
                    throw new ExceptionWorldConflict("The save for world located at " + this.baseDir + " is being accessed from another location, aborting");  // Spigot
                }
            } finally {
                datainputstream.close();
            }

        } catch (IOException ioexception) {
            throw new ExceptionWorldConflict("Failed to check session lock for world located at " + this.baseDir + ", aborting. Stop the server and delete the session.lock in this world to prevent further issues."); // Spigot
        }
    }

    public IChunkLoader createChunkLoader(WorldProvider worldprovider) {
        throw new RuntimeException("Old Chunk Storage is no longer supported.");
    }

    public WorldData getWorldData() {
        File file = new File(this.baseDir, "level.dat");
        NBTTagCompound nbttagcompound;
        NBTTagCompound nbttagcompound1;

        if (file.exists()) {
            try {
                nbttagcompound = NBTCompressedStreamTools.a((InputStream) (new FileInputStream(file)));
                nbttagcompound1 = nbttagcompound.getCompound("Data");
                return new WorldData(nbttagcompound1);
            } catch (Exception exception) {
                exception.printStackTrace();
                ServerInternalException.reportInternalException(exception); // Paper
            }
        }

        file = new File(this.baseDir, "level.dat_old");
        if (file.exists()) {
            try {
                nbttagcompound = NBTCompressedStreamTools.a((InputStream) (new FileInputStream(file)));
                nbttagcompound1 = nbttagcompound.getCompound("Data");
                return new WorldData(nbttagcompound1);
            } catch (Exception exception1) {
                exception1.printStackTrace();
                ServerInternalException.reportInternalException(exception1); // Paper
            }
        }

        return null;
    }

    public void saveWorldData(WorldData worlddata, NBTTagCompound nbttagcompound) {
        NBTTagCompound nbttagcompound1 = worlddata.a(nbttagcompound);
        NBTTagCompound nbttagcompound2 = new NBTTagCompound();

        nbttagcompound2.set("Data", nbttagcompound1);

        try {
            File file = new File(this.baseDir, "level.dat_new");
            File file1 = new File(this.baseDir, "level.dat_old");
            File file2 = new File(this.baseDir, "level.dat");

            NBTCompressedStreamTools.a(nbttagcompound2, (OutputStream) (new FileOutputStream(file)));
            if (file1.exists()) {
                file1.delete();
            }

            file2.renameTo(file1);
            if (file2.exists()) {
                file2.delete();
            }

            file.renameTo(file2);
            if (file.exists()) {
                file.delete();
            }
        } catch (Exception exception) {
            exception.printStackTrace();
            ServerInternalException.reportInternalException(exception); // Paper
        }

    }

    public void saveWorldData(WorldData worlddata) {
        NBTTagCompound nbttagcompound = worlddata.a();
        NBTTagCompound nbttagcompound1 = new NBTTagCompound();

        nbttagcompound1.set("Data", nbttagcompound);

        try {
            File file = new File(this.baseDir, "level.dat_new");
            File file1 = new File(this.baseDir, "level.dat_old");
            File file2 = new File(this.baseDir, "level.dat");

            NBTCompressedStreamTools.a(nbttagcompound1, (OutputStream) (new FileOutputStream(file)));
            if (file1.exists()) {
                file1.delete();
            }

            file2.renameTo(file1);
            if (file2.exists()) {
                file2.delete();
            }

            file.renameTo(file2);
            if (file.exists()) {
                file.delete();
            }
        } catch (Exception exception) {
            exception.printStackTrace();
            ServerInternalException.reportInternalException(exception); // Paper
        }

    }

    public void save(EntityHuman entityhuman) {
        KewlSpigot.asyncExecutor.submit(() -> {
            try {
                NBTTagCompound nbttagcompound = new NBTTagCompound();

                entityhuman.e(nbttagcompound);
                File file = new File(this.playerDir, entityhuman.getUniqueID().toString() + ".dat.tmp");
                File file1 = new File(this.playerDir, entityhuman.getUniqueID().toString() + ".dat");

                NBTCompressedStreamTools.a(nbttagcompound, (OutputStream) (new FileOutputStream(file)));
                if (file1.exists()) {
                    file1.delete();
                }

                file.renameTo(file1);
            } catch (Exception exception) {
                WorldNBTStorage.a.warn("Failed to save player data for " + entityhuman.getName());
            }
        });
    }

    public NBTTagCompound load(EntityHuman entityhuman) {
        NBTTagCompound nbttagcompound = null;

        try {
            File file = new File(this.playerDir, entityhuman.getUniqueID().toString() + ".dat");
            // Spigot Start
            boolean usingWrongFile = false;
            if ( org.bukkit.Bukkit.getOnlineMode() && !file.exists() ) // PaperSpigot - Check online mode first
            {
                file = new File( this.playerDir, UUID.nameUUIDFromBytes( ( "OfflinePlayer:" + entityhuman.getName() ).getBytes( "UTF-8" ) ).toString() + ".dat");
                if ( file.exists() )
                {
                    usingWrongFile = true;
                    org.bukkit.Bukkit.getServer().getLogger().warning( "Using offline mode UUID file for player " + entityhuman.getName() + " as it is the only copy we can find." );
                }
            }
            // Spigot End

            if (file.exists() && file.isFile()) {
                nbttagcompound = NBTCompressedStreamTools.a((InputStream) (new FileInputStream(file)));
            }
            // Spigot Start
            if ( usingWrongFile )
            {
                file.renameTo( new File( file.getPath() + ".offline-read" ) );
            }
            // Spigot End
        } catch (Exception exception) {
            WorldNBTStorage.a.warn("Failed to load player data for " + entityhuman.getName());
        }

        if (nbttagcompound != null) {
            // CraftBukkit start
            if (entityhuman instanceof EntityPlayer) {
                CraftPlayer player = (CraftPlayer) entityhuman.getBukkitEntity();
                // Only update first played if it is older than the one we have
                long modified = new File(this.playerDir, entityhuman.getUniqueID().toString() + ".dat").lastModified();
                if (modified < player.getFirstPlayed()) {
                    player.setFirstPlayed(modified);
                }
            }
            // CraftBukkit end

            entityhuman.f(nbttagcompound);
        }

        return nbttagcompound;
    }

    // CraftBukkit start
    public NBTTagCompound getPlayerData(String s) {
        try {
            File file1 = new File(this.playerDir, s + ".dat");

            if (file1.exists()) {
                return NBTCompressedStreamTools.a((InputStream) (new FileInputStream(file1)));
            }
        } catch (Exception exception) {
            a.warn("Failed to load player data for " + s);
        }

        return null;
    }
    // CraftBukkit end

    public IPlayerFileData getPlayerFileData() {
        return this;
    }

    public String[] getSeenPlayers() {
        String[] astring = this.playerDir.list();

        if (astring == null) {
            astring = new String[0];
        }

        for (int i = 0; i < astring.length; ++i) {
            if (astring[i].endsWith(".dat")) {
                astring[i] = astring[i].substring(0, astring[i].length() - 4);
            }
        }

        return astring;
    }

    public void a() {}

    public File getDataFile(String s) {
        return new File(this.dataDir, s + ".dat");
    }

    public String g() {
        return this.f;
    }

    // CraftBukkit start
    public UUID getUUID() {
        if (uuid != null) return uuid;
        File file1 = new File(this.baseDir, "uid.dat");
        if (file1.exists()) {
            DataInputStream dis = null;
            try {
                dis = new DataInputStream(new FileInputStream(file1));
                return uuid = new UUID(dis.readLong(), dis.readLong());
            } catch (IOException ex) {
                a.warn("Failed to read " + file1 + ", generating new random UUID", ex);
            } finally {
                if (dis != null) {
                    try {
                        dis.close();
                    } catch (IOException ex) {
                        // NOOP
                    }
                }
            }
        }
        uuid = UUID.randomUUID();
        DataOutputStream dos = null;
        try {
            dos = new DataOutputStream(new FileOutputStream(file1));
            dos.writeLong(uuid.getMostSignificantBits());
            dos.writeLong(uuid.getLeastSignificantBits());
        } catch (IOException ex) {
            a.warn("Failed to write " + file1, ex);
        } finally {
            if (dos != null) {
                try {
                    dos.close();
                } catch (IOException ex) {
                    // NOOP
                }
            }
        }
        return uuid;
    }

    public File getPlayerDir() {
        return playerDir;
    }
    // CraftBukkit end
}
