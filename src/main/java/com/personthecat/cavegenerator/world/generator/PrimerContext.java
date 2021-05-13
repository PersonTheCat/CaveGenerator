package com.personthecat.cavegenerator.world.generator;

import com.personthecat.cavegenerator.util.XoRoShiRo;
import com.personthecat.cavegenerator.world.BiomeSearch;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkPrimer;

import java.util.Random;

public class PrimerContext {
    final BiomeSearch biomes;
    final int[][] heightmap;
    final World world;
    final Random localRand;
    final int chunkX;
    final int chunkZ;
    final int offsetX;
    final int offsetZ;
    final ChunkPrimer primer;

    public PrimerContext(
        BiomeSearch biomes,
        int[][] heightmap,
        World world,
        int chunkX,
        int chunkZ,
        ChunkPrimer primer
    ) {
        this.biomes = biomes;
        this.heightmap = heightmap;
        this.world = world;
        this.localRand = new XoRoShiRo(world.getSeed() ^ chunkX ^ chunkZ);
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.offsetX = chunkX * 16;
        this.offsetZ = chunkZ * 16;
        this.primer = primer;
    }
}
