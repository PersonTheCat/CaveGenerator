package com.personthecat.cavegenerator.world;

/**
 * Generates noise quickly. Useful when shape isn't so important.
 * Thanks to FastNoise for a very similar algorithm.
 */
public class HashGenerator {

    private static final int X_MULTIPLE = 0x653;  // 1619
    private static final int Y_MULTIPLE = 0x7A69; // 31337
    private static final int Z_MULTIPLE = 0x1B3B; // 6971

    private static final long GENERAL_MULTIPLE = 0x5DEECE66DL; // 25214903917
    private static final long ADDEND = 0xBL;                   // 11
    private static final long MASK = 0xFFFFFFFFFFFFL;          // 281474976710656
    private static final long SCALE = 0x16345785D8A0000L;      // E18

    /** The scrambled seed to use for hash generation. */
    private final long seed;

    public HashGenerator(long seed) {
        this.seed = scramble(seed);
    }

    /** Similar to Random's scramble method. */
    private static long scramble(long seed) {
        long newseed = (seed ^ GENERAL_MULTIPLE) & MASK;
        return (newseed * GENERAL_MULTIPLE + ADDEND) & MASK;
    }

    public double getHash(int x, int y, int z) {
        // Clone the seed to allow for reuse.
        long hash = seed;

        // Mask the value using x, y, and z.
        hash ^= x * X_MULTIPLE;
        hash ^= y * Y_MULTIPLE;
        hash ^= z * Z_MULTIPLE;

        // Scale it up.
        hash *= hash;

        return ((hash >> 13) ^ hash) / (double) SCALE;
    }
}