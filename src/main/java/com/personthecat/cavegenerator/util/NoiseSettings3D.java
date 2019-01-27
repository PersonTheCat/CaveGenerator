package com.personthecat.cavegenerator.util;

import fastnoise.FastNoise;

/**
 * Variant of NoiseSettings2D
 */
public class NoiseSettings3D {
    private final float selectionThreshold; // Calculated from scale.
    private final float frequency;
    private final float scaleY;
    private final int octaves;
    private final FastNoise.NoiseType noiseType;

    /** Where @param scale is a value between 0.0 and 1.0. */
    public NoiseSettings3D(float frequency, float scale, float scaleY, int octaves, FastNoise.NoiseType noiseType) {
        // Convert scale into a range from -1.0 to +1.0.
        this.selectionThreshold = (scale * 2.0f) - 1.0f;
        this.frequency = frequency;
        this.scaleY = scaleY;
        this.octaves = octaves;
        this.noiseType = noiseType;
    }

    /** Variant of the primary constructor with a default value for noiseType. */
    public NoiseSettings3D(float frequency, float scale, float scaleY, int octaves) {
        this(frequency, scale, scaleY, octaves, FastNoise.NoiseType.SimplexFractal);
    }

    public float getSelectionThreshold() {
        return selectionThreshold;
    }

    public float getFrequency() {
        return frequency;
    }

    public float getScale() {
        return (selectionThreshold + 1.0f) / 2.0f;
    }

    public float getScaleY() {
        return scaleY;
    }

    public int getOctaves() {
        return octaves;
    }

    public FastNoise getNoise(int seed) {
        return new FastNoise(seed)
            .SetNoiseType(noiseType)
            .SetFrequency(frequency)
            .SetFractalType(FastNoise.FractalType.RigidMulti)
            .SetFractalOctaves(octaves);
    }
}