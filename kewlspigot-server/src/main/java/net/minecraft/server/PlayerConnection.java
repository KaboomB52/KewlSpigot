package net.minecraft.server;

import org.eytril.spigot.KewlSpigot;
import org.eytril.spigot.handler.MovementHandler;
import org.eytril.spigot.handler.PacketHandler;
import org.eytril.spigot.util.NotchUtil;

import co.aikar.timings.SpigotTimings;

import com.google.common.collect.Lists;
import com.google.common.primitives.Doubles;
import com.google.common.primitives.Floats;

import io.netty.buffer.Unpooled;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.Location;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.craftbukkit.inventory.CraftInventoryView;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.util.CraftChatMessage;
import org.bukkit.craftbukkit.util.LazyPlayerSet;
import org.bukkit.craftbukkit.util.Waitable;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.util.NumberConversions;
import org.github.paperspigot.PaperSpigotConfig;

public class PlayerConnection implements PacketListenerPlayIn, IUpdatePlayerListBox {

	private static final Logger c = LogManager.getLogger();
	public final NetworkManager networkManager;
	private final MinecraftServer minecraftServer;
	public EntityPlayer player;
	private int e;
	private int f;
	private int g;
	private boolean h;
	private int i;
	private long j;
	private long k;
	private volatile int chatThrottle;
	private static final AtomicIntegerFieldUpdater chatSpamField = AtomicIntegerFieldUpdater.newUpdater(PlayerConnection.class, "chatThrottle");
	private int m;
	private IntHashMap<Short> n = new IntHashMap();
	private double o;
	private double p;
	private double q;
	public boolean checkMovement = true;
	private int lastSwingTick;
	private int swings;
	private int sequentialSwingRateLimits;
	private int lastSequentialSwingTick;
	private int forwardSwingTick;
	private boolean processedDisconnect;

	public PlayerConnection(MinecraftServer minecraftserver, NetworkManager networkmanager, EntityPlayer entityplayer) {
		this.minecraftServer = minecraftserver;
		this.networkManager = networkmanager;

		networkmanager.a(this);

		this.player = entityplayer;

		entityplayer.playerConnection = this;

		this.server = minecraftserver.server;
	}

	private final org.bukkit.craftbukkit.CraftServer server;
	private int lastTick = MinecraftServer.currentTick;
	private int lastDropTick = MinecraftServer.currentTick;
	private int dropCount = 0;
	private static final int SURVIVAL_PLACE_DISTANCE_SQUARED = 6 * 6;
	private static final int CREATIVE_PLACE_DISTANCE_SQUARED = 7 * 7;

	private double lastPosX = Double.MAX_VALUE;
	private double lastPosY = Double.MAX_VALUE;
	private double lastPosZ = Double.MAX_VALUE;
	private float lastPitch = Float.MAX_VALUE;
	private float lastYaw = Float.MAX_VALUE;
	private boolean justTeleported = false;
	private boolean hasMoved;

	public CraftPlayer getPlayer() {
		return (this.player == null) ? null : this.player.getBukkitEntity();
	}

	private final static HashSet<Integer> invalidItems = new HashSet<>(java.util.Arrays.asList(8, 9, 10, 11, 26, 34, 36, 43, 51, 52, 55, 59, 60, 62, 63, 64, 68, 71, 74, 75, 83, 90, 92, 93, 94, 104, 105, 115, 117, 118, 119, 125, 127, 132, 140, 141, 142, 144)); // TODO: Check after every update.

	public void c() {
		this.h = false;

		++this.e;

		this.minecraftServer.methodProfiler.a("keepAlive");

		if ((long) this.e - this.k > 40L) {
			this.k = (long) this.e;
			this.j = this.d();
			this.i = (int) this.j;
			this.sendPacket(new PacketPlayOutKeepAlive(this.i));
		}

		this.minecraftServer.methodProfiler.b();

		for (int spam; (spam = this.chatThrottle) > 0 && !chatSpamField.compareAndSet(this, spam, spam - 1); ) ;

		if (this.m > 0) {
			--this.m;
		}

		if (this.player.D() > 0L && this.minecraftServer.getIdleTimeout() > 0 && MinecraftServer.az() - this.player.D() > (long) (this.minecraftServer.getIdleTimeout() * 1000 * 60)) {
			this.player.resetIdleTimer();
			this.disconnect("You have been idle for too long!");
		}
	}

	public NetworkManager a() {
		return this.networkManager;
	}

	public void disconnect(String s) {
		String leaveMessage = EnumChatFormat.YELLOW + this.player.getName() + " left the game.";

		PlayerKickEvent event = new PlayerKickEvent(this.server.getPlayer(this.player), s, leaveMessage);

		if (this.server.getServer().isRunning()) {
			this.server.getPluginManager().callEvent(event);
		}

		if (event.isCancelled()) {
			return;
		}

		s = event.getReason();

		final ChatComponentText chatcomponenttext = new ChatComponentText(s);

		this.networkManager.a(new PacketPlayOutKickDisconnect(chatcomponenttext), new GenericFutureListener() {
			public void operationComplete(Future future) throws Exception { // CraftBukkit - fix decompile error
				PlayerConnection.this.networkManager.close(chatcomponenttext);
			}
		}, new GenericFutureListener[0]);

		this.a(chatcomponenttext);
		this.networkManager.k();

		this.minecraftServer.postToMainThread(new Runnable() {
			public void run() {
				PlayerConnection.this.networkManager.l();
			}
		});
	}

	public void a(PacketPlayInSteerVehicle packetplayinsteervehicle) {
		PlayerConnectionUtils.ensureMainThread(packetplayinsteervehicle, this, this.player.u());
		this.player.a(packetplayinsteervehicle.a(), packetplayinsteervehicle.b(), packetplayinsteervehicle.c(), packetplayinsteervehicle.d());
	}

	private boolean b(PacketPlayInFlying packetplayinflying) {
		return !Doubles.isFinite(packetplayinflying.a()) || !Doubles.isFinite(packetplayinflying.b()) || !Doubles.isFinite(packetplayinflying.c()) || !Floats.isFinite(packetplayinflying.e()) || !Floats.isFinite(packetplayinflying.d());
	}

	public void a(PacketPlayInFlying packetplayinflying) {
		PlayerConnectionUtils.ensureMainThread(packetplayinflying, this, this.player.u());

		if (this.b(packetplayinflying)) {
			this.disconnect("Invalid move packet received");
		} else {
			WorldServer worldserver = this.minecraftServer.getWorldServer(this.player.dimension);

			this.h = true;
			if (!this.player.viewingCredits) {
				double d0 = this.player.locX;
				double d1 = this.player.locY;
				double d2 = this.player.locZ;
				double d3 = 0.0D;
				double d4 = packetplayinflying.a() - this.o;
				double d5 = packetplayinflying.b() - this.p;
				double d6 = packetplayinflying.c() - this.q;

				if (packetplayinflying.g()) {
					d3 = d4 * d4 + d5 * d5 + d6 * d6;
					if (!this.checkMovement && d3 < 0.25D) {
						this.checkMovement = true;
					}
				}

				Player player = this.getPlayer();

				if (!hasMoved) {
					Location curPos = player.getLocation();
					lastPosX = curPos.getX();
					lastPosY = curPos.getY();
					lastPosZ = curPos.getZ();
					lastYaw = curPos.getYaw();
					lastPitch = curPos.getPitch();
					hasMoved = true;
				}

				Location from = new Location(player.getWorld(), lastPosX, lastPosY, lastPosZ, lastYaw, lastPitch);
				Location to = player.getLocation().clone();

				if (packetplayinflying.hasPos && !(packetplayinflying.hasPos && packetplayinflying.y == -999.0D)) {
					to.setX(packetplayinflying.x);
					to.setY(packetplayinflying.y);
					to.setZ(packetplayinflying.z);
				}

				if (packetplayinflying.hasLook) {
					to.setYaw(packetplayinflying.yaw);
					to.setPitch(packetplayinflying.pitch);
				}

				double delta = Math.pow(this.lastPosX - to.getX(), 2) + Math.pow(this.lastPosY - to.getY(), 2) + Math.pow(this.lastPosZ - to.getZ(), 2);
				float deltaAngle = Math.abs(this.lastYaw - to.getYaw()) + Math.abs(this.lastPitch - to.getPitch());

				if (packetplayinflying.hasPos && delta > 0.0D && this.checkMovement && !this.player.dead) {
					for (MovementHandler handler : KewlSpigot.INSTANCE.getMovementHandlers()) {
						try {
							handler.handleUpdateLocation(player, to, from, packetplayinflying);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}

				if (packetplayinflying.hasLook && deltaAngle > 0.0F && this.checkMovement && !this.player.dead) {
					for (MovementHandler handler : KewlSpigot.INSTANCE.getMovementHandlers()) {
						try {
							handler.handleUpdateRotation(player, to, from, packetplayinflying);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}

				if (((packetplayinflying.hasPos && delta > 0.0D) || (packetplayinflying.hasLook && deltaAngle > 0.0F)) && (this.checkMovement && !this.player.dead)) {
					this.lastPosX = to.getX();
					this.lastPosY = to.getY();
					this.lastPosZ = to.getZ();
					this.lastYaw = to.getYaw();
					this.lastPitch = to.getPitch();

					if (KewlSpigot.INSTANCE.getConfig().isFirePlayerMoveEvent()) {
						Location oldTo = to.clone();
						PlayerMoveEvent event = new PlayerMoveEvent(player, from, to);
						this.server.getPluginManager().callEvent(event);

						if (event.isCancelled()) {
							this.player.playerConnection.sendPacket(new PacketPlayOutPosition(from.getX(), from.getY(), from.getZ(), from.getYaw(), from.getPitch(), Collections.<PacketPlayOutPosition.EnumPlayerTeleportFlags>emptySet()));
							return;
						}

						if (!oldTo.equals(event.getTo()) && !event.isCancelled()) {
							this.player.getBukkitEntity().teleport(event.getTo(), PlayerTeleportEvent.TeleportCause.UNKNOWN);
							return;
						}

						if (!from.equals(this.getPlayer().getLocation()) && this.justTeleported) {
							this.justTeleported = false;
							return;
						}
					}
				}

				if (this.checkMovement && !this.player.dead) {
					this.f = this.e;
					double d7;
					double d8;
					double d9;

					if (this.player.vehicle != null) {
						float f = this.player.yaw;
						float f1 = this.player.pitch;

						this.player.vehicle.al();

						d7 = this.player.locX;
						d8 = this.player.locY;
						d9 = this.player.locZ;

						if (packetplayinflying.h()) {
							f = packetplayinflying.d();
							f1 = packetplayinflying.e();
						}

						this.player.onGround = packetplayinflying.f();
						this.player.l();
						this.player.setLocation(d7, d8, d9, f, f1);

						if (this.player.vehicle != null) {
							this.player.vehicle.al();
						}

						this.minecraftServer.getPlayerList().d(this.player);

						if (this.player.vehicle != null) {
							this.player.vehicle.ai = true;

							if (d3 > 4.0D) {
								Entity entity = this.player.vehicle;

								this.player.playerConnection.sendPacket(new PacketPlayOutEntityTeleport(entity));
								this.a(this.player.locX, this.player.locY, this.player.locZ, this.player.yaw, this.player.pitch);
							}
						}

						if (this.checkMovement) {
							this.o = this.player.locX;
							this.p = this.player.locY;
							this.q = this.player.locZ;
						}

						worldserver.g(this.player);

						return;
					}

					if (this.player.isSleeping()) {
						this.player.l();
						this.player.setLocation(this.o, this.p, this.q, this.player.yaw, this.player.pitch);
						worldserver.g(this.player);
						return;
					}

					double d10 = this.player.locY;

					this.o = this.player.locX;
					this.p = this.player.locY;
					this.q = this.player.locZ;
					d7 = this.player.locX;
					d8 = this.player.locY;
					d9 = this.player.locZ;
					float f2 = this.player.yaw;
					float f3 = this.player.pitch;

					if (packetplayinflying.g() && packetplayinflying.b() == -999.0D) {
						packetplayinflying.a(false);
					}

					if (packetplayinflying.g()) {
						d7 = packetplayinflying.a();
						d8 = packetplayinflying.b();
						d9 = packetplayinflying.c();
						if (Math.abs(packetplayinflying.a()) > 3.0E7D || Math.abs(packetplayinflying.c()) > 3.0E7D) {
							this.disconnect("Illegal position");
							return;
						}
					}

					EntityPlayer entityPlayer = ((CraftPlayer) player).getHandle();
					if (packetplayinflying.h() && !entityPlayer.isFakingDeath() && entityPlayer.getDataWatcher().getFloat(6) > 0.0F) {
						f2 = packetplayinflying.d();
						f3 = packetplayinflying.e();
					}

					this.player.l();
					this.player.setLocation(this.o, this.p, this.q, f2, f3);
					if (!this.checkMovement) {
						return;
					}

					double d11 = d7 - this.player.locX;
					double d12 = d8 - this.player.locY;
					double d13 = d9 - this.player.locZ;
					double d14 = this.player.motX * this.player.motX + this.player.motY * this.player.motY + this.player.motZ * this.player.motZ;
					double d15 = d11 * d11 + d12 * d12 + d13 * d13;

					if (d15 - d14 > org.spigotmc.SpigotConfig.movedTooQuicklyThreshold && this.checkMovement && (!this.minecraftServer.T() || !this.minecraftServer.S().equals(this.player.getName()))) { // CraftBukkit - Added this.checkMovement condition to solve this check being triggered by teleports
						PlayerConnection.c.warn(this.player.getName() + " moved too quickly! " + d11 + "," + d12 + "," + d13 + " (" + d11 + ", " + d12 + ", " + d13 + ")");
						this.a(this.o, this.p, this.q, this.player.yaw, this.player.pitch);
						return;
					}

					float f4 = 0.0625F;
					boolean flag = worldserver.getCubes(this.player, this.player.getBoundingBox().shrink((double) f4, (double) f4, (double) f4)).isEmpty();

					if (this.player.onGround && !packetplayinflying.f() && d12 > 0.0D) {
						this.player.bF();
					}

					this.player.move(d11, d12, d13);
					this.player.onGround = packetplayinflying.f();
					double d16 = d12;

					d11 = d7 - this.player.locX;
					d12 = d8 - this.player.locY;

					if (d12 > -0.5D || d12 < 0.5D) {
						d12 = 0.0D;
					}

					d13 = d9 - this.player.locZ;
					d15 = d11 * d11 + d12 * d12 + d13 * d13;
					boolean flag1 = false;

					if (d15 > org.spigotmc.SpigotConfig.movedWronglyThreshold && !this.player.isSleeping() && !this.player.playerInteractManager.isCreative()) {
						flag1 = true;
						PlayerConnection.c.warn(this.player.getName() + " moved wrongly!");
					}

					this.player.setLocation(d7, d8, d9, f2, f3);
					this.player.checkMovement(this.player.locX - d0, this.player.locY - d1, this.player.locZ - d2);

					if (!this.player.noclip) {
						boolean flag2 = worldserver.getCubes(this.player, this.player.getBoundingBox().shrink((double) f4, (double) f4, (double) f4)).isEmpty();

						if (flag && (flag1 || !flag2) && !this.player.isSleeping()) {
							this.a(this.o, this.p, this.q, f2, f3);
							return;
						}
					}

					AxisAlignedBB axisalignedbb = this.player.getBoundingBox().grow((double) f4, (double) f4, (double) f4).a(0.0D, -0.55D, 0.0D);

					if (!this.minecraftServer.getAllowFlight() && !this.player.abilities.canFly && !worldserver.c(axisalignedbb)) {
						if (d16 >= -0.03125D) {
							++this.g;

							if (this.g > 80) {
								PlayerConnection.c.warn(this.player.getName() + " was kicked for floating too long!");
								this.disconnect("Flying is not enabled on this server");
								return;
							}
						}
					} else {
						this.g = 0;
					}

					this.player.onGround = packetplayinflying.f();
					this.minecraftServer.getPlayerList().d(this.player);
					this.player.a(this.player.locY - d10, packetplayinflying.f());
				} else if (this.e - this.f > 20) {
					this.a(this.o, this.p, this.q, this.player.yaw, this.player.pitch);
				}
			}

		}
	}

	public void a(double d0, double d1, double d2, float f, float f1) {
		this.a(d0, d1, d2, f, f1, Collections.emptySet());
	}

	public void a(double d0, double d1, double d2, float f, float f1, Set<PacketPlayOutPosition.EnumPlayerTeleportFlags> set) {
		Player player = this.getPlayer();
		Location from = player.getLocation();

		double x = d0;
		double y = d1;
		double z = d2;
		float yaw = f;
		float pitch = f1;
		if (set.contains(PacketPlayOutPosition.EnumPlayerTeleportFlags.X)) {
			x += from.getX();
		}
		if (set.contains(PacketPlayOutPosition.EnumPlayerTeleportFlags.Y)) {
			y += from.getY();
		}
		if (set.contains(PacketPlayOutPosition.EnumPlayerTeleportFlags.Z)) {
			z += from.getZ();
		}
		if (set.contains(PacketPlayOutPosition.EnumPlayerTeleportFlags.Y_ROT)) {
			yaw += from.getYaw();
		}
		if (set.contains(PacketPlayOutPosition.EnumPlayerTeleportFlags.X_ROT)) {
			pitch += from.getPitch();
		}


		Location to = new Location(this.getPlayer().getWorld(), x, y, z, yaw, pitch);
		PlayerTeleportEvent event = new PlayerTeleportEvent(player, from.clone(), to.clone(), PlayerTeleportEvent.TeleportCause.UNKNOWN);
		this.server.getPluginManager().callEvent(event);

		if (event.isCancelled() || to.equals(event.getTo())) {
			set.clear();
			to = event.isCancelled() ? event.getFrom() : event.getTo();
			d0 = to.getX();
			d1 = to.getY();
			d2 = to.getZ();
			f = to.getYaw();
			f1 = to.getPitch();
		}

		this.internalTeleport(d0, d1, d2, f, f1, set);
	}

	public void teleport(Location dest) {
		internalTeleport(dest.getX(), dest.getY(), dest.getZ(), dest.getYaw(), dest.getPitch(), Collections.emptySet());
	}

	private void internalTeleport(double d0, double d1, double d2, float f, float f1, Set set) {
		if (Float.isNaN(f)) {
			f = 0;
		}

		if (Float.isNaN(f1)) {
			f1 = 0;
		}

		this.justTeleported = true;
		this.checkMovement = false;
		this.o = d0;
		this.p = d1;
		this.q = d2;

		if (set.contains(PacketPlayOutPosition.EnumPlayerTeleportFlags.X)) {
			this.o += this.player.locX;
		}

		if (set.contains(PacketPlayOutPosition.EnumPlayerTeleportFlags.Y)) {
			this.p += this.player.locY;
		}

		if (set.contains(PacketPlayOutPosition.EnumPlayerTeleportFlags.Z)) {
			this.q += this.player.locZ;
		}

		float f2 = f;
		float f3 = f1;

		if (set.contains(PacketPlayOutPosition.EnumPlayerTeleportFlags.Y_ROT)) {
			f2 = f + this.player.yaw;
		}

		if (set.contains(PacketPlayOutPosition.EnumPlayerTeleportFlags.X_ROT)) {
			f3 = f1 + this.player.pitch;
		}

		this.lastPosX = this.o;
		this.lastPosY = this.p;
		this.lastPosZ = this.q;
		this.lastYaw = f2;
		this.lastPitch = f3;

		this.player.setLocation(this.o, this.p, this.q, f2, f3);
		this.player.playerConnection.sendPacket(new PacketPlayOutPosition(d0, d1, d2, f, f1, set));
	}

	public void a(PacketPlayInBlockDig packetplayinblockdig) {
		PlayerConnectionUtils.ensureMainThread(packetplayinblockdig, this, this.player.u());
		if (this.player.dead) return;
		WorldServer worldserver = this.minecraftServer.getWorldServer(this.player.dimension);
		BlockPosition blockposition = packetplayinblockdig.a();

		this.player.resetIdleTimer();

		switch (PlayerConnection.SyntheticClass_1.a[packetplayinblockdig.c().ordinal()]) {
			case 1: // DROP_ITEM
				if (!this.player.isSpectator()) {
					if (this.lastDropTick != MinecraftServer.currentTick) {
						this.dropCount = 0;
						this.lastDropTick = MinecraftServer.currentTick;
					} else {
						this.dropCount++;

						if (this.dropCount >= 20) {
							this.c.warn(this.player.getName() + " dropped their items too quickly!");
							this.disconnect("You dropped your items too quickly (Hacking?)");
							return;
						}
					}

					this.player.a(false);
				}

				return;

			case 2: // DROP_ALL_ITEMS
				if (!this.player.isSpectator()) {
					this.player.a(true);
				}

				return;

			case 3: // RELEASE_USE_ITEM
				this.player.bU();
				return;

			case 4: // START_DESTROY_BLOCK
			case 5: // ABORT_DESTROY_BLOCK
			case 6: // STOP_DESTROY_BLOCK
				double d0 = this.player.locX - ((double) blockposition.getX() + 0.5D);
				double d1 = this.player.locY - ((double) blockposition.getY() + 0.5D) + 1.5D;
				double d2 = this.player.locZ - ((double) blockposition.getZ() + 0.5D);
				double d3 = d0 * d0 + d1 * d1 + d2 * d2;

				if (d3 > 36.0D) {
					this.sendPacket(new PacketPlayOutBlockChange(worldserver, blockposition));
					return;
				} else if (blockposition.getY() >= this.minecraftServer.getMaxBuildHeight()) {
					return;
				} else {
					if (packetplayinblockdig.c() == PacketPlayInBlockDig.EnumPlayerDigType.START_DESTROY_BLOCK) {
						if (!this.minecraftServer.a(worldserver, blockposition, this.player) && worldserver.getWorldBorder().a(blockposition)) {
							this.player.playerInteractManager.a(blockposition, packetplayinblockdig.b());
						} else {
							if (KewlSpigot.INSTANCE.getConfig().isFireLeftClickBlock()) {
								CraftEventFactory.callPlayerInteractEvent(this.player, Action.LEFT_CLICK_BLOCK, blockposition, packetplayinblockdig.b(), this.player.inventory.getItemInHand());
							}

							this.player.playerConnection.sendPacket(new PacketPlayOutBlockChange(worldserver, blockposition));

							TileEntity tileentity = worldserver.getTileEntity(blockposition);

							if (tileentity != null) {
								this.player.playerConnection.sendPacket(tileentity.getUpdatePacket());
							}
						}
					} else {
						if (packetplayinblockdig.c() == PacketPlayInBlockDig.EnumPlayerDigType.STOP_DESTROY_BLOCK) {
							this.player.playerInteractManager.a(blockposition);
						} else if (packetplayinblockdig.c() == PacketPlayInBlockDig.EnumPlayerDigType.ABORT_DESTROY_BLOCK) {
							this.player.playerInteractManager.e();
						}

						if (worldserver.getType(blockposition).getBlock().getMaterial() != Material.AIR) {
							this.player.playerConnection.sendPacket(new PacketPlayOutBlockChange(worldserver, blockposition));
						}
					}

					return;
				}

			default:
				throw new IllegalArgumentException("Invalid player action");
		}
	}

	private long lastPlace = -1;
	private int packets = 0;

	public void a(PacketPlayInBlockPlace packetplayinblockplace) {
		PlayerConnectionUtils.ensureMainThread(packetplayinblockplace, this, this.player.u());
		WorldServer worldserver = this.minecraftServer.getWorldServer(this.player.dimension);
		boolean throttled = false;

		if (org.github.paperspigot.PaperSpigotConfig.interactLimitEnabled && lastPlace != -1 && packetplayinblockplace.timestamp - lastPlace < 30 && packets++ >= 4) {
			throttled = true;
		} else if (packetplayinblockplace.timestamp - lastPlace >= 30 || lastPlace == -1) {
			lastPlace = packetplayinblockplace.timestamp;
			packets = 0;
		}

		if (this.player.dead) return;

		boolean always = false;

		ItemStack itemstack = this.player.inventory.getItemInHand();
		boolean flag = false;
		BlockPosition blockposition = packetplayinblockplace.a();
		EnumDirection enumdirection = EnumDirection.fromType1(packetplayinblockplace.getFace());

		this.player.resetIdleTimer();
		if (packetplayinblockplace.getFace() == 255) {
			if (itemstack == null) {
				return;
			}

			int itemstackAmount = itemstack.count;

			if (!throttled) {
				boolean cancelled = false;

				if (packetplayinblockplace.getFace() == 255) {
					org.bukkit.event.player.PlayerInteractEvent event = CraftEventFactory.callPlayerInteractEvent(this.player, Action.RIGHT_CLICK_AIR, itemstack);
					cancelled = event.useItemInHand() == Event.Result.DENY;
				} else {
					if (player.playerInteractManager.firedInteract) {
						player.playerInteractManager.firedInteract = false;
						cancelled = player.playerInteractManager.interactResult;
					} else {
						EnumDirection enumDirection = NotchUtil.getDirection(
								this.player.pitch,
								this.player.yaw,
								this.player.locX,
								this.player.locY + this.player.getHeadHeight(),
								this.player.locZ,
								player.playerInteractManager.getGameMode()
						);

						org.bukkit.event.player.PlayerInteractEvent event = CraftEventFactory.callPlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK,
								packetplayinblockplace.a(), itemstack, true, enumDirection);
						cancelled = event.useItemInHand() == Event.Result.DENY;
					}
				}

				if (!cancelled) {
					this.player.playerInteractManager.useItem(this.player, this.player.world, itemstack);
				}
			}

			always = (itemstack.count != itemstackAmount) || itemstack.getItem() == Item.getItemOf(Blocks.WATERLILY);
		} else if (blockposition.getY() >= this.minecraftServer.getMaxBuildHeight() - 1 && (enumdirection == EnumDirection.UP || blockposition.getY() >= this.minecraftServer.getMaxBuildHeight())) {
			ChatMessage chatmessage = new ChatMessage("build.tooHigh", this.minecraftServer.getMaxBuildHeight());

			chatmessage.getChatModifier().setColor(EnumChatFormat.RED);
			this.player.playerConnection.sendPacket(new PacketPlayOutChat(chatmessage));

			flag = true;
		} else {
			Location eyeLoc = this.getPlayer().getEyeLocation();
			double reachDistance = NumberConversions.square(eyeLoc.getX() - blockposition.getX()) + NumberConversions.square(eyeLoc.getY() - blockposition.getY()) + NumberConversions.square(eyeLoc.getZ() - blockposition.getZ());

			if (reachDistance > (this.getPlayer().getGameMode() == org.bukkit.GameMode.CREATIVE ? CREATIVE_PLACE_DISTANCE_SQUARED : SURVIVAL_PLACE_DISTANCE_SQUARED)) {
				return;
			}

			if (!worldserver.getWorldBorder().a(blockposition)) {
				return;
			}

			if (this.checkMovement && this.player.e((double) blockposition.getX() + 0.5D, (double) blockposition.getY() + 0.5D, (double) blockposition.getZ() + 0.5D) < 64.0D && !this.minecraftServer.a(worldserver, blockposition, this.player) && worldserver.getWorldBorder().a(blockposition)) {
				always = throttled || !this.player.playerInteractManager.interact(this.player, worldserver, itemstack, blockposition, enumdirection, packetplayinblockplace.d(), packetplayinblockplace.e(), packetplayinblockplace.f());
			}

			flag = true;
		}

		if (flag) {
			this.player.playerConnection.sendPacket(new PacketPlayOutBlockChange(worldserver, blockposition));
			this.player.playerConnection.sendPacket(new PacketPlayOutBlockChange(worldserver, blockposition.shift(enumdirection)));
		}

		itemstack = this.player.inventory.getItemInHand();

		if (itemstack != null && itemstack.count == 0) {
			this.player.inventory.items[this.player.inventory.itemInHandIndex] = null;

			itemstack = null;
		}

		if (itemstack == null || itemstack.l() == 0) {
			this.player.g = true;
			this.player.inventory.items[this.player.inventory.itemInHandIndex] = ItemStack.b(this.player.inventory.items[this.player.inventory.itemInHandIndex]);
			Slot slot = this.player.activeContainer.getSlot(this.player.inventory, this.player.inventory.itemInHandIndex);

			this.player.activeContainer.b();

			this.player.g = false;

			if (!ItemStack.matches(this.player.inventory.getItemInHand(), packetplayinblockplace.getItemStack()) || always) {
				this.sendPacket(new PacketPlayOutSetSlot(this.player.activeContainer.windowId, slot.rawSlotIndex, this.player.inventory.getItemInHand()));
			}
		}

	}

	public void a(PacketPlayInSpectate packetplayinspectate) {
		PlayerConnectionUtils.ensureMainThread(packetplayinspectate, this, this.player.u());

		if (this.player.isSpectator()) {
			Entity entity = null;

			for (WorldServer worldserver : minecraftServer.worlds) {
				if (worldserver != null) {
					entity = packetplayinspectate.a(worldserver);
					if (entity != null) {
						break;
					}
				}
			}

			if (entity != null) {
				this.player.setSpectatorTarget(this.player);
				this.player.mount(null);
				this.player.getBukkitEntity().teleport(entity.getBukkitEntity(), PlayerTeleportEvent.TeleportCause.SPECTATE);
			}
		}

	}

	public void a(PacketPlayInResourcePackStatus packetplayinresourcepackstatus) {
		this.server.getPluginManager().callEvent(new PlayerResourcePackStatusEvent(getPlayer(), PlayerResourcePackStatusEvent.Status.values()[packetplayinresourcepackstatus.b.ordinal()]));
	}

	public void a(IChatBaseComponent ichatbasecomponent) {
		if (this.processedDisconnect) {
			return;
		} else {
			this.processedDisconnect = true;
		}

		PlayerConnection.c.info(this.player.getName() + " lost connection: " + ichatbasecomponent.c());
		this.player.q();

		String quitMessage = this.minecraftServer.getPlayerList().disconnect(this.player);

		if ((quitMessage != null) && (quitMessage.length() > 0)) {
			this.minecraftServer.getPlayerList().sendMessage(CraftChatMessage.fromString(quitMessage));
		}

		if (this.minecraftServer.T() && this.player.getName().equals(this.minecraftServer.S())) {
			this.minecraftServer.safeShutdown();
		}

	}

	public void sendPacket(final Packet packet) {
		if (packet instanceof PacketPlayOutChat) {
			PacketPlayOutChat packetplayoutchat = (PacketPlayOutChat) packet;
			EntityHuman.EnumChatVisibility chatVisibility = this.player.getChatFlags();

			if (chatVisibility == EntityHuman.EnumChatVisibility.HIDDEN) {
				return;
			}

			if (chatVisibility == EntityHuman.EnumChatVisibility.SYSTEM && !packetplayoutchat.b()) {
				return;
			}
		}

		if (packet == null || this.processedDisconnect) {
			return;
		} else if (packet instanceof PacketPlayOutSpawnPosition) {
			PacketPlayOutSpawnPosition packet6 = (PacketPlayOutSpawnPosition) packet;
			this.player.compassTarget = new Location(this.getPlayer().getWorld(), packet6.position.getX(), packet6.position.getY(), packet6.position.getZ());
		}

		try {
			this.networkManager.handle(packet);

			for (PacketHandler handler : KewlSpigot.INSTANCE.getPacketHandlers()) {
				try {
					handler.handleSentPacket(this, packet);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		} catch (Throwable throwable) {
			CrashReport crashreport = CrashReport.a(throwable, "Sending packet");
			CrashReportSystemDetails crashreportsystemdetails = crashreport.a("Packet being sent");

			crashreportsystemdetails.a("Packet class", new Callable() {
				public String a() throws Exception {
					return packet.getClass().getCanonicalName();
				}

				public Object call() throws Exception {
					return this.a();
				}
			});
			throw new ReportedException(crashreport);
		}
	}

	public void a(PacketPlayInHeldItemSlot packetplayinhelditemslot) {
		if (this.player.dead) {
			return;
		}

		PlayerConnectionUtils.ensureMainThread(packetplayinhelditemslot, this, this.player.u());

		if (packetplayinhelditemslot.a() >= 0 && packetplayinhelditemslot.a() < PlayerInventory.getHotbarSize()) {
			PlayerItemHeldEvent event = new PlayerItemHeldEvent(this.getPlayer(), this.player.inventory.itemInHandIndex, packetplayinhelditemslot.a());
			this.server.getPluginManager().callEvent(event);

			if (event.isCancelled()) {
				this.sendPacket(new PacketPlayOutHeldItemSlot(this.player.inventory.itemInHandIndex));
				this.player.resetIdleTimer();
				return;
			}

			this.player.inventory.itemInHandIndex = packetplayinhelditemslot.a();
			this.player.resetIdleTimer();
		} else {
			PlayerConnection.c.warn(this.player.getName() + " tried to set an invalid carried item");
			this.disconnect("Invalid hotbar selection (Hacking?)");
		}
	}

	public void a(PacketPlayInChat packetplayinchat) {
		boolean isSync = packetplayinchat.a().startsWith("/");

		if (packetplayinchat.a().startsWith("/")) {
			PlayerConnectionUtils.ensureMainThread(packetplayinchat, this, this.player.u());
		}

		if (this.player.dead || this.player.getChatFlags() == EntityHuman.EnumChatVisibility.HIDDEN) {
			ChatMessage chatmessage = new ChatMessage("chat.cannotSend", new Object[0]);

			chatmessage.getChatModifier().setColor(EnumChatFormat.RED);
			this.sendPacket(new PacketPlayOutChat(chatmessage));
		} else {
			this.player.resetIdleTimer();
			String s = packetplayinchat.a();

			s = StringUtils.normalizeSpace(s);

			for (int i = 0; i < s.length(); ++i) {
				if (!SharedConstants.isAllowedChatCharacter(s.charAt(i))) {
					if (!isSync) {
						Waitable waitable = new Waitable() {
							@Override
							protected Object evaluate() {
								PlayerConnection.this.disconnect("Illegal characters in chat");
								return null;
							}
						};

						this.minecraftServer.processQueue.add(waitable);

						try {
							waitable.get();
						} catch (InterruptedException e) {
							Thread.currentThread().interrupt();
						} catch (ExecutionException e) {
							throw new RuntimeException(e);
						}
					} else {
						this.disconnect("Illegal characters in chat");
					}

					return;
				}
			}

			if (isSync) {
				try {
					this.minecraftServer.server.playerCommandState = true;
					this.handleCommand(s);
				} finally {
					this.minecraftServer.server.playerCommandState = false;
				}
			} else if (s.isEmpty()) {
				c.warn(this.player.getName() + " tried to send an empty message");
			} else if (getPlayer().isConversing()) {
				final String message = s;
				this.minecraftServer.processQueue.add(new Waitable() {
					@Override
					protected Object evaluate() {
						getPlayer().acceptConversationInput(message);
						return null;
					}
				});
			} else if (this.player.getChatFlags() == EntityHuman.EnumChatVisibility.SYSTEM) {
				ChatMessage chatmessage = new ChatMessage("chat.cannotSend", new Object[0]);

				chatmessage.getChatModifier().setColor(EnumChatFormat.RED);
				this.sendPacket(new PacketPlayOutChat(chatmessage));
			} else if (true) {
				this.chat(s, true);
			} else {
				ChatMessage chatmessage1 = new ChatMessage("chat.type.text", new Object[]{this.player.getScoreboardDisplayName(), s});

				this.minecraftServer.getPlayerList().sendMessage(chatmessage1, false);
			}

			boolean counted = true;

			for (String exclude : org.spigotmc.SpigotConfig.spamExclusions) {
				if (exclude != null && s.startsWith(exclude)) {
					counted = false;
					break;
				}
			}

			if (counted && chatSpamField.addAndGet(this, 20) > 60 && !this.minecraftServer.getPlayerList().isOp(this.player.getProfile())) { // Spigot
				if (!isSync) {
					Waitable waitable = new Waitable() {
						@Override
						protected Object evaluate() {
							PlayerConnection.this.disconnect("disconnect.spam");
							return null;
						}
					};

					this.minecraftServer.processQueue.add(waitable);

					try {
						waitable.get();
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					} catch (ExecutionException e) {
						throw new RuntimeException(e);
					}
				} else {
					this.disconnect("disconnect.spam");
				}
			}
		}
	}

	public void chat(String s, boolean async) {
		if (s.isEmpty() || this.player.getChatFlags() == EntityHuman.EnumChatVisibility.HIDDEN) {
			return;
		}

		if (!async && s.startsWith("/")) {
			if (!org.bukkit.Bukkit.isPrimaryThread()) {
				final String fCommandLine = s;

				MinecraftServer.LOGGER.log(org.apache.logging.log4j.Level.ERROR, "Command Dispatched Async: " + fCommandLine);
				MinecraftServer.LOGGER.log(org.apache.logging.log4j.Level.ERROR, "Please notify author of plugin causing this execution to fix this bug! see: http://bit.ly/1oSiM6C", new Throwable());

				Waitable wait = new Waitable() {
					@Override
					protected Object evaluate() {
						chat(fCommandLine, false);
						return null;
					}
				};

				minecraftServer.processQueue.add(wait);

				try {
					wait.get();
					return;
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				} catch (Exception e) {
					throw new RuntimeException("Exception processing chat command", e.getCause());
				}
			}

			this.handleCommand(s);
		} else if (this.player.getChatFlags() == EntityHuman.EnumChatVisibility.SYSTEM) {

		} else {
			Player player = this.getPlayer();
			AsyncPlayerChatEvent event = new AsyncPlayerChatEvent(async, player, s, new LazyPlayerSet());
			this.server.getPluginManager().callEvent(event);

			if (PlayerChatEvent.getHandlerList().getRegisteredListeners().length != 0) {
				final PlayerChatEvent queueEvent = new PlayerChatEvent(player, event.getMessage(), event.getFormat(), event.getRecipients());

				queueEvent.setCancelled(event.isCancelled());

				Waitable waitable = new Waitable() {
					@Override
					protected Object evaluate() {
						org.bukkit.Bukkit.getPluginManager().callEvent(queueEvent);

						if (queueEvent.isCancelled()) {
							return null;
						}

						String message = String.format(queueEvent.getFormat(), queueEvent.getPlayer().getDisplayName(), queueEvent.getMessage());
						PlayerConnection.this.minecraftServer.console.sendMessage(message);
						if (((LazyPlayerSet) queueEvent.getRecipients()).isLazy()) {
							for (Object player : PlayerConnection.this.minecraftServer.getPlayerList().players) {
								((EntityPlayer) player).sendMessage(CraftChatMessage.fromString(message));
							}
						} else {
							for (Player player : queueEvent.getRecipients()) {
								player.sendMessage(message);
							}
						}
						return null;
					}
				};

				if (async) {
					minecraftServer.processQueue.add(waitable);
				} else {
					waitable.run();
				}

				try {
					waitable.get();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				} catch (ExecutionException e) {
					throw new RuntimeException("Exception processing chat event", e.getCause());
				}
			} else {
				if (event.isCancelled()) {
					return;
				}

				s = String.format(event.getFormat(), event.getPlayer().getDisplayName(), event.getMessage());
				minecraftServer.console.sendMessage(s);
				if (((LazyPlayerSet) event.getRecipients()).isLazy()) {
					for (Object recipient : minecraftServer.getPlayerList().players) {
						((EntityPlayer) recipient).sendMessage(CraftChatMessage.fromString(s));
					}
				} else {
					for (Player recipient : event.getRecipients()) {
						recipient.sendMessage(s);
					}
				}
			}
		}
	}

	private void handleCommand(String s) {
		SpigotTimings.playerCommandTimer.startTiming();
		if (org.spigotmc.SpigotConfig.logCommands)
			this.c.info(this.player.getName() + " issued server command: " + s);

		CraftPlayer player = this.getPlayer();

		PlayerCommandPreprocessEvent event = new PlayerCommandPreprocessEvent(player, s, new LazyPlayerSet());
		this.server.getPluginManager().callEvent(event);

		if (event.isCancelled()) {
			SpigotTimings.playerCommandTimer.stopTiming();
			return;
		}

		try {
			if (this.server.dispatchCommand(event.getPlayer(), event.getMessage().substring(1))) {
				SpigotTimings.playerCommandTimer.stopTiming();
				return;
			}
		} catch (org.bukkit.command.CommandException ex) {
			player.sendMessage(org.bukkit.ChatColor.RED + "An internal error occurred while attempting to perform this command");
			java.util.logging.Logger.getLogger(PlayerConnection.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
			SpigotTimings.playerCommandTimer.stopTiming();
			return;
		}

		SpigotTimings.playerCommandTimer.stopTiming();
	}

	public void a(PacketPlayInArmAnimation packetplayinarmanimation) {
		if (this.player.dead) {
			return;
		}

		if (KewlSpigot.INSTANCE.getConfig().isInvalidArmAnimationKick()) {
			if (lastSwingTick != MinecraftServer.currentTick) {
				swings = 0;
				lastSwingTick = MinecraftServer.currentTick;
			} else {
				if (swings > 5) {
					if (MinecraftServer.currentTick - 1 == lastSequentialSwingTick) {
						sequentialSwingRateLimits++;
					} else if (MinecraftServer.currentTick != lastSequentialSwingTick) {
						sequentialSwingRateLimits = 0;
					} else if (75 < sequentialSwingRateLimits) {
						this.disconnect("Invalid arm animations");
					}
					lastSequentialSwingTick = MinecraftServer.currentTick;
					return;
				}
				swings++;
			}
		}

		PlayerConnectionUtils.ensureMainThread(packetplayinarmanimation, this, this.player.u());

		this.player.resetIdleTimer();

		if (KewlSpigot.INSTANCE.getConfig().isFireLeftClickAir()) {
			float pitch = this.player.pitch;
			float yaw = this.player.yaw;
			double locX = this.player.locX;
			double locY = this.player.locY + (double) this.player.getHeadHeight();
			double locZ = this.player.locZ;
			Vec3D vec3d = new Vec3D(locX, locY, locZ);
			float f3 = MathHelper.cos(-yaw * 0.017453292F - 3.1415927F);
			float f4 = (float) MathHelper.sin(-yaw * 0.017453292F - 3.1415927F);
			float f5 = -MathHelper.cos(-pitch * 0.017453292F);
			float f6 = (float) MathHelper.sin(-pitch * 0.017453292F);
			float f7 = f4 * f5;
			float f8 = f3 * f5;
			double d3 = player.playerInteractManager.getGameMode() == WorldSettings.EnumGamemode.CREATIVE ? 5.0D : 4.5D;
			Vec3D vec3d1 = vec3d.add((double) f7 * d3, (double) f6 * d3, (double) f8 * d3);

			MovingObjectPosition movingobjectposition = this.player.world.rayTrace(vec3d, vec3d1, false);

			if (movingobjectposition == null || movingobjectposition.type != MovingObjectPosition.EnumMovingObjectType.BLOCK) {
				CraftEventFactory.callPlayerInteractEvent(this.player, Action.LEFT_CLICK_AIR, this.player.inventory.getItemInHand());
			}
		}

		PlayerAnimationEvent event = new PlayerAnimationEvent(this.getPlayer());

		this.server.getPluginManager().callEvent(event);

		if (event.isCancelled()) {
			return;
		}

		if (MinecraftServer.currentTick >= this.forwardSwingTick) {
			this.player.bw();
			this.forwardSwingTick = MinecraftServer.currentTick + 5;
		}

		if (this.player.isBlocking()) {
			this.player.bU();
		}
	}

	public void a(PacketPlayInEntityAction packetplayinentityaction) {
		PlayerConnectionUtils.ensureMainThread(packetplayinentityaction, this, this.player.u());

		if (this.player.dead) {
			return;
		}

		switch (packetplayinentityaction.b()) {
			case START_SNEAKING:
			case STOP_SNEAKING:
				PlayerToggleSneakEvent event = new PlayerToggleSneakEvent(this.getPlayer(), packetplayinentityaction.b() == PacketPlayInEntityAction.EnumPlayerAction.START_SNEAKING);

				this.server.getPluginManager().callEvent(event);

				if (event.isCancelled()) {
					return;
				}

				break;
			case START_SPRINTING:
			case STOP_SPRINTING:
				PlayerToggleSprintEvent e2 = new PlayerToggleSprintEvent(this.getPlayer(), packetplayinentityaction.b() == PacketPlayInEntityAction.EnumPlayerAction.START_SPRINTING);

				this.server.getPluginManager().callEvent(e2);

				if (e2.isCancelled()) {
					return;
				}

				break;
		}

		this.player.resetIdleTimer();
		switch (PlayerConnection.SyntheticClass_1.b[packetplayinentityaction.b().ordinal()]) {
			case 1:
				this.player.setSneaking(true);
				break;
			case 2:
				this.player.setSneaking(false);
				break;
			case 3:
				this.player.setSprinting(true);
				this.player.setSneaking(false);
				break;
			case 4:
				this.player.setSprinting(false);
				break;
			case 5:
				this.player.a(false, true, true);
				break;

			case 6:
				if (this.player.vehicle instanceof EntityHorse) {
					((EntityHorse) this.player.vehicle).v(packetplayinentityaction.c());
				}
				break;

			case 7:
				if (this.player.vehicle instanceof EntityHorse) {
					((EntityHorse) this.player.vehicle).g(this.player);
				}
				break;

			default:
				throw new IllegalArgumentException("Invalid client command!");
		}

	}

	public void a(PacketPlayInUseEntity packetplayinuseentity) {
		if (this.player.dead) {
			return;
		}

		PlayerConnectionUtils.ensureMainThread(packetplayinuseentity, this, this.player.u());

		WorldServer worldserver = this.minecraftServer.getWorldServer(this.player.dimension);
		Entity entity = packetplayinuseentity.a(worldserver);

		if (entity == player && !player.isSpectator()) {
			disconnect("Cannot interact with self!");
			return;
		}

		this.player.resetIdleTimer();

		if (entity != null) {
			double d0 = 36.0D;

			if (this.player.h(entity) < d0) {
				ItemStack itemInHand = this.player.inventory.getItemInHand();

				if (packetplayinuseentity.a() == PacketPlayInUseEntity.EnumEntityUseAction.INTERACT || packetplayinuseentity.a() == PacketPlayInUseEntity.EnumEntityUseAction.INTERACT_AT) {
					boolean triggerLeashUpdate = itemInHand != null && itemInHand.getItem() == Items.LEAD && entity instanceof EntityInsentient;
					Item origItem = this.player.inventory.getItemInHand() == null ? null : this.player.inventory.getItemInHand().getItem();
					PlayerInteractEntityEvent event;

					if (packetplayinuseentity.a() == PacketPlayInUseEntity.EnumEntityUseAction.INTERACT) {
						event = new PlayerInteractEntityEvent(this.getPlayer(), entity.getBukkitEntity());
					} else {
						Vec3D target = packetplayinuseentity.b();
						event = new PlayerInteractAtEntityEvent(this.getPlayer(), entity.getBukkitEntity(), new org.bukkit.util.Vector(target.a, target.b, target.c));
					}

					this.server.getPluginManager().callEvent(event);

					if (triggerLeashUpdate && (event.isCancelled() || this.player.inventory.getItemInHand() == null || this.player.inventory.getItemInHand().getItem() != Items.LEAD)) {
						this.sendPacket(new PacketPlayOutAttachEntity(1, entity, ((EntityInsentient) entity).getLeashHolder()));
					}

					if (event.isCancelled() || this.player.inventory.getItemInHand() == null || this.player.inventory.getItemInHand().getItem() != origItem) {
						this.sendPacket(new PacketPlayOutEntityMetadata(entity.getId(), entity.datawatcher, true));
					}

					if (event.isCancelled()) {
						return;
					}
				}

				if (packetplayinuseentity.a() == PacketPlayInUseEntity.EnumEntityUseAction.INTERACT) {
					this.player.u(entity);

					if (itemInHand != null && itemInHand.count <= -1) {
						this.player.updateInventory(this.player.activeContainer);
					}
				} else if (packetplayinuseentity.a() == PacketPlayInUseEntity.EnumEntityUseAction.INTERACT_AT) {
					entity.a(this.player, packetplayinuseentity.b());

					if (itemInHand != null && itemInHand.count <= -1) {
						this.player.updateInventory(this.player.activeContainer);
					}
				} else if (packetplayinuseentity.a() == PacketPlayInUseEntity.EnumEntityUseAction.ATTACK) {
					if (entity instanceof EntityItem || entity instanceof EntityExperienceOrb || entity instanceof EntityArrow || (entity == this.player && !player.isSpectator())) { // CraftBukkit
						this.disconnect("Attempting to attack an invalid entity");
						this.minecraftServer.warning("Player " + this.player.getName() + " tried to attack an invalid entity");
						return;
					}

					// fix sword blocking desync
					if (this.player.isBlocking()) {
						this.player.bU();
					}
					// - kabberz

					this.player.attack(entity);

					if (itemInHand != null && itemInHand.count <= -1) {
						this.player.updateInventory(this.player.activeContainer);
					}
				}
			}
		}
	}

	public void a(PacketPlayInClientCommand packetplayinclientcommand) {
		PlayerConnectionUtils.ensureMainThread(packetplayinclientcommand, this, this.player.u());

		this.player.resetIdleTimer();

		PacketPlayInClientCommand.EnumClientCommand clientCommand = packetplayinclientcommand.a();

		switch (PlayerConnection.SyntheticClass_1.c[clientCommand.ordinal()]) {
			case 1:
				if (this.player.viewingCredits) {
					this.minecraftServer.getPlayerList().changeDimension(this.player, 0, PlayerTeleportEvent.TeleportCause.END_PORTAL);
				} else if (this.player.u().getWorldData().isHardcore()) {
					if (this.minecraftServer.T() && this.player.getName().equals(this.minecraftServer.S())) {
						this.player.playerConnection.disconnect("You have died. Game over, man, it\'s game over!");
						this.minecraftServer.aa();
					} else {
						GameProfileBanEntry gameprofilebanentry = new GameProfileBanEntry(this.player.getProfile(), null, "(You just lost the game)", null, "Death in Hardcore");

						this.minecraftServer.getPlayerList().getProfileBans().add(gameprofilebanentry);
						this.player.playerConnection.disconnect("You have died. Game over, man, it\'s game over!");
					}
				} else {
					if (this.player.getHealth() > 0.0F) {
						return;
					}

					this.player = this.minecraftServer.getPlayerList().moveToWorld(this.player, 0, false);
				}
				break;

			case 2:
				this.player.getStatisticManager().a(this.player);
				break;

			case 3:
				this.player.b(AchievementList.f);
		}

	}

	public void a(PacketPlayInCloseWindow packetplayinclosewindow) {
		if (this.player.dead) {
			return;
		}

		PlayerConnectionUtils.ensureMainThread(packetplayinclosewindow, this, this.player.u());

		CraftEventFactory.handleInventoryCloseEvent(this.player);

		this.player.p();
	}

	public void a(PacketPlayInWindowClick packetplayinwindowclick) {
		if (this.player.dead) {
			return;
		}

		PlayerConnectionUtils.ensureMainThread(packetplayinwindowclick, this, this.player.u());

		this.player.resetIdleTimer();

		if (this.player.activeContainer.windowId == packetplayinwindowclick.a() && this.player.activeContainer.c(this.player)) {
			boolean cancelled = this.player.isSpectator();

			if (packetplayinwindowclick.b() < -1 && packetplayinwindowclick.b() != -999) {
				return;
			}

			InventoryView inventory = this.player.activeContainer.getBukkitView();
			SlotType type = CraftInventoryView.getSlotType(inventory, packetplayinwindowclick.b());
			InventoryClickEvent event = null;
			ClickType click = ClickType.UNKNOWN;
			InventoryAction action = InventoryAction.UNKNOWN;
			ItemStack itemstack = null;

			if (packetplayinwindowclick.b() == -1) {
				type = SlotType.OUTSIDE;
				click = packetplayinwindowclick.c() == 0 ? ClickType.WINDOW_BORDER_LEFT : ClickType.WINDOW_BORDER_RIGHT;
				action = InventoryAction.NOTHING;
			} else if (packetplayinwindowclick.f() == 0) {
				if (packetplayinwindowclick.c() == 0) {
					click = ClickType.LEFT;
				} else if (packetplayinwindowclick.c() == 1) {
					click = ClickType.RIGHT;
				}

				if (packetplayinwindowclick.c() == 0 || packetplayinwindowclick.c() == 1) {
					action = InventoryAction.NOTHING;

					if (packetplayinwindowclick.b() == -999) {
						if (player.inventory.getCarried() != null) {
							action = packetplayinwindowclick.c() == 0 ? InventoryAction.DROP_ALL_CURSOR : InventoryAction.DROP_ONE_CURSOR;
						}
					} else {
						Slot slot = this.player.activeContainer.getSlot(packetplayinwindowclick.b());

						if (slot != null) {
							ItemStack clickedItem = slot.getItem();
							ItemStack cursor = player.inventory.getCarried();

							if (clickedItem == null) {
								if (cursor != null) {
									action = packetplayinwindowclick.c() == 0 ? InventoryAction.PLACE_ALL : InventoryAction.PLACE_ONE;
								}
							} else if (slot.isAllowed(player)) {
								if (cursor == null) {
									action = packetplayinwindowclick.c() == 0 ? InventoryAction.PICKUP_ALL : InventoryAction.PICKUP_HALF;
								} else if (slot.isAllowed(cursor)) {
									if (clickedItem.doMaterialsMatch(cursor) && ItemStack.equals(clickedItem, cursor)) {
										int toPlace = packetplayinwindowclick.c() == 0 ? cursor.count : 1;
										toPlace = Math.min(toPlace, clickedItem.getMaxStackSize() - clickedItem.count);
										toPlace = Math.min(toPlace, slot.inventory.getMaxStackSize() - clickedItem.count);

										if (toPlace == 1) {
											action = InventoryAction.PLACE_ONE;
										} else if (toPlace == cursor.count) {
											action = InventoryAction.PLACE_ALL;
										} else if (toPlace < 0) {
											action = toPlace != -1 ? InventoryAction.PICKUP_SOME : InventoryAction.PICKUP_ONE;
										} else if (toPlace != 0) {
											action = InventoryAction.PLACE_SOME;
										}
									} else if (cursor.count <= slot.getMaxStackSize()) {
										action = InventoryAction.SWAP_WITH_CURSOR;
									}
								} else if (cursor.getItem() == clickedItem.getItem() && (!cursor.usesData() || cursor.getData() == clickedItem.getData()) && ItemStack.equals(cursor, clickedItem)) {
									if (clickedItem.count >= 0) {
										if (clickedItem.count + cursor.count <= cursor.getMaxStackSize()) {
											action = InventoryAction.PICKUP_ALL;
										}
									}
								}
							}
						}
					}
				}
			} else if (packetplayinwindowclick.f() == 1) {
				if (packetplayinwindowclick.c() == 0) {
					click = ClickType.SHIFT_LEFT;
				} else if (packetplayinwindowclick.c() == 1) {
					click = ClickType.SHIFT_RIGHT;
				}
				if (packetplayinwindowclick.c() == 0 || packetplayinwindowclick.c() == 1) {
					if (packetplayinwindowclick.b() < 0) {
						action = InventoryAction.NOTHING;
					} else {
						Slot slot = this.player.activeContainer.getSlot(packetplayinwindowclick.b());

						if (slot != null && slot.isAllowed(this.player) && slot.hasItem()) {
							action = InventoryAction.MOVE_TO_OTHER_INVENTORY;
						} else {
							action = InventoryAction.NOTHING;
						}
					}
				}
			} else if (packetplayinwindowclick.f() == 2) {
				if (packetplayinwindowclick.c() >= 0 && packetplayinwindowclick.c() < 9) {
					click = ClickType.NUMBER_KEY;
					Slot clickedSlot = this.player.activeContainer.getSlot(packetplayinwindowclick.b());

					if (clickedSlot.isAllowed(player)) {
						ItemStack hotbar = this.player.inventory.getItem(packetplayinwindowclick.c());
						boolean canCleanSwap = hotbar == null || (clickedSlot.inventory == player.inventory && clickedSlot.isAllowed(hotbar));

						if (clickedSlot.hasItem()) {
							if (canCleanSwap) {
								action = InventoryAction.HOTBAR_SWAP;
							} else {
								int firstEmptySlot = player.inventory.getFirstEmptySlotIndex();

								if (firstEmptySlot > -1) {
									action = InventoryAction.HOTBAR_MOVE_AND_READD;
								} else {
									action = InventoryAction.NOTHING;
								}
							}
						} else if (!clickedSlot.hasItem() && hotbar != null && clickedSlot.isAllowed(hotbar)) {
							action = InventoryAction.HOTBAR_SWAP;
						} else {
							action = InventoryAction.NOTHING;
						}
					} else {
						action = InventoryAction.NOTHING;
					}

					event = new InventoryClickEvent(inventory, type, packetplayinwindowclick.b(), click, action, packetplayinwindowclick.c());
				}
			} else if (packetplayinwindowclick.f() == 3) {
				if (packetplayinwindowclick.c() == 2) {
					click = ClickType.MIDDLE;

					if (packetplayinwindowclick.b() == -999) {
						action = InventoryAction.NOTHING;
					} else {
						Slot slot = this.player.activeContainer.getSlot(packetplayinwindowclick.b());

						if (slot != null && slot.hasItem() && player.abilities.canInstantlyBuild && player.inventory.getCarried() == null) {
							action = InventoryAction.CLONE_STACK;
						} else {
							action = InventoryAction.NOTHING;
						}
					}
				} else {
					click = ClickType.UNKNOWN;
					action = InventoryAction.UNKNOWN;
				}
			} else if (packetplayinwindowclick.f() == 4) {
				if (packetplayinwindowclick.b() >= 0) {
					if (packetplayinwindowclick.c() == 0) {
						click = ClickType.DROP;
						Slot slot = this.player.activeContainer.getSlot(packetplayinwindowclick.b());

						if (slot != null && slot.hasItem() && slot.isAllowed(player) && slot.getItem() != null && slot.getItem().getItem() != Item.getItemOf(Blocks.AIR)) {
							action = InventoryAction.DROP_ONE_SLOT;
						} else {
							action = InventoryAction.NOTHING;
						}
					} else if (packetplayinwindowclick.c() == 1) {
						click = ClickType.CONTROL_DROP;
						Slot slot = this.player.activeContainer.getSlot(packetplayinwindowclick.b());

						if (slot != null && slot.hasItem() && slot.isAllowed(player) && slot.getItem() != null && slot.getItem().getItem() != Item.getItemOf(Blocks.AIR)) {
							action = InventoryAction.DROP_ALL_SLOT;
						} else {
							action = InventoryAction.NOTHING;
						}
					}
				} else {
					click = ClickType.LEFT;

					if (packetplayinwindowclick.c() == 1) {
						click = ClickType.RIGHT;
					}

					action = InventoryAction.NOTHING;
				}
			} else if (packetplayinwindowclick.f() == 5) {
				itemstack = this.player.activeContainer.clickItem(packetplayinwindowclick.b(), packetplayinwindowclick.c(), 5, this.player);
			} else if (packetplayinwindowclick.f() == 6) {
				click = ClickType.DOUBLE_CLICK;
				action = InventoryAction.NOTHING;

				if (packetplayinwindowclick.b() >= 0 && this.player.inventory.getCarried() != null) {
					ItemStack cursor = this.player.inventory.getCarried();
					action = InventoryAction.NOTHING;

					if (inventory.getTopInventory().contains(org.bukkit.Material.getMaterial(Item.getId(cursor.getItem()))) || inventory.getBottomInventory().contains(org.bukkit.Material.getMaterial(Item.getId(cursor.getItem())))) {
						action = InventoryAction.COLLECT_TO_CURSOR;
					}
				}
			}

			if (packetplayinwindowclick.f() != 5) {
				if (click == ClickType.NUMBER_KEY) {
					event = new InventoryClickEvent(inventory, type, packetplayinwindowclick.b(), click, action, packetplayinwindowclick.c());
				} else {
					event = new InventoryClickEvent(inventory, type, packetplayinwindowclick.b(), click, action);
				}

				org.bukkit.inventory.Inventory top = inventory.getTopInventory();

				if (packetplayinwindowclick.b() == 0 && top instanceof CraftingInventory) {
					org.bukkit.inventory.Recipe recipe = ((CraftingInventory) top).getRecipe();

					if (recipe != null) {
						if (click == ClickType.NUMBER_KEY) {
							event = new CraftItemEvent(recipe, inventory, type, packetplayinwindowclick.b(), click, action, packetplayinwindowclick.c());
						} else {
							event = new CraftItemEvent(recipe, inventory, type, packetplayinwindowclick.b(), click, action);
						}
					}
				}

				event.setCancelled(cancelled);
				server.getPluginManager().callEvent(event);

				switch (event.getResult()) {
					case ALLOW:
					case DEFAULT:
						itemstack = this.player.activeContainer.clickItem(packetplayinwindowclick.b(), packetplayinwindowclick.c(), packetplayinwindowclick.f(), this.player);

						if (itemstack != null &&
								((itemstack.getItem() == Items.LAVA_BUCKET && PaperSpigotConfig.stackableLavaBuckets) ||
										(itemstack.getItem() == Items.WATER_BUCKET && PaperSpigotConfig.stackableWaterBuckets) ||
										(itemstack.getItem() == Items.MILK_BUCKET && PaperSpigotConfig.stackableMilkBuckets))) {
							if (action == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
								this.player.updateInventory(this.player.activeContainer);
							} else {
								this.player.playerConnection.sendPacket(new PacketPlayOutSetSlot(-1, -1, this.player.inventory.getCarried()));
								this.player.playerConnection.sendPacket(new PacketPlayOutSetSlot(this.player.activeContainer.windowId, packetplayinwindowclick.b(), this.player.activeContainer.getSlot(packetplayinwindowclick.b()).getItem()));
							}
						}
						break;
					case DENY:
						switch (action) {
							case PICKUP_ALL:
							case MOVE_TO_OTHER_INVENTORY:
							case HOTBAR_MOVE_AND_READD:
							case HOTBAR_SWAP:
							case COLLECT_TO_CURSOR:
							case UNKNOWN:
								this.player.updateInventory(this.player.activeContainer);
								break;
							case PICKUP_SOME:
							case PICKUP_HALF:
							case PICKUP_ONE:
							case PLACE_ALL:
							case PLACE_SOME:
							case PLACE_ONE:
							case SWAP_WITH_CURSOR:
								this.player.playerConnection.sendPacket(new PacketPlayOutSetSlot(-1, -1, this.player.inventory.getCarried()));
								this.player.playerConnection.sendPacket(new PacketPlayOutSetSlot(this.player.activeContainer.windowId, packetplayinwindowclick.b(), this.player.activeContainer.getSlot(packetplayinwindowclick.b()).getItem()));
								break;
							case DROP_ALL_SLOT:
							case DROP_ONE_SLOT:
								this.player.playerConnection.sendPacket(new PacketPlayOutSetSlot(this.player.activeContainer.windowId, packetplayinwindowclick.b(), this.player.activeContainer.getSlot(packetplayinwindowclick.b()).getItem()));
								break;
							case DROP_ALL_CURSOR:
							case DROP_ONE_CURSOR:
							case CLONE_STACK:
								this.player.playerConnection.sendPacket(new PacketPlayOutSetSlot(-1, -1, this.player.inventory.getCarried()));
								break;
							// Nothing
							case NOTHING:
								break;
						}
						return;
				}

				if (event instanceof CraftItemEvent) {
					player.updateInventory(player.activeContainer);
				}
			}

			if (ItemStack.matches(packetplayinwindowclick.e(), itemstack)) {
				this.player.playerConnection.sendPacket(new PacketPlayOutTransaction(packetplayinwindowclick.a(), packetplayinwindowclick.d(), true));
				this.player.g = true;
				this.player.activeContainer.b();
				this.player.broadcastCarriedItem();
				this.player.g = false;
			} else {
				this.n.a(this.player.activeContainer.windowId, packetplayinwindowclick.d());
				this.player.playerConnection.sendPacket(new PacketPlayOutTransaction(packetplayinwindowclick.a(), packetplayinwindowclick.d(), false));
				this.player.activeContainer.a(this.player, false);
				List<ItemStack> items = new ArrayList<>();

				for (int j = 0; j < this.player.activeContainer.c.size(); ++j) {
					items.add(this.player.activeContainer.c.get(j).getItem());
				}

				this.player.a(this.player.activeContainer, items);
			}
		}

	}

	public void a(PacketPlayInEnchantItem packetplayinenchantitem) {
		PlayerConnectionUtils.ensureMainThread(packetplayinenchantitem, this, this.player.u());

		this.player.resetIdleTimer();

		if (this.player.activeContainer.windowId == packetplayinenchantitem.a() && this.player.activeContainer.c(this.player) && !this.player.isSpectator()) {
			this.player.activeContainer.a(this.player, packetplayinenchantitem.b());
			this.player.activeContainer.b();
		}

	}

	public void a(PacketPlayInSetCreativeSlot packetplayinsetcreativeslot) {
		PlayerConnectionUtils.ensureMainThread(packetplayinsetcreativeslot, this, this.player.u());

		if (this.player.playerInteractManager.isCreative()) {
			boolean flag = packetplayinsetcreativeslot.a() < 0;
			ItemStack itemstack = packetplayinsetcreativeslot.getItemStack();

			if (itemstack != null && itemstack.hasTag() && itemstack.getTag().hasKeyOfType("BlockEntityTag", 10)) {
				NBTTagCompound nbttagcompound = itemstack.getTag().getCompound("BlockEntityTag");

				if (nbttagcompound.hasKey("x") && nbttagcompound.hasKey("y") && nbttagcompound.hasKey("z")) {
					BlockPosition blockposition = new BlockPosition(nbttagcompound.getInt("x"), nbttagcompound.getInt("y"), nbttagcompound.getInt("z"));
					TileEntity tileentity = this.player.world.getTileEntity(blockposition);

					if (tileentity != null) {
						NBTTagCompound nbt = new NBTTagCompound();

						tileentity.b(nbt);
						nbt.remove("x");
						nbt.remove("y");
						nbt.remove("z");
						itemstack.a("BlockEntityTag", nbt);
					}
				}
			}

			boolean flag1 = packetplayinsetcreativeslot.a() >= 1 && packetplayinsetcreativeslot.a() < 36 + PlayerInventory.getHotbarSize();

			boolean flag2 = itemstack == null || itemstack.getItem() != null && (!invalidItems.contains(Item.getId(itemstack.getItem())) || !org.spigotmc.SpigotConfig.filterCreativeItems); // Spigot
			boolean flag3 = itemstack == null || itemstack.getData() >= 0 && itemstack.count <= 64 && itemstack.count > 0;

			if (flag || (flag1 && !ItemStack.matches(this.player.defaultContainer.getSlot(packetplayinsetcreativeslot.a()).getItem(), packetplayinsetcreativeslot.getItemStack()))) { // Insist on valid slot

				org.bukkit.entity.HumanEntity player = this.player.getBukkitEntity();
				InventoryView inventory = new CraftInventoryView(player, player.getInventory(), this.player.defaultContainer);
				org.bukkit.inventory.ItemStack item = CraftItemStack.asBukkitCopy(packetplayinsetcreativeslot.getItemStack());

				SlotType type = SlotType.QUICKBAR;
				if (flag) {
					type = SlotType.OUTSIDE;
				} else if (packetplayinsetcreativeslot.a() < 36) {
					if (packetplayinsetcreativeslot.a() >= 5 && packetplayinsetcreativeslot.a() < 9) {
						type = SlotType.ARMOR;
					} else {
						type = SlotType.CONTAINER;
					}
				}
				InventoryCreativeEvent event = new InventoryCreativeEvent(inventory, type, flag ? -999 : packetplayinsetcreativeslot.a(), item);
				server.getPluginManager().callEvent(event);

				itemstack = CraftItemStack.asNMSCopy(event.getCursor());

				switch (event.getResult()) {
					case ALLOW:
						flag2 = flag3 = true;
						break;
					case DEFAULT:
						break;
					case DENY:
						if (packetplayinsetcreativeslot.a() >= 0) {
							this.player.playerConnection.sendPacket(new PacketPlayOutSetSlot(this.player.defaultContainer.windowId, packetplayinsetcreativeslot.a(), this.player.defaultContainer.getSlot(packetplayinsetcreativeslot.a()).getItem()));
							this.player.playerConnection.sendPacket(new PacketPlayOutSetSlot(-1, -1, null));
						}
						return;
				}
			}

			if (flag1 && flag2 && flag3) {
				if (itemstack == null) {
					this.player.defaultContainer.setItem(packetplayinsetcreativeslot.a(), null);
				} else {
					this.player.defaultContainer.setItem(packetplayinsetcreativeslot.a(), itemstack);
				}

				this.player.defaultContainer.a(this.player, true);
			} else if (flag && flag2 && flag3 && this.m < 200) {
				this.m += 20;
				EntityItem entityitem = this.player.drop(itemstack, true);

				if (entityitem != null) {
					entityitem.j();
				}
			}
		}

	}

	public void a(PacketPlayInTransaction packetplayintransaction) {
		if (this.player.dead) {
			return;
		}

		PlayerConnectionUtils.ensureMainThread(packetplayintransaction, this, this.player.u());

		Short s = this.n.get(this.player.activeContainer.windowId);

		if (s != null && packetplayintransaction.b() == s && this.player.activeContainer.windowId == packetplayintransaction.a() && !this.player.activeContainer.c(this.player) && !this.player.isSpectator()) {
			this.player.activeContainer.a(this.player, true);
		}

	}

	public void a(PacketPlayInUpdateSign updateSignPacket) {
		if (this.player.dead) {
			return;
		}

		PlayerConnectionUtils.ensureMainThread(updateSignPacket, this, this.player.u());

		this.player.resetIdleTimer();

		WorldServer worldserver = this.minecraftServer.getWorldServer(this.player.dimension);
		BlockPosition blockposition = updateSignPacket.a();

		if (worldserver.isLoaded(blockposition)) {
			TileEntity tileentity = worldserver.getTileEntity(blockposition);

			if (!(tileentity instanceof TileEntitySign)) {
				return;
			}

			TileEntitySign tileentitysign = (TileEntitySign) tileentity;

			if (!tileentitysign.b() || tileentitysign.c() != this.player) {
				this.minecraftServer.warning("Player " + this.player.getName() + " just tried to change non-editable sign");
				this.sendPacket(new PacketPlayOutUpdateSign(tileentity.world, updateSignPacket.a(), tileentitysign.lines));
				return;
			}

			IChatBaseComponent[] baseComponent = updateSignPacket.b();

			Player player = this.server.getPlayer(this.player);
			int x = updateSignPacket.a().getX();
			int y = updateSignPacket.a().getY();
			int z = updateSignPacket.a().getZ();
			String[] lines = new String[4];

			for (int i = 0; i < baseComponent.length; ++i) {
				lines[i] = EnumChatFormat.a(baseComponent[i].c());
			}

			SignChangeEvent event = new SignChangeEvent(player.getWorld().getBlockAt(x, y, z), this.server.getPlayer(this.player), lines);

			this.server.getPluginManager().callEvent(event);

			if (!event.isCancelled()) {
				System.arraycopy(org.bukkit.craftbukkit.block.CraftSign.sanitizeLines(event.getLines()), 0, tileentitysign.lines, 0, 4);
				tileentitysign.isEditable = false;
			}

			tileentitysign.update();
			worldserver.notify(blockposition);
		}

	}

	public void a(PacketPlayInKeepAlive packetplayinkeepalive) {
		if (packetplayinkeepalive.a() == this.i) {
			int i = (int) (this.d() - this.j);

			this.player.ping = (this.player.ping * 3 + i) / 4;
		}

	}

	private long d() {
		return System.nanoTime() / 1000000L;
	}

	public void a(PacketPlayInAbilities packetplayinabilities) {
		PlayerConnectionUtils.ensureMainThread(packetplayinabilities, this, this.player.u());

		if (this.player.abilities.canFly && this.player.abilities.isFlying != packetplayinabilities.isFlying()) {
			PlayerToggleFlightEvent event = new PlayerToggleFlightEvent(this.server.getPlayer(this.player), packetplayinabilities.isFlying());

			this.server.getPluginManager().callEvent(event);

			if (!event.isCancelled()) {
				this.player.abilities.isFlying = packetplayinabilities.isFlying();
			} else {
				this.player.updateAbilities();
			}
		}
	}

	public void a(PacketPlayInTabComplete packetplayintabcomplete) {
		PlayerConnectionUtils.ensureMainThread(packetplayintabcomplete, this, this.player.u());

		if (chatSpamField.addAndGet(this, 10) > 500 && !this.minecraftServer.getPlayerList().isOp(this.player.getProfile())) {
			this.disconnect("disconnect.spam");
			return;
		}

		ArrayList arraylist = Lists.newArrayList();
		Iterator iterator = this.minecraftServer.tabCompleteCommand(this.player, packetplayintabcomplete.a(), packetplayintabcomplete.b()).iterator();

		while (iterator.hasNext()) {
			String s = (String) iterator.next();

			arraylist.add(s);
		}

		this.player.playerConnection.sendPacket(new PacketPlayOutTabComplete((String[]) arraylist.toArray(new String[arraylist.size()])));
	}

	public void a(PacketPlayInSettings packetplayinsettings) {
		PlayerConnectionUtils.ensureMainThread(packetplayinsettings, this, this.player.u());
		this.player.a(packetplayinsettings);
	}

	public void a(PacketPlayInCustomPayload packetplayincustompayload) {
		PlayerConnectionUtils.ensureMainThread(packetplayincustompayload, this, this.player.u());
		PacketDataSerializer packetdataserializer;
		ItemStack itemstack;
		ItemStack itemstack1;

		try {
			if ("MC|BEdit".equals(packetplayincustompayload.a())) {
				packetdataserializer = new PacketDataSerializer(Unpooled.wrappedBuffer(packetplayincustompayload.b()));

				try {
					itemstack = packetdataserializer.i();
					if (itemstack == null) {
						return;
					}

					if (!ItemBookAndQuill.b(itemstack.getTag())) {
						throw new IOException("Invalid book tag!");
					}

					itemstack1 = this.player.inventory.getItemInHand();

					if (itemstack1 != null) {
						if (itemstack.getItem() == Items.WRITABLE_BOOK && itemstack.getItem() == itemstack1.getItem()) {
							itemstack1 = new ItemStack(Items.WRITABLE_BOOK); // CraftBukkit
							itemstack1.a("pages", itemstack.getTag().getList("pages", 8));
							CraftEventFactory.handleEditBookEvent(player, itemstack1);
						}

						return;
					}
				} catch (Exception exception) {
					PlayerConnection.c.error("Couldn\'t handle book info", exception);
					this.disconnect("Invalid book data!");
					return;
				} finally {
					packetdataserializer.release();
				}

				return;
			} else if ("MC|BSign".equals(packetplayincustompayload.a())) {
				packetdataserializer = new PacketDataSerializer(Unpooled.wrappedBuffer(packetplayincustompayload.b()));

				try {
					itemstack = packetdataserializer.i();
					if (itemstack == null) {
						return;
					}

					if (!ItemWrittenBook.b(itemstack.getTag())) {
						throw new IOException("Invalid book tag!");
					}

					itemstack1 = this.player.inventory.getItemInHand();

					if (itemstack1 != null) {
						if (itemstack.getItem() == Items.WRITTEN_BOOK && itemstack1.getItem() == Items.WRITABLE_BOOK) {
							itemstack1 = new ItemStack(Items.WRITTEN_BOOK);
							itemstack1.a("author", new NBTTagString(this.player.getName()));
							itemstack1.a("title", new NBTTagString(itemstack.getTag().getString("title")));
							itemstack1.a("pages", itemstack.getTag().getList("pages", 8));
							itemstack1.setItem(Items.WRITTEN_BOOK);
							CraftEventFactory.handleEditBookEvent(player, itemstack1);
						}

						return;
					}
				} catch (Exception exception1) {
					PlayerConnection.c.error("Couldn\'t sign book", exception1);
					this.disconnect("Invalid book data!"); // CraftBukkit
					return;
				} finally {
					packetdataserializer.release();
				}

				return;
			} else if ("MC|TrSel".equals(packetplayincustompayload.a())) {
				try {
					int i = packetplayincustompayload.b().readInt();
					Container container = this.player.activeContainer;

					if (container instanceof ContainerMerchant) {
						((ContainerMerchant) container).d(i);
					}
				} catch (Exception exception2) {
					PlayerConnection.c.error("Couldn\'t select trade", exception2);
					this.disconnect("Invalid trade data!");
				}
			} else if ("MC|AdvCdm".equals(packetplayincustompayload.a())) {
				if (!this.minecraftServer.getEnableCommandBlock()) {
					this.player.sendMessage(new ChatMessage("advMode.notEnabled", new Object[0]));
				} else if (this.player.getBukkitEntity().isOp() && this.player.abilities.canInstantlyBuild) {
					packetdataserializer = packetplayincustompayload.b();

					try {
						byte b0 = packetdataserializer.readByte();
						CommandBlockListenerAbstract commandblocklistenerabstract = null;

						if (b0 == 0) {
							TileEntity tileentity = this.player.world.getTileEntity(new BlockPosition(packetdataserializer.readInt(), packetdataserializer.readInt(), packetdataserializer.readInt()));

							if (tileentity instanceof TileEntityCommand) {
								commandblocklistenerabstract = ((TileEntityCommand) tileentity).getCommandBlock();
							}
						} else if (b0 == 1) {
							Entity entity = this.player.world.a(packetdataserializer.readInt());

							if (entity instanceof EntityMinecartCommandBlock) {
								commandblocklistenerabstract = ((EntityMinecartCommandBlock) entity).getCommandBlock();
							}
						}

						String s = packetdataserializer.c(packetdataserializer.readableBytes());
						boolean flag = packetdataserializer.readBoolean();

						if (commandblocklistenerabstract != null) {
							commandblocklistenerabstract.setCommand(s);
							commandblocklistenerabstract.a(flag);
							if (!flag) {
								commandblocklistenerabstract.b((IChatBaseComponent) null);
							}

							commandblocklistenerabstract.h();
							this.player.sendMessage(new ChatMessage("advMode.setCommand.success", new Object[]{s}));
						}
					} catch (Exception exception3) {
						PlayerConnection.c.error("Couldn\'t set command block", exception3);
						this.disconnect("Invalid CommandBlock data!");
					} finally {
						packetdataserializer.release();
					}
				} else {
					this.player.sendMessage(new ChatMessage("advMode.notAllowed", new Object[0]));
				}
			} else if ("MC|Beacon".equals(packetplayincustompayload.a())) {
				if (this.player.activeContainer instanceof ContainerBeacon) {
					try {
						packetdataserializer = packetplayincustompayload.b();
						int j = packetdataserializer.readInt();
						int k = packetdataserializer.readInt();
						ContainerBeacon containerbeacon = (ContainerBeacon) this.player.activeContainer;
						Slot slot = containerbeacon.getSlot(0);

						if (slot.hasItem()) {
							slot.a(1);
							IInventory iinventory = containerbeacon.e();

							iinventory.b(1, j);
							iinventory.b(2, k);
							iinventory.update();
						}
					} catch (Exception exception4) {
						PlayerConnection.c.error("Couldn\'t set beacon", exception4);
						this.disconnect("Invalid beacon data!");
					}
				}
			} else if ("MC|ItemName".equals(packetplayincustompayload.a()) && this.player.activeContainer instanceof ContainerAnvil) {
				ContainerAnvil containeranvil = (ContainerAnvil) this.player.activeContainer;

				if (packetplayincustompayload.b() != null && packetplayincustompayload.b().readableBytes() >= 1) {
					String s1 = SharedConstants.a(packetplayincustompayload.b().c(32767));

					if (s1.length() <= 30) {
						containeranvil.a(s1);
					}
				} else {
					containeranvil.a("");
				}
			} else if (packetplayincustompayload.a().equals("REGISTER")) {
				String channels = packetplayincustompayload.b().toString(com.google.common.base.Charsets.UTF_8);
				for (String channel : channels.split("\0")) {
					getPlayer().addChannel(channel);
				}
			} else if (packetplayincustompayload.a().equals("UNREGISTER")) {
				String channels = packetplayincustompayload.b().toString(com.google.common.base.Charsets.UTF_8);
				for (String channel : channels.split("\0")) {
					getPlayer().removeChannel(channel);
				}
			} else {
				byte[] data = new byte[packetplayincustompayload.b().readableBytes()];
				packetplayincustompayload.b().readBytes(data);
				server.getMessenger().dispatchIncomingMessage(player.getBukkitEntity(), packetplayincustompayload.a(), data);
			}
		} finally {
			if (packetplayincustompayload.b().refCnt() > 0) {
				packetplayincustompayload.b().release();
			}
		}
		// CraftBukkit end
	}

	// CraftBukkit start - Add "isDisconnected" method
	public boolean isDisconnected() { // Spigot
		return !this.player.joining && !this.networkManager.channel.config().isAutoRead();
	}

	static class SyntheticClass_1 {

		static final int[] a;
		static final int[] b;
		static final int[] c = new int[PacketPlayInClientCommand.EnumClientCommand.values().length];

		static {
			try {
				PlayerConnection.SyntheticClass_1.c[PacketPlayInClientCommand.EnumClientCommand.PERFORM_RESPAWN.ordinal()] = 1;
			} catch (NoSuchFieldError nosuchfielderror) {
				;
			}

			try {
				PlayerConnection.SyntheticClass_1.c[PacketPlayInClientCommand.EnumClientCommand.REQUEST_STATS.ordinal()] = 2;
			} catch (NoSuchFieldError nosuchfielderror1) {
				;
			}

			try {
				PlayerConnection.SyntheticClass_1.c[PacketPlayInClientCommand.EnumClientCommand.OPEN_INVENTORY_ACHIEVEMENT.ordinal()] = 3;
			} catch (NoSuchFieldError nosuchfielderror2) {
				;
			}

			b = new int[PacketPlayInEntityAction.EnumPlayerAction.values().length];

			try {
				PlayerConnection.SyntheticClass_1.b[PacketPlayInEntityAction.EnumPlayerAction.START_SNEAKING.ordinal()] = 1;
			} catch (NoSuchFieldError nosuchfielderror3) {
				;
			}

			try {
				PlayerConnection.SyntheticClass_1.b[PacketPlayInEntityAction.EnumPlayerAction.STOP_SNEAKING.ordinal()] = 2;
			} catch (NoSuchFieldError nosuchfielderror4) {
				;
			}

			try {
				PlayerConnection.SyntheticClass_1.b[PacketPlayInEntityAction.EnumPlayerAction.START_SPRINTING.ordinal()] = 3;
			} catch (NoSuchFieldError nosuchfielderror5) {
				;
			}

			try {
				PlayerConnection.SyntheticClass_1.b[PacketPlayInEntityAction.EnumPlayerAction.STOP_SPRINTING.ordinal()] = 4;
			} catch (NoSuchFieldError nosuchfielderror6) {
				;
			}

			try {
				PlayerConnection.SyntheticClass_1.b[PacketPlayInEntityAction.EnumPlayerAction.STOP_SLEEPING.ordinal()] = 5;
			} catch (NoSuchFieldError nosuchfielderror7) {
				;
			}

			try {
				PlayerConnection.SyntheticClass_1.b[PacketPlayInEntityAction.EnumPlayerAction.RIDING_JUMP.ordinal()] = 6;
			} catch (NoSuchFieldError nosuchfielderror8) {
				;
			}

			try {
				PlayerConnection.SyntheticClass_1.b[PacketPlayInEntityAction.EnumPlayerAction.OPEN_INVENTORY.ordinal()] = 7;
			} catch (NoSuchFieldError nosuchfielderror9) {
				;
			}

			a = new int[PacketPlayInBlockDig.EnumPlayerDigType.values().length];

			try {
				PlayerConnection.SyntheticClass_1.a[PacketPlayInBlockDig.EnumPlayerDigType.DROP_ITEM.ordinal()] = 1;
			} catch (NoSuchFieldError nosuchfielderror10) {
				;
			}

			try {
				PlayerConnection.SyntheticClass_1.a[PacketPlayInBlockDig.EnumPlayerDigType.DROP_ALL_ITEMS.ordinal()] = 2;
			} catch (NoSuchFieldError nosuchfielderror11) {
				;
			}

			try {
				PlayerConnection.SyntheticClass_1.a[PacketPlayInBlockDig.EnumPlayerDigType.RELEASE_USE_ITEM.ordinal()] = 3;
			} catch (NoSuchFieldError nosuchfielderror12) {
				;
			}

			try {
				PlayerConnection.SyntheticClass_1.a[PacketPlayInBlockDig.EnumPlayerDigType.START_DESTROY_BLOCK.ordinal()] = 4;
			} catch (NoSuchFieldError nosuchfielderror13) {
				;
			}

			try {
				PlayerConnection.SyntheticClass_1.a[PacketPlayInBlockDig.EnumPlayerDigType.ABORT_DESTROY_BLOCK.ordinal()] = 5;
			} catch (NoSuchFieldError nosuchfielderror14) {
				;
			}

			try {
				PlayerConnection.SyntheticClass_1.a[PacketPlayInBlockDig.EnumPlayerDigType.STOP_DESTROY_BLOCK.ordinal()] = 6;
			} catch (NoSuchFieldError nosuchfielderror15) {
				;
			}

		}
	}

}
