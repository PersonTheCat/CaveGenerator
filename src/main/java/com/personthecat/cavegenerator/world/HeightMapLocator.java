package com.personthecat.cavegenerator.world;

import net.minecraft.block.state.IBlockState;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkPrimer;

import java.util.Arrays;

import static com.personthecat.cavegenerator.util.CommonMethods.*;

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
public class HeightMapLocator {
    /**
     * The relative distance to be checked around each previous y coordinate.
     * Should always be greater than 1.
     */
    private static final int RELATIVE_DISTANCE = 5;

    /** Quickly determines the full height map for the input ChunkPrimer. */
    public static int[][] getHeightFromPrimer(ChunkPrimer primer) {
        int[][] map = new int[16][16];
        int previousHeight = getHeightFromBottom(primer, 0, 0);
        for (int x = 0; x < 16; x = x + 2) {
            fillTwoRows(primer, map, previousHeight, x);
        }
        // printMap(map);
        return map;
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
        final IBlockState state = primer.getBlockState(x, y, z);
        return state.isOpaqueCube();
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

    /** A debug function used to display the heightMap found. */
    private static void printMap(int[][] map) {
        for (int x = 0; x < map.length; x++) {
            info(Arrays.toString(map[x]));
        }
        info(""); // New line
    }
}