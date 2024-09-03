package net.minecraft.server;

import java.io.IOException;

public class PacketPlayOutMultiBlockChange implements Packet<PacketListenerPlayOut> {

    private ChunkCoordIntPair a;
    private MultiBlockChangeInfo[] b;

    public PacketPlayOutMultiBlockChange() {

    }

    public PacketPlayOutMultiBlockChange(int paramInt, short[] paramArrayOfShort, Chunk paramChunk) {
        this.a = new ChunkCoordIntPair(paramChunk.locX, paramChunk.locZ);

        this.b = new MultiBlockChangeInfo[paramInt];
        for (byte b1 = 0; b1 < this.b.length; b1++) {
            this.b[b1] = new MultiBlockChangeInfo(this, paramArrayOfShort[b1], paramChunk);
        }
    }

    public void a(PacketDataSerializer paramPacketDataSerializer) throws IOException {
        this.a = new ChunkCoordIntPair(paramPacketDataSerializer.readInt(), paramPacketDataSerializer.readInt());
        this.b = new MultiBlockChangeInfo[paramPacketDataSerializer.e()];

        for (byte b1 = 0; b1 < this.b.length; b1++) {
            this.b[b1] = new MultiBlockChangeInfo(this, paramPacketDataSerializer.readShort(), Block.d.a(paramPacketDataSerializer.e()));
        }
    }

    public void b(PacketDataSerializer paramPacketDataSerializer) throws IOException {
        paramPacketDataSerializer.writeInt(this.a.x);
        paramPacketDataSerializer.writeInt(this.a.z);
        paramPacketDataSerializer.b(this.b.length);
        for (MultiBlockChangeInfo multiBlockChangeInfo : this.b) {
            paramPacketDataSerializer.writeShort(multiBlockChangeInfo.b());
            paramPacketDataSerializer.b(Block.d.b(multiBlockChangeInfo.c()));
        }
    }

    public void setChanges(MultiBlockChangeInfo[] b) {
        this.b = b;
    }

    public void a(PacketListenerPlayOut paramPacketListenerPlayOut) {
        paramPacketListenerPlayOut.a(this);
    }

    public class MultiBlockChangeInfo {

        public final short b;
        public IBlockData c;

        public MultiBlockChangeInfo(PacketPlayOutMultiBlockChange this$0, short param1Short, IBlockData param1IBlockData) {
            this.b = param1Short;
            this.c = param1IBlockData;
        }

        public MultiBlockChangeInfo(PacketPlayOutMultiBlockChange this$0, short param1Short, Chunk param1Chunk) {
            this.b = param1Short;
            this.c = param1Chunk.getBlockData(a());
        }


        public BlockPosition a() {
            return new BlockPosition(PacketPlayOutMultiBlockChange.this.a.a(this.b >> 12 & 0xF, this.b & 0xFF, this.b >> 8 & 0xF));
        }

        public short b() {
            return this.b;
        }

        public IBlockData c() {
            return this.c;
        }
    }
}
