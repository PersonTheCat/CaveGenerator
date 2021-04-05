package com.personthecat.cavegenerator.model;

import net.minecraft.world.chunk.ChunkPrimer;

/**
 * A DTO used for consolidating positional data when generating
 * world features via MapGenBase objects.
 */
public class PrimerData {
    public final ChunkPrimer p;
    public final int chunkX, chunkZ;
    public final int absX, absZ;
    public final int centerX, centerZ;

    public PrimerData(ChunkPrimer p, int chunkX, int chunkZ) {
        this.p = p;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.absX = chunkX * 16;
        this.absZ = chunkZ * 16;
        this.centerX = this.absX + 8;
        this.centerZ = this.absZ + 8;
    }
}