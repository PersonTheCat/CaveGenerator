package com.personthecat.cavegenerator.model;

import net.minecraft.world.chunk.ChunkPrimer;

/**
 * A DTO used for consolidating positional data when generating
 * world features via MapGenBase objects.
 */
public class PrimerData {
    public final ChunkPrimer p;
    public final int chunkX, chunkZ;
    public final int centerX, centerZ;

    public PrimerData(ChunkPrimer p, int chunkX, int chunkZ) {
        this.p = p;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.centerX = chunkX * 16 + 8;
        this.centerZ = chunkZ * 16 + 8;
    }
}