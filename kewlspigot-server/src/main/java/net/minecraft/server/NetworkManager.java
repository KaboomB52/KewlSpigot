package net.minecraft.server;

import io.netty.util.concurrent.AbstractEventExecutor;
import org.eytril.spigot.KewlSpigot;
import org.eytril.spigot.handler.PacketHandler;
import com.google.common.collect.Queues;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.local.LocalChannel;
import io.netty.channel.local.LocalEventLoopGroup;
import io.netty.channel.local.LocalServerChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.timeout.TimeoutException;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import java.net.SocketAddress;
import java.util.Queue;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.crypto.SecretKey;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

public class NetworkManager extends SimpleChannelInboundHandler<Packet> {

    private static final Logger g = LogManager.getLogger();
    public static final Marker a = MarkerManager.getMarker("NETWORK");
    public static final Marker b = MarkerManager.getMarker("NETWORK_PACKETS", NetworkManager.a);
    public static final AttributeKey<EnumProtocol> c = AttributeKey.valueOf("protocol");

    public static final LazyInitVar<NioEventLoopGroup> d = new LazyInitVar<NioEventLoopGroup>() {
        @Override
        protected NioEventLoopGroup init() {
            return new NioEventLoopGroup(0, (new ThreadFactoryBuilder()).setNameFormat("Netty Client IO #%d").setDaemon(true).build());
        }
    };
    public static final LazyInitVar<EpollEventLoopGroup> e = new LazyInitVar<EpollEventLoopGroup>() {
        @Override
        protected EpollEventLoopGroup init() {
            return new EpollEventLoopGroup(0, (new ThreadFactoryBuilder()).setNameFormat("Netty Epoll Client IO #%d").setDaemon(true).build());
        }
    };
    public static final LazyInitVar<LocalEventLoopGroup> f = new LazyInitVar<LocalEventLoopGroup>() {
        @Override
        protected LocalEventLoopGroup init() {
            return new LocalEventLoopGroup(0, (new ThreadFactoryBuilder()).setNameFormat("Netty Local Client IO #%d").setDaemon(true).build());
        }
    };

    private final EnumProtocolDirection h;
    private final Queue<NetworkManager.QueuedPacket> packetQueue = Queues.newConcurrentLinkedQueue();
    private final ReentrantReadWriteLock j = new ReentrantReadWriteLock();
    public Channel channel;
    // Spigot Start // PAIL
    public SocketAddress l;
    public java.util.UUID spoofedUUID;
    public com.mojang.authlib.properties.Property[] spoofedProfile;
    public boolean preparing = true;
    // Spigot End
    private PacketListener m;
    private IChatBaseComponent n;
    private boolean o;
    private boolean p;

    private boolean openedBook;

    public static Channel getChannel(final NetworkManager nm) {
        return nm.channel;
    }

    public NetworkManager(EnumProtocolDirection enumprotocoldirection) {
        this.h = enumprotocoldirection;
    }

    public void channelActive(ChannelHandlerContext channelhandlercontext) throws Exception {
        super.channelActive(channelhandlercontext);
        this.channel = channelhandlercontext.channel();
        this.l = this.channel.remoteAddress();
        // Spigot Start
        this.preparing = false;
        // Spigot End

        try {
            this.a(EnumProtocol.HANDSHAKING);
        } catch (Throwable throwable) {
            NetworkManager.g.fatal(throwable);
        }

    }

    public void a(EnumProtocol enumprotocol) {
        this.channel.attr(NetworkManager.c).set(enumprotocol);
        this.channel.config().setAutoRead(true);
        NetworkManager.g.debug("Enabled auto read");
    }

    public void channelInactive(ChannelHandlerContext channelhandlercontext) throws Exception {
        this.close(new ChatMessage("disconnect.endOfStream", new Object[0]));
    }

    public void exceptionCaught(ChannelHandlerContext channelhandlercontext, Throwable throwable) throws Exception {
        ChatMessage chatmessage;

        if (throwable instanceof TimeoutException) {
            chatmessage = new ChatMessage("disconnect.timeout", new Object[0]);
        } else {
            chatmessage = new ChatMessage("disconnect.genericReason", new Object[] { "Internal Exception: " + throwable});
        }

        this.close(chatmessage);
        if (MinecraftServer.getServer().isDebugging()) throwable.printStackTrace(); // Spigot
    }

    protected void a(ChannelHandlerContext channelhandlercontext, Packet packet) throws Exception {
        if (this.channel.isOpen()) {
	        if (packet instanceof PacketPlayInCustomPayload) {
	            PacketPlayInCustomPayload payload = (PacketPlayInCustomPayload) packet;
	            String name = payload.a();

                if (name.equalsIgnoreCase("MC|BSign") || name.equalsIgnoreCase("MC|BEdit")) {
                    if (this.m instanceof PlayerConnection) {
                        byte[] data = payload.b().array();

                        if(data.length > 15000){
                            this.close(new ChatMessage("Invalid book packet"));
                            return;
                        }
                        if (!this.openedBook) {
                            this.close(new ChatMessage("Invalid book packet"));
                            return;
                        }
                        this.openedBook = false;
                    }
                }
            } else if (packet instanceof PacketPlayInBlockPlace) {
	            ItemStack stack = ((PacketPlayInBlockPlace) packet).getItemStack();
	            if (stack != null && stack.getItem() != null && stack.getItem().getName().equalsIgnoreCase("item.writingBook")) {
	                this.openedBook = true;
                }
	        }
            try {
                packet.a(this.m);
            } catch (CancelledPacketHandleException cancelledpackethandleexception) {
                ;
            }
            if (this.m instanceof PlayerConnection) {
                try {
                    for (PacketHandler handler : KewlSpigot.INSTANCE.getPacketHandlers()) {
                        handler.handleReceivedPacket((PlayerConnection) this.m, packet);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

    }

    public void a(PacketListener packetlistener) {
        Validate.notNull(packetlistener, "packetListener");
        NetworkManager.g.debug("Set listener of {} to {}", this, packetlistener);
        this.m = packetlistener;
    }

    public void handle(Packet packet) {
        if (this.g()) {
            this.m();
            this.a(packet, null);
        } else {
            this.j.writeLock().lock();

            try {
                this.packetQueue.add(new NetworkManager.QueuedPacket(packet));
            } finally {
                this.j.writeLock().unlock();
            }
        }

    }

    public void a(Packet packet, GenericFutureListener<? extends Future<? super Void>> genericfuturelistener, GenericFutureListener<? extends Future<? super Void>>... agenericfuturelistener) {
        if (this.g()) {
            this.m();
            this.a(packet, (GenericFutureListener[]) ArrayUtils.add(agenericfuturelistener, 0, genericfuturelistener));
        } else {
            this.j.writeLock().lock();

            try {
                this.packetQueue.add(new NetworkManager.QueuedPacket(packet, (GenericFutureListener[]) ArrayUtils.add(agenericfuturelistener, 0, genericfuturelistener)));
            } finally {
                this.j.writeLock().unlock();
            }
        }

    }

    private void a(Packet packet, GenericFutureListener<? extends Future<? super Void>>[] listeners) {
        final EnumProtocol packetProtocol = EnumProtocol.a(packet);
        final EnumProtocol channelProtocol = this.channel.attr(NetworkManager.c).get();

        if (channelProtocol != packetProtocol) {
            NetworkManager.g.debug("Disabled auto read");
            this.channel.config().setAutoRead(false);
        }

        if (this.channel.eventLoop().inEventLoop()) {
            if (packetProtocol != channelProtocol) {
                this.a(packetProtocol);
            }

            ChannelFuture channelfuture = NetworkManager.this.channel.writeAndFlush(packet);

            if (listeners != null) {
                channelfuture.addListeners(listeners);
            }

            channelfuture.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
        } else {
            this.channel.eventLoop().execute((AbstractEventExecutor.LazyRunnable) () -> {
                    if (packetProtocol != channelProtocol) {
                        NetworkManager.this.a(packetProtocol);
                    }

                    ChannelFuture channelfuture = NetworkManager.this.channel.writeAndFlush(packet);

                    if (listeners != null) {
                        channelfuture.addListeners(listeners);
                    }

                    channelfuture.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
            });
        }

    }

    private void m() {
        if (this.channel != null && this.channel.isOpen()) {
            this.j.readLock().lock();

            try {
                while (!this.packetQueue.isEmpty()) {
                    NetworkManager.QueuedPacket queuedPacket = this.packetQueue.poll();

                    this.a(queuedPacket.a, queuedPacket.b);
                }
            } finally {
                this.j.readLock().unlock();
            }

        }
    }

    public void a() {
        this.m();

        if (this.m instanceof IUpdatePlayerListBox) {
            ((IUpdatePlayerListBox) this.m).c();
        }

        this.channel.flush();
    }

    public SocketAddress getSocketAddress() {
        return this.l;
    }

    public void close(IChatBaseComponent ichatbasecomponent) {
        this.preparing = false;
        this.packetQueue.clear(); // MinetickMod

        if (this.channel.isOpen()) {
            this.channel.close();
            this.n = ichatbasecomponent;
        }

    }

    public boolean c() {
        return this.channel instanceof LocalChannel || this.channel instanceof LocalServerChannel;
    }

    public void a(SecretKey secretkey) {
        this.o = true;
        this.channel.pipeline().addBefore("splitter", "decrypt", new PacketDecrypter(MinecraftEncryption.a(2, secretkey)));
        this.channel.pipeline().addBefore("prepender", "encrypt", new PacketEncrypter(MinecraftEncryption.a(1, secretkey)));
    }

    public boolean g() {
        return this.channel != null && this.channel.isOpen();
    }

    public boolean h() {
        return this.channel == null;
    }

    public PacketListener getPacketListener() {
        return this.m;
    }

    public IChatBaseComponent j() {
        return this.n;
    }

    public void k() {
        this.channel.config().setAutoRead(false);
    }

    public void a(int i) {
        if (i >= 0) {
            if (this.channel.pipeline().get("decompress") instanceof PacketDecompressor) {
                ((PacketDecompressor) this.channel.pipeline().get("decompress")).a(i);
            } else {
                this.channel.pipeline().addBefore("decoder", "decompress", new PacketDecompressor(i));
            }

            if (this.channel.pipeline().get("compress") instanceof PacketCompressor) {
                ((PacketCompressor) this.channel.pipeline().get("decompress")).a(i);
            } else {
                this.channel.pipeline().addBefore("encoder", "compress", new PacketCompressor(i));
            }
        } else {
            if (this.channel.pipeline().get("decompress") instanceof PacketDecompressor) {
                this.channel.pipeline().remove("decompress");
            }

            if (this.channel.pipeline().get("compress") instanceof PacketCompressor) {
                this.channel.pipeline().remove("compress");
            }
        }

    }

    public void l() {
        if (this.channel != null && !this.channel.isOpen()) {
            if (!this.p) {
                this.p = true;
                if (this.j() != null) {
                    this.getPacketListener().a(this.j());
                } else if (this.getPacketListener() != null) {
                    this.getPacketListener().a(new ChatComponentText("Disconnected"));
                }
                this.packetQueue.clear(); // Free up packet queue.
            } else {
                NetworkManager.g.warn("handleDisconnection() called twice");
            }

        }
    }

    public EnumProtocol getProtocol() {
        return this.channel.attr(NetworkManager.c).get();
    }

    protected void channelRead0(ChannelHandlerContext channelhandlercontext, Packet object) throws Exception {
        this.a(channelhandlercontext, object);
    }

    static class QueuedPacket {

        private final Packet a;
        private final GenericFutureListener<? extends Future<? super Void>>[] b;

        public QueuedPacket(Packet packet, GenericFutureListener<? extends Future<? super Void>>... agenericfuturelistener) {
            this.a = packet;
            this.b = agenericfuturelistener;
        }
    }

    public SocketAddress getRawAddress() {
        return this.channel.remoteAddress();
    }

}
