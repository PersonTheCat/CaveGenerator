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
    private final FastNoise.FractalType fractalType;

    /** Where @param scale is a value between 0.0 and 1.0. */
    public NoiseSettings3D(
        float frequency,
        float scale,
        float scaleY,
        int octaves,
        FastNoise.NoiseType noiseType,
        FastNoise.FractalType fractalType
    ) {
        // Convert scale into a range from -1.0 to +1.0.
        this.selectionThreshold = (scale * 2.0f) - 1.0f;
        this.frequency = frequency;
        this.scaleY = scaleY;
        this.octaves = octaves;
        this.noiseType = noiseType;
        this.fractalType = fractalType;
    }

    /** Variant of the primary constructor with a default value for noiseType. */
    public NoiseSettings3D(float frequency, float scale, float scaleY, int octaves) {
        this(frequency, scale, scaleY, octaves, FastNoise.NoiseType.SimplexFractal, FastNoise.FractalType.FBM);
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

    public FastNoise.NoiseType getNoiseType() {
        return noiseType;
    }

    public FastNoise.FractalType getFractalType() {
        return fractalType;
    }

    public FastNoise getGenerator(int seed) {
        return new FastNoise(seed)
            .SetNoiseType(noiseType)
            .SetFrequency(frequency)
            .SetFractalType(fractalType)
            .SetFractalOctaves(octaves);
//            .SetCellularNoiseLookup(new FastNoise())
//            .SetCellularReturnType(FastNoise.CellularReturnType.NoiseLookup);
    }
}