package net.minecraft.server;

import java.io.IOException;

public class PacketPlayOutEntityTeleport implements Packet<PacketListenerPlayOut> {
    private int a;
    private int b;
    private int c;
    private int d;
    private byte e;
    private byte f;
    private boolean g;

    public PacketPlayOutEntityTeleport() {
    }

    public PacketPlayOutEntityTeleport(Entity var1) {
        this.a = var1.getId();
        this.b = MathHelper.floor(var1.locX * 32.0D);
        this.c = MathHelper.floor(var1.locY * 32.0D);
        this.d = MathHelper.floor(var1.locZ * 32.0D);
        this.e = (byte) ((int) (var1.yaw * 256.0F / 360.0F));
        this.f = (byte) ((int) (var1.pitch * 256.0F / 360.0F));
        this.g = var1.onGround;
    }

    public PacketPlayOutEntityTeleport(int var1, int var2, int var3, int var4, byte var5, byte var6, boolean var7) {
        this.a = var1;
        this.b = var2;
        this.c = var3;
        this.d = var4;
        this.e = var5;
        this.f = var6;
        this.g = var7;
    }

    public void a(PacketDataSerializer var1) throws IOException {
        this.a = var1.e();
        this.b = var1.readInt();
        this.c = var1.readInt();
        this.d = var1.readInt();
        this.e = var1.readByte();
        this.f = var1.readByte();
        this.g = var1.readBoolean();
    }

    public void b(PacketDataSerializer var1) throws IOException {
        var1.b(this.a);
        var1.writeInt(this.b);
        var1.writeInt(this.c);
        var1.writeInt(this.d);
        var1.writeByte(this.e);
        var1.writeByte(this.f);
        var1.writeBoolean(this.g);
    }

    public void a(PacketListenerPlayOut var1) {
        var1.a(this);
    }

    public int getA() {
        return this.a;
    }

    public int getB() {
        return this.b;
    }

    public int getC() {
        return this.c;
    }

    public int getD() {
        return this.d;
    }

    public byte getE() {
        return this.e;
    }

    public byte getF() {
        return this.f;
    }

    public void setB(int b) {
        this.b = b;
    }

    public void setC(int c) {
        this.c = c;
    }

    public void setD(int d) {
        this.d = d;
    }
}
