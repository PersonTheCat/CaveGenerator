package com.personthecat.cavegenerator.world;

import fastnoise.FastNoise;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import org.apache.commons.lang3.ArrayUtils;

import javax.annotation.concurrent.Immutable;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static com.personthecat.cavegenerator.util.CommonMethods.*;

/**
 * For neatly interacting with tunnel sections. Each tunnel
 * section is essentially a sphere that is constrained by
 * the boundaries of the current chunk @ original(X/Z).
 */
@Immutable
public class TunnelSectionInfo {
    /** The exact purpose of some of these values is still unclear. */
    private final double centerX, centerY, centerZ;
    private final double radiusXZ, radiusY;
    private final int chunkX, chunkZ;
    private final int startX, endX;
    private final int startY, endY;
    private final int startZ, endZ;

    /** Stores all valid positions to avoid redundant calculations. */
    private BlockPos[] positions;

    /** Temporary noise generator */
    private final FastNoise noise = new FastNoise().SetFrequency(0.001f).SetRange(-5f, 5f).SetNoiseType(FastNoise.NoiseType.SimplexFractal);

    public TunnelSectionInfo(double x, double y, double z, double radiusXZ, double radiusY, int originalX, int originalZ) {
        this.centerX = x;
        this.centerY = y;
        this.centerZ = z;
        this.radiusXZ = radiusXZ;
        this.radiusY = radiusY;
        this.chunkX = originalX;
        this.chunkZ = originalZ;
        // Always stay within the chunk bounds.
        this.startX = applyLimitXZ(MathHelper.floor(x - radiusXZ) - originalX * 16 - 1);
        this.endX = applyLimitXZ(MathHelper.floor(x + radiusXZ) - originalX * 16 + 1);
        this.startY = applyLimitY(MathHelper.floor(y - radiusY) - 1);
        this.endY = applyLimitY(MathHelper.floor(y + radiusY) + 1);
        this.startZ = applyLimitXZ(MathHelper.floor(z - radiusXZ) - originalZ * 16 - 1);
        this.endZ = applyLimitXZ(MathHelper.floor(z + radiusXZ) - originalZ * 16 + 1);
        // Setup the array to the maximum possible size;
        initializePositions();
    }

    /** Pre-calculates positions and stores them into an array. */
    public void calculate() {
        // Monitor the index for pushing values to the array;
        int index = 0;
        for (int x = startX; x < endX; x++) {
            // (Relative coordinate, centered, offset) / radius?
            final double distX = ((x + chunkX * 16) + 0.5 - centerX) / radiusXZ;
            final double distX2 = distX * distX;
            for (int z = startZ; z < endZ; z++) {
                final double distZ = ((z + chunkZ * 16) + 0.5 - centerZ) / radiusXZ;
                final double distZ2 = distZ * distZ;
                // To-do: Confirm that this is necessary. Might just improve performance.
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
    }

    /** Variant of calculate() that uses the random values from MapGenRavine. */
    public void calculateMutated(float[] mut) {
        int index = 0;
        for (int x = startX; x < endX; x++) {
            final double distX = ((x + chunkX * 16) + 0.5 - centerX) / radiusXZ;
            final double distX2 = distX * distX;
            for (int z = startZ; z < endZ; z++) {
                final double distZ = ((z + chunkZ * 16) + 0.5 - centerZ) / radiusXZ;
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
    }

    /** Sets the positions array to be the maximum possible size for this section. */
    private void initializePositions() {
        int maxPossibleSize = (endX - startX) * (endY - startY) * (endZ - startZ);
        positions = new BlockPos[maxPossibleSize];
    }

    /** Creates a slice? of the array `positions`, from 0 to @param size. */
    private void shrinkPositionsToSize(int size) {
        positions = ArrayUtils.subarray(positions, 0, size);
    }

    /** Finds the end of the array and shrinks it to that size. */
    private void shrinkPositionsToSize() {
        int firstNullIndex = firstNull(positions, 0, positions.length - 1);
        positions = ArrayUtils.subarray(positions, 0, firstNullIndex);
    }

    /** A modified binary search algorithm that finds a boundary between null / not. */
    // No longer needed.
    private int firstNull(BlockPos[] values, int start, int end) {
        if (end > start + 1 && values[0] != null) {
            // The index at the middle of this range.
            final int middle = (start + end) / 2;
            // Whether the current and previous values are null.
            final boolean nullAt = values[middle] == null;
            final boolean nullBefore = values[middle - 1] == null;
            // Found it.
            if (nullAt && !nullBefore) {
                return middle;
            }
            // If we're currently null, then the last
            // non-null value is to the left.
            if (nullAt) {
                return firstNull(values, start, middle - 1);
            }
            // Otherwise, the last non-null value is
            // to the right.
            return firstNull(values, middle + 1, end);
        }
        return 0;
    }

    public int getHighestY() {
        return endY;
    }

    /** Tests each valid position in the section for one single `true`. */
    public boolean test(Predicate<BlockPos> func) {
        // Verify that the positions are correctly
        // initialized before proceeding.
        nullCheck();
        // Test each value.
        for (int i = 0; i < positions.length; i++) {
            if (func.test(positions[i])) {
                return true;
            }
        }
        return false;
    }

    /** Runs a function for every valid position in the section. */
    public void run(Consumer<BlockPos> func) {
        // Verify that the positions are correctly
        // initialized before proceeding.
        nullCheck();
        // Accept each value.
        for (int i = 0; i < positions.length; i++) {
            func.accept(positions[i]);
        }
    }

    /** Determines whether the input coordinates would exist in this section. */
    public boolean testCoords(int x, int y, int z) {
        final double distX = ((x + chunkX * 16) + 0.5 - centerX) / radiusXZ;
        final double distX2 = distX * distX;
        final double distZ = ((z + chunkZ * 16) + 0.5 - centerZ) / radiusXZ;
        final double distZ2 = distZ * distZ;

        if ((distX2 + distZ2) >= 1.0) {
            return false;
        }

        final double distY = ((y - 1) + 0.5 - centerY) / radiusY;
        final double distY2 = distY * distY;

        return (distY > -0.7) && ((distX2 + distY2 + distZ2) < 1.0);
    }

    public boolean testCoords(int x, int y, int z, float[] mut) {
        final double distX = ((x + chunkX * 16) + 0.5 - centerX) / radiusXZ;
        final double distX2 = distX * distX;
        final double distZ = ((z + chunkZ * 16) + 0.5 - centerZ) / radiusXZ;
        final double distZ2 = distZ * distZ;

        if ((distX2 + distZ2) >= 1.0) {
            return false;
        }

        final double distY = ((y - 1) + 0.5 - centerY) / radiusY;
        final double distY2 = distY * distY;

        return (distX2 + distZ2) * (double) mut[y - 1] + distY2 / 6.0 < 1.0;
    }

    private void nullCheck() {
        if (positions.length > 0 && positions[0] == null) {
            throw runEx("Error: Forgot to call calculate() on a tunnel section.");
        }
    }

    /** Makes sure the resulting value stays within chunk bounds. */
    private static int applyLimitXZ(int xz) {
        return xz < 0 ? 0 : xz > 16 ? 16 : xz;
    }

    /** Makes sure the resulting value stays between y = 1 & y = 248 */
    private static int applyLimitY(int y) {
        return y < 1 ? 1 : y > 248 ? 248 : y;
    }
}