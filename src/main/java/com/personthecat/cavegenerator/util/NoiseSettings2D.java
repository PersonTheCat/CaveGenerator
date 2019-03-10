package com.personthecat.cavegenerator.util;

import fastnoise.FastNoise;

/**
 * Contains all of the generic info related to spawning noise-
 * based features in the world. This is applicable for 2D noise
 * only.
 * With most of these parameters now being merged directly into
 * FastNoise, this class should soon be obsolete. The only holdup
 * is that it facilitates retrieving values from a preset in that
 * it contains only the values we need.
 */
public class NoiseSettings2D {
    public final float scale;
    public final float frequency;
    public final int min, max;

    /** Where @param scale is a value between 0.0 and 1.0. */
    public NoiseSettings2D(float frequency, float scale, int min, int max) {
        this.frequency = frequency;
        this.scale = scale;
        this.min = min;
        this.max = max;
    }

    public FastNoise getGenerator(int seed) {
        return new FastNoise(seed)
            .SetNoiseType(FastNoise.NoiseType.SimplexFractal)
            .SetScale(scale)
            .SetFrequency(frequency)
            .SetRange(min, max)
            .SetInterp(FastNoise.Interp.Hermite);
    }
}