package com.personthecat.cavegenerator.util;

import java.util.Random;

/**
 * Implementation of XoRoShiRo128 borrowed from Sodium, which itself is borrowed from DSI utilities.
 * This class is provided in Cave Generator as an experiment to improve performance with some
 * decorators that heavily rely on RNG.
 *
 * The original design of this utility is documented here: http://xoshiro.di.unimi.it/
 */
public class XoRoShiRo extends Random {

    private SplitMixRandom mixer;
    private long seed = Long.MIN_VALUE;
    private long p0, p1; // The initialization words for the current seed
    private long s0, s1; // The current random words
    private boolean hasSavedState; // True if we can be quickly reseed by using resetting the words

    public XoRoShiRo(final long seed) {
        this.setSeed(seed);
    }

    @Override
    public long nextLong() {
        final long s0 = this.s0;
        long s1 = this.s1;
        final long result = s0 + s1;
        s1 ^= s0;

        this.s0 = Long.rotateLeft(s0, 24) ^ s1 ^ s1 << 16;
        this.s1 = Long.rotateLeft(s1, 37);
        return result;
    }

    @Override
    public int nextInt() {
        return (int) this.nextLong();
    }

    @Override
    public int nextInt(final int n) {
        return (int) this.nextLong(n);
    }

    private long nextLong(final long n) {
        if (n <= 0) {
            throw new IllegalArgumentException("illegal bound " + n + " (must be positive)");
        }
        long t = this.nextLong();
        final long nMinus1 = n - 1;
        // Shortcut for powers of two--high bits
        if ((n & nMinus1) == 0) {
            return (t >>> Long.numberOfLeadingZeros(nMinus1)) & nMinus1;
        }
        // Rejection-based algorithm to get uniform integers in the general case
        long u = t >>> 1;
        while (u + nMinus1 - (t = u % n) < 0) {
            u = this.nextLong() >>> 1;
        }
        return t;
    }

    @Override
    public double nextDouble() {
        return Double.longBitsToDouble(0x3FFL << 52 | this.nextLong() >>> 12) - 1.0;
    }

    @Override
    public float nextFloat() {
        return (this.nextLong() >>> 40) * 0x1.0p-24f;
    }

    @Override
    public boolean nextBoolean() {
        return this.nextLong() < 0;
    }

    @Override
    public void nextBytes(final byte[] bytes) {
        int i = bytes.length, n;
        while (i != 0) {
            n = Math.min(i, 8);
            for (long bits = this.nextLong(); n-- != 0; bits >>= 8) {
                bytes[--i] = (byte) bits;
            }
        }
    }

    @Override
    public void setSeed(final long seed) {
        // Restore the previous initial state if the seed hasn't changed
        // Setting and mixing the seed is expensive, so this saves some CPU cycles
        if (this.hasSavedState && this.seed == seed) {
            this.s0 = this.p0;
            this.s1 = this.p1;
        } else {
            // Avoid allocations of SplitMixRandom
            if (this.mixer == null) {
                this.mixer = new SplitMixRandom(seed);
            } else {
                this.mixer.setSeed(seed);
            }

            this.s0 = this.mixer.nextLong();
            this.s1 = this.mixer.nextLong();
            this.p0 = this.s0;
            this.p1 = this.s1;
            this.seed = seed;
            this.hasSavedState = true;
        }
    }
}
