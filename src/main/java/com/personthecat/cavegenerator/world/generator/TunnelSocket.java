package com.personthecat.cavegenerator.world.generator;

import java.util.Random;

public interface TunnelSocket {
    int CANNOT_SPAWN = Integer.MIN_VALUE;
    int getTunnelHeight(int[][] heightmap, Random rand, int x, int z, int chunkX, int chunkZ);
}
