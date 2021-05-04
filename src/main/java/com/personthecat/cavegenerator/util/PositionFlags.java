package com.personthecat.cavegenerator.util;

/**
 * A reusable list of <em>relative</em> block positions stored in an array of raw integers.
 * This class is designed to reduce the number of memory allocations that occur when generating
 * spheres in a world, while simultaneously allowing generated spheres to be reused several
 * times each.
 */
public class PositionFlags {

    /** Based on the size and offset of these data in raw memory. */
    private static final int Z_MASK = (1 << 4) - 1;
    private static final int Y_MASK = (1 << 8) - 1;

    /** A buffer to avoid some unnecessary grow operations. */
    private static final int BUFFER_SIZE = 64;

    private int[] positions;
    private int index;

    public PositionFlags(int capacity) {
        this.positions = new int[capacity];
        this.index = 0;
    }

    /**
     * Checks to see if the data can hold the given volume. If not, grows.
     *
     * In order to ensure that no data are written out of bounds, this should be called before
     * each sphere is generated.
     */
    public void grow(int volume) {
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
     * Completes an operation for each position currently stored in the data.
     * When the process has completed, only the conditions which passed will
     * remain in the array.
     *
     * @param predicate A condition to test at each position.
     */
    public void filter(PositionPredicate predicate) {
        final int end = index;
        index = 0;
        for (int i = 0; i < end; i++) {
            final int data = positions[i];
            final int x = data >> 12;
            final int y = data & Y_MASK;
            final int z = data >> 8 & Z_MASK;
            if (predicate.test(x, y, z)) {
                positions[index++] = data;
            }
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

    /** Resets the cursor to 0. No need to overwrite values. */
    public void reset() {
        this.index = 0;
    }
}
