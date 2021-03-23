package com.personthecat.cavegenerator.world.generator;

import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkPrimer;

import java.util.Random;

public class PrimerContext {
    final int[][] heightmap;
    final World world;
    final Random rand;
    final int destChunkX;
    final int destChunkZ;
    final int chunkX;
    final int chunkZ;
    final int offsetX;
    final int offsetZ;
    final ChunkPrimer primer;

    public PrimerContext(
        int[][] heightmap,
        World world,
        Random rand,
        int destChunkX,
        int destChunkZ,
        int chunkX,
        int chunkZ,
        ChunkPrimer primer
    ) {
        this.heightmap = heightmap;
        this.world = world;
        this.rand = rand;
        this.destChunkX = destChunkX;
        this.destChunkZ = destChunkZ;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.offsetX = chunkX * 16;
        this.offsetZ = chunkZ * 16;
        this.primer = primer;
    }
}
