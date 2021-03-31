package com.personthecat.cavegenerator.world.generator;

import com.personthecat.cavegenerator.model.PrimerData;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.util.TriConsumer;

import java.util.function.Predicate;

/**
 * For neatly interacting with tunnel sections. Each tunnel section is essentially
 * a sphere that is constrained by the boundaries of the current chunk @ original(X/Z).
 */
public class TunnelSectionInfo {

    /** The exact purpose of some of these values is still unclear. */
    private final double centerX, centerY, centerZ;
    private final double radiusXZ, radiusY;
    private final int absX, absZ;
    private final int startX, endX;
    private final int startY, endY;
    private final int startZ, endZ;

    /** Stores all valid positions to avoid redundant calculations. */
    private BlockPos[] positions;

    public TunnelSectionInfo(PrimerData data, TunnelPathInfo path, double radiusXZ, double radiusY) {
        this(data, path.getX(), path.getY(), path.getZ(), radiusXZ, radiusY);
    }

    public TunnelSectionInfo(PrimerData data, double x, double y, double z, double radiusXZ, double radiusY) {
        this.centerX = x;
        this.centerY = y;
        this.centerZ = z;
        this.radiusXZ = radiusXZ;
        this.radiusY = radiusY;
        this.absX = data.chunkX * 16;
        this.absZ = data.chunkZ * 16;
        // Always stay within the chunk bounds.
        this.startX = limitXZ(MathHelper.floor(centerX - radiusXZ) - absX - 1);
        this.endX = limitXZ(MathHelper.floor(centerX + radiusXZ) - absX + 1);
        this.startY = limitY(MathHelper.floor(centerY - radiusY) - 1);
        this.endY = limitY(MathHelper.floor(centerY + radiusY) + 1);
        this.startZ = limitXZ(MathHelper.floor(centerZ - radiusXZ) - absZ - 1);
        this.endZ = limitXZ(MathHelper.floor(centerZ + radiusXZ) - absZ + 1);
        // Setup the array to the maximum possible size;
        int maxPossibleSize = (endX - startX) * (endY - startY) * (endZ - startZ);
        positions = new BlockPos[maxPossibleSize];
    }

    /** Pre-calculates positions and stores them into an array. */
    public TunnelSectionInfo calculate() {
        // Monitor the index for pushing values to the array;
        int index = 0;
        for (int x = startX; x < endX; x++) {
            // (Relative coordinate, centered, offset) / radius?
            final double distX = ((x + absX) + 0.5 - centerX) / radiusXZ;
            final double distX2 = distX * distX;
            for (int z = startZ; z < endZ; z++) {
                final double distZ = ((z + absZ) + 0.5 - centerZ) / radiusXZ;
                final double distZ2 = distZ * distZ;
                if ((distX2 + distZ2) >= 1.0) {
                    continue;
                }
                for (int y = endY; y > startY; y--) {
                    final double distY = ((y - 1) + 0.5 - centerY) / radiusY;
                    final double distY2 = distY * distY;
                    if ((distY > -0.7) && ((distX2 + distY2 + distZ2) < 1.0)) {
                        positions[index] = new BlockPos(x, y, z);
                        index ++;
                    }
                }
            }
        }
        shrinkPositionsToSize(index);
        return this;
    }

    /** Variant of calculate() that uses the random values from MapGenRavine. */
    public TunnelSectionInfo calculateMutated(float[] mut) {
        int index = 0;
        for (int x = startX; x < endX; x++) {
            final double distX = ((x + absX) + 0.5 - centerX) / radiusXZ;
            final double distX2 = distX * distX;
            for (int z = startZ; z < endZ; z++) {
                final double distZ = ((z + absZ) + 0.5 - centerZ) / radiusXZ;
                final double distZ2 = distZ * distZ;
                if (distX2 + distZ2 >= 1.0) {
                    continue;
                }
                for (int y = endY; y > startY; y--) {
                    final double distY = ((y - 1) + 0.5 - centerY) / radiusY;
                    final double distY2 = distY * distY;
                    if ((distX2 + distZ2) * (double) mut[y - 1] + distY2 / 6.0 < 1.0) {
                        positions[index] = new BlockPos(x, y, z);
                        index++;
                    }
                }
            }
        }
        shrinkPositionsToSize(index);
        return this;
    }

    /** Creates a slice of the array `positions`, from 0 to @param size. */
    private void shrinkPositionsToSize(int size) {
        positions = ArrayUtils.subarray(positions, 0, size);
    }

    public int getLowestY() {
        return startY;
    }

    public int getHighestY() {
        return endY;
    }

    /** Tests each valid position in the section for one single `true`. */
    public boolean test(Predicate<BlockPos> func) {
        for (BlockPos pos : positions) {
            if (func.test(pos)) {
                return true;
            }
        }
        return false;
    }

    /** Runs a function for every valid position in the section. */
    public void run(TriConsumer<Integer, Integer, Integer> func) {
        for (BlockPos pos : positions) {
            func.accept(pos.getX(), pos.getY(), pos.getZ());
        }
    }

    /** Makes sure the resulting value stays within chunk bounds. */
    private static int limitXZ(int xz) {
        return xz < 0 ? 0 : xz > 16 ? 16 : xz;
    }

    /** Makes sure the resulting value stays between y = 1 & y = 248 */
    private static int limitY(int y) {
        return y < 1 ? 1 : y > 248 ? 248 : y;
    }
}