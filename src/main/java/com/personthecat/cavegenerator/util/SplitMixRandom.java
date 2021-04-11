package com.personthecat.cavegenerator.util;

import it.unimi.dsi.fastutil.HashCommon;

/**
 * Implementation of SplitMixRandom borrowed from Sodium, which itself is borrowed from DSI utilities.
 * This class is provided in Cave Generator as an experiment to improve performance with some
 * decorators that heavily rely on RNG.
 *
 * The original design of this utility is documented here: http://xoshiro.di.unimi.it/
 */
public class SplitMixRandom {
    private static final long PHI = 0x9E3779B97F4A7C15L;

    private long x;

    public SplitMixRandom(final long seed) {
        this.setSeed(seed);
    }

    private static long staffordMix13(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    private static int staffordMix4Upper32(long z) {
        z = (z ^ (z >>> 33)) * 0x62A9D9ED799705F5L;
        return (int) (((z ^ (z >>> 28)) * 0xCB24D0A5C88C35B3L) >>> 32);
    }

    public long nextLong() {
        return staffordMix13(this.x += PHI);
    }

    public int nextInt() {
        return staffordMix4Upper32(this.x += PHI);
    }

    public int nextInt(final int n) {
        return (int) this.nextLong(n);
    }

    public long nextLong(final long n) {
        if (n <= 0) {
            throw new IllegalArgumentException("illegal bound " + n + " (must be positive)");
        }
        long t = staffordMix13(this.x += PHI);
        final long nMinus1 = n - 1;
        if ((n & nMinus1) == 0) {
            return t & nMinus1;
        }
        long u = t >>> 1;
        while (u + nMinus1 - (t = u % n) < 0) {
            u = staffordMix13(this.x += PHI) >>> 1;
        }
        return t;
    }

    public void setSeed(final long seed) {
        this.x = HashCommon.murmurHash3(seed);
    }

    public void setState(final long state) {
        this.x = state;
    }
}
