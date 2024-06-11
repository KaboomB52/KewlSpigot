package net.minecraft.server;

import java.io.IOException;

public class PacketPlayOutAttachEntity implements Packet<PacketListenerPlayOut> {
	private int a;
	private int b;
	private int c;

	public PacketPlayOutAttachEntity() {
	}

	public PacketPlayOutAttachEntity(int var1, Entity var2, Entity var3) {
		this.a = var1;
		this.b = var2.getId();
		this.c = var3 != null ? var3.getId() : -1;
	}

	public void a(PacketDataSerializer var1) throws IOException {
		this.b = var1.readInt();
		this.c = var1.readInt();
		this.a = var1.readUnsignedByte();
	}

	public void b(PacketDataSerializer var1) throws IOException {
		var1.writeInt(this.b);
		var1.writeInt(this.c);
		var1.writeByte(this.a);
	}

	public void a(PacketListenerPlayOut var1) {
		var1.a(this);
	}
}
