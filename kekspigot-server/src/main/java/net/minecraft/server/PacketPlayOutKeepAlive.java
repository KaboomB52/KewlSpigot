package net.minecraft.server;

import java.io.IOException;

public class PacketPlayOutKeepAlive implements Packet<PacketListenerPlayOut> {
    private int a;

    public PacketPlayOutKeepAlive() {
    }

    public PacketPlayOutKeepAlive(int var1) {
        this.a = var1;
    }

    public void a(PacketListenerPlayOut var1) {
        var1.a(this);
    }

    public void a(PacketDataSerializer var1) throws IOException {
        this.a = var1.e();
    }

    public void b(PacketDataSerializer var1) throws IOException {
        var1.b(this.a);
    }

    public int getA() {
        return a;
    }
}
