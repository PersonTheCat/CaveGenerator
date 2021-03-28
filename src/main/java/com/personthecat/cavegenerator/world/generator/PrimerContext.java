package com.personthecat.cavegenerator.world.generator;

import com.personthecat.cavegenerator.world.BiomeSearch;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkPrimer;

import java.util.Random;

public class PrimerContext {
    final BiomeSearch biomes;
    final int[][] heightmap;
    final World world;
    final Random rand;
    final int chunkX;
    final int chunkZ;
    final int offsetX;
    final int offsetZ;
    final ChunkPrimer primer;

    public PrimerContext(
        BiomeSearch biomes,
        int[][] heightmap,
        World world,
        Random rand,
        int chunkX,
        int chunkZ,
        ChunkPrimer primer
    ) {
        this.biomes = biomes;
        this.heightmap = heightmap;
        this.world = world;
        this.rand = rand;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.offsetX = chunkX * 16;
        this.offsetZ = chunkZ * 16;
        this.primer = primer;
    }
}
