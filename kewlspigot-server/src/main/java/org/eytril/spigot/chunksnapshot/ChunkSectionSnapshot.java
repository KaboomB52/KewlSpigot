package org.eytril.spigot.chunksnapshot;

import net.minecraft.server.NibbleArray;

public class ChunkSectionSnapshot {

    private final int nonEmptyBlockCount;
    private final int tickingBlockCount;
    private final char[] blockIds;
    private final NibbleArray emittedLight;
    private final NibbleArray skyLight;

    public ChunkSectionSnapshot(
            int nonEmptyBlockCount,
            int tickingBlockCount,
            char[] blockIds,
            NibbleArray emittedLight,
            NibbleArray skyLight
    ) {
        this.nonEmptyBlockCount = nonEmptyBlockCount;
        this.tickingBlockCount = tickingBlockCount;
        this.blockIds = blockIds;
        this.emittedLight = emittedLight;
        this.skyLight = skyLight;
    }

    public final int getNonEmptyBlockCount() {
        return nonEmptyBlockCount;
    }

    public final int getTickingBlockCount() {
        return tickingBlockCount;
    }

    public final char[] getBlockIds() {
        return blockIds;
    }

    public final NibbleArray getEmittedLight() {
        return emittedLight;
    }

    public final NibbleArray getSkyLight() {
        return skyLight;
    }

}
