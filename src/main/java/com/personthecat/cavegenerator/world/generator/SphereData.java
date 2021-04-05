package com.personthecat.cavegenerator.world.generator;

import com.personthecat.cavegenerator.util.PositionConsumer;
import com.personthecat.cavegenerator.util.PositionPredicate;

import java.util.Arrays;

/**
 * A reusable list of <em>relative</em> block positions stored in an array of raw integers.
 * This class is designed to reduce the number of memory allocations that occur when generating
 * spheres in a world, while simultaneously allowing generated spheres to be reused several
 * times each.
 */
public class SphereData {

    /** Based on the size and offset of these data in raw memory. */
    private static final int Z_MASK = (1 << 4) - 1;
    private static final int Y_MASK = (1 << 8) - 1;

    /** A buffer to avoid some unnecessary grow operations. */
    private static final int BUFFER_SIZE = 64;

    private int[] positions = new int[256];
    private int index = 0;

    /**
     * Checks to see if the data can hold the given volume. If not, grows. These radii represent
     * the size of a sphere in the current chunk. They do not represent the entire area of a
     * sphere and, as a result, we are not calculating the volume of a sphere, but something more
     * like a rectangular prism.
     *
     * In order to ensure that no data are written out of bounds, this should be called before
     * each sphere is generated.
     *
     * @param radX The width of this segment on the x-axis, max 15.
     * @param radY The height of this segment, max 255.
     * @param radZ The width of this segment on the z-axis, max 15.
     */
    public void grow(int radX, int radY, int radZ) {
        final int volume = radX * radY * radZ;
        final int len = positions.length;
        if (volume > len) {
            final int[] data = this.positions;
            this.positions = new int[volume + BUFFER_SIZE];
            System.arraycopy(data, 0, positions, 0, len);
        }
    }

    /**
     * Appends a new block position into the data.
     *
     * Callers must be sure that x, y, and z are within chunk bounds.
     *
     * @param x The x-coordinate being stored.
     * @param y The y-coordinate being stored.
     * @param z The z-coordinate being stored.
     */
    public void add(int x, int y, int z) {
        positions[index++] = x << 12 | z << 8 | y;
    }

    /**
     * Completes an operation for each position currently stored in the data.
     *
     * @param f Instructions for what to do when given 3 coordinates.
     */
    public void forEach(PositionConsumer f) {
        for (int i = 0; i < index; i++) {
            final int data = positions[i];
            f.accept(data >> 12, data & Y_MASK, data >> 8 & Z_MASK);
        }
    }

    /**
     * Runs a test on each of the positions in the array.
     *
     * @param predicate A condition to test at each position.
     * @return Whether any predicate returns true.
     */
    public boolean anyMatches(PositionPredicate predicate) {
        for (int i = 0; i < index; i++) {
            final int data = positions[i];
            if (predicate.test(data >> 12, data & Y_MASK, data >> 8 & Z_MASK)) {
                return true;
            }
        }
        return false;
    }

    /** Clears all data from the array and resets the cursor to 0. */
    public void reset() {
        Arrays.fill(positions, 0, this.index, 0); // This avoids GC performance cost.
        this.index = 0;
    }
}
