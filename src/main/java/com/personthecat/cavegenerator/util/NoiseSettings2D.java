package com.personthecat.cavegenerator.util;

import fastnoise.FastNoise;

/**
 * Contains all of the generic info related to spawning noise-
 * based features in the world. This is applicable for 2D noise
 * only.
 */
public class NoiseSettings2D {
    public final float selectionThreshold; // Calculated from scale.
    public final float frequency;
    public final int min, max;

    /** Where @param scale is a value between 0.0 and 1.0. */
    public NoiseSettings2D(float frequency, float scale, int min, int max) {
        // Convert scale into a range from -1.0 to +1.0.
        this.selectionThreshold = (scale * 2.0f) - 1.0f;
        this.frequency = frequency;
        this.min = min;
        this.max = max;
    }

    /** Converts the selection threshold into its original value. */
    public float getScale() {
        return (selectionThreshold + 1.0f) / 2.0f;
    }

    public FastNoise getGenerator(int seed) {
        return new FastNoise(seed)
            .SetNoiseType(FastNoise.NoiseType.SimplexFractal)
            .SetFrequency(frequency)
            .SetRange(min, max)
            .SetInterp(FastNoise.Interp.Hermite);
    }
}