package com.personthecat.cavegenerator.world;

import lombok.extern.log4j.Log4j2;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkPrimer;

import java.util.Arrays;

/**
 *   This class is used to quickly determine the maximum height at every
 * (x, z) coordinate in any given chunk, excluding water blocks. It is
 * necessary for two reasons:
 *  * In 1.12, there are no accessible fields containing the current
 *    heightmap for each chunk when it only exists in the form of a
 *    ChunkPrimer.
 *  * At later stages of chunk generation, the coordinate reported by
 *    {@link World#getHeight} includes water blocks and not just the
 *    original terrain height.
 *
 *    It is unclear whether this class will provide any substantial
 *  performance gains when used with ChunkPrimers; however, due to the
 *  way blocks are stored and retrieved from regular Chunks, its usefulness
 *  in that context may justify its existence.
 */
@Log4j2
public class HeightMapLocator {

    /**
     * The relative distance to be checked around each previous y coordinate.
     * Used when searching ChunkPrimers only. Should always be greater than 1.
     */
    private static final int RELATIVE_DISTANCE = 5;

    /**
     * The number of coordinates to skip when searching for land below water.
     * Higher numbers may slightly increase performance, but decrease accuracy.
     * The result is conservative in that the height found will always be at or
     * below the actual surface, such that structures can never spawn above it.
     */
    private static final int NUM_TO_SKIP = 5;

    /** A convenient reference to water. */
    private static final IBlockState BLK_WATER = Blocks.WATER.getDefaultState();

    /** The height to be used when filling the substitute height map below. */
    public static final int FAUX_MAP_HEIGHT = 255;

    /** A substitute height map with all values at the level specified above. */
    public static final int[][] FAUX_MAP = getFauxMap();

    /** Quickly determines the full heightmap for the input ChunkPrimer. */
    public static int[][] getHeightFromPrimer(ChunkPrimer primer) {
        int[][] map = new int[16][16];
        int previousHeight = getHeightFromBottom(primer, 0, 0);
        for (int x = 0; x < 16; x = x + 2) {
            previousHeight = fillTwoRows(primer, map, previousHeight, x);
        }
        // printMap(map);
        return map;
    }

    /**
     * Retrieves and modifies the heightmap stored inside of `Chunk` to
     * ignore the height of water.
     */
    public static int[][] getHeightFromChunk(World world, Chunk chunk) {
        int[][] map = new int[16][16];
        final int[] original = chunk.getHeightMap();
        final int seaLevel = world.getSeaLevel();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                final int originalY = original[z << 4 | x];
                if (originalY == seaLevel) {
                    // We're at the sea level, which might imply that water has
                    // been filled up to this point.
                    map[x][z] = getHeight(chunk, x, z, seaLevel - 1);
                } else {
                    map[x][z] = originalY;
                }
            }
        }
        // printMap(map);
        return map;
    }

    /**
     * Generates a single, unified heightmap based on data from the
     * intersection of four chunks, according to an offset of 8 on
     * each axis. The index used for each coordinate is not based on
     * its global or absolute position, but its relative position
     * according to its original chunk. This means that, so long as
     * the input coordinate is offset by 8, the correct height value
     * can be still retrieved if it is converted to a chunk coordinate.
     */
    public static int[][] getHeightFromWorld(World world, int chunkX, int chunkZ) {
        int[][] map = new int[16][16];
        fillMapCorner(map, world, chunkX, chunkZ, 8, 15, 8, 15);
        fillMapCorner(map, world, chunkX + 1, chunkZ, 0, 7, 8, 15);
        fillMapCorner(map, world, chunkX, chunkZ + 1, 8, 15, 0, 7);
        fillMapCorner(map, world, chunkX + 1, chunkZ + 1, 0, 7, 0, 7);
        // printMap(map);
        return map;
    }

    /** Fills in a quarter of the height map using data from its corresponding chunk. */
    private static void fillMapCorner(int[][] map, World world, int chunkX, int chunkZ, int minX, int maxX, int minZ, int maxZ) {
        final Chunk chunk = world.getChunk(chunkX, chunkZ);
        final int seaLevel = world.getSeaLevel();
        final int[] original = chunk.getHeightMap();
        for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                final int originalY = original[z << 4 | x];
                if (originalY == seaLevel) {
                    // We're at the sea level, which might imply that water has
                    // been filled up to this point.
                    map[x][z] = getHeight(chunk, x, z, seaLevel - 1);
                } else {
                    map[x][z] = originalY;
                }
            }
        }
    }

    /** Converts an absolute coordinate to a chunk coordinate. */
    private static int toRelative(int absolute) {
        return absolute & 15;
    }

    /**
     * Scans every y coordinate to determine the boundary between solid / air.
     * This is possible because no air or otherwise non-solid blocks have been
     * generated underground at this time.
     */
    private static int getHeightFromBottom(ChunkPrimer primer, int x, int z) {
        boolean previouslySolid = isSolid(primer, x, 0, z);
        for (int y = 1; y < 256; y++) {
            final boolean currentlySolid = isSolid(primer, x, y, z);
            // We're at a boundary between solid and air / water
            if (previouslySolid && !currentlySolid) {
                // Return the highest solid block, which is below the current pos.
                return y - 1;
            }
            // Reset
            previouslySolid = currentlySolid;
        }
        // Nothing was found. Default to 63.
        return 63;
    }

    /** Determines the maximum height by scanning around the adjacent y coordinate. */
    private static int getHeightFromRelative(ChunkPrimer primer, int x, int z, int previousHeight) {
        boolean previouslySolid = isSolid(primer, x, previousHeight, z);
        if (previouslySolid) { // In ground -> search up.
            for (int y = previousHeight + 1; y < previousHeight + RELATIVE_DISTANCE; y++) {
                final boolean currentlySolid = isSolid(primer, x, y, z);
                if (previouslySolid && !currentlySolid) {
                    return y - 1;
                }
                previouslySolid = currentlySolid;
            }
        } else { // In air -> search down.
            for (int y = previousHeight - 1; y > previousHeight - RELATIVE_DISTANCE; y--) {
                final boolean currentlySolid = isSolid(primer, x, y, z);
                if (!previouslySolid && currentlySolid) {
                    return y;
                }
                previouslySolid = currentlySolid;
            }
        }
        // Nothing was found. Start over.
        return getHeightFromBottom(primer, x, z);
    }

    /** Determines whether the IBlockState at the input coordinates is an opaque cube. */
    private static boolean isSolid(ChunkPrimer primer, int x, int y, int z) {
        return primer.getBlockState(x, y, z).isOpaqueCube();
    }

    /**
     * Iterates from 0 to 15 @ x = startX, and then from 15 to 0 @ x = startX + 1.
     * This ensures that `previousHeight` was always calculated from the adjacent
     * block. Returns `previousHeight` when finished.
     */
    private static int fillTwoRows(ChunkPrimer primer, int[][] map, int previousHeight, int startX) {
        for (int z = 0; z < 16; z++) {
            previousHeight = getHeightFromRelative(primer, startX, z, previousHeight);
            map[startX][z] = previousHeight;
        }
        for (int z = 15; z > -1; z--) {
            previousHeight = getHeightFromRelative(primer, startX + 1, z, previousHeight);
            map[startX + 1][z] = previousHeight;
        }
        return previousHeight;
    }

    /**
     * Locates the first non-water block starting at sea level and iterating down.
     * In 1.12, the surface will always be at or above this point. The ground is
     * searched upward until another water block is found, indicating that the
     * surface has been breached.
     */
    private static int getHeight(Chunk chunk, int x, int z, int seaLevel) {
        // Start at the top; Go down; Don't skip below y = 0;
        for (int y = seaLevel; y > NUM_TO_SKIP; y = y - NUM_TO_SKIP) {
            // If we've found anything but water, we assume to be below the surface.
            if (!chunk.getBlockState(x, y, z).equals(BLK_WATER)) {
                return findWaterAbove(chunk, x, y, z) - 1;
            }
        }
        return seaLevel;
    }

    private static int findWaterAbove(Chunk chunk, int x, int y, int z) {
        for (int h = y; h < y + NUM_TO_SKIP; h++) {
            if (chunk.getBlockState(x, h, z).equals(BLK_WATER)) {
                return h;
            }
        }
        return y;
    }

    /** Generates a substitute height map where all values equal 64; */
    private static int[][] getFauxMap() {
        final int[][] map = new int[16][16];
        for (int[] x : map) {
            Arrays.fill(x, FAUX_MAP_HEIGHT);
        }
        return map;
    }

    /** A debug function used to display the heightMap found. */
    private static void printMap(int[][] map) {
        for (int[] row : map) {
            log.info(Arrays.toString(row));
        }
        log.info(""); // New line
    }
}