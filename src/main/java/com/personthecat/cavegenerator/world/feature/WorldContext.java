package com.personthecat.cavegenerator.world.feature;

import com.personthecat.cavegenerator.world.GeneratorController;
import net.minecraft.world.World;

import java.util.Random;

public class WorldContext {
    final int[][] heightmap;
    final GeneratorController gen;
    final Random rand;
    final int chunkX, chunkZ, offsetX, offsetZ;
    final World world;

    public WorldContext(
        int[][] heightmap,
        GeneratorController gen,
        Random rand,
        int chunkX,
        int chunkZ,
        World world
    ) {
        this.heightmap = heightmap;
        this.gen = gen;
        this.rand = rand;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.offsetX = chunkX * 16 + 8;
        this.offsetZ = chunkZ * 16 + 8;
        this.world = world;
    }
}
