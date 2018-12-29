package com.personthecat.cavegenerator.util;

/**
 * Contains all of the generic info related to spawning noise-
 * based features in the world. This is applicable for 2D noise
 * only.
 */
public class NoiseSettings2D {
    private final float selectionThreshold; // Calculated from scale.
    private final float frequency;
    private final int variance;

    /** Where @param scale is a value between 0.0 and 1.0. */
    public NoiseSettings2D(float scale, float frequency, int variance) {
        // Convert scale into a range from -1.0 to +1.0.
        this.selectionThreshold = (scale * 2.0f) - 1.0f;
        this.frequency = frequency;
        this.variance = variance;
    }

    public float getSelectionThreshold() {
        return selectionThreshold;
    }

    public float getScale() {
        return (selectionThreshold + 1.0f) / 2.0f;
    }

    public float getFrequency() {
        return frequency;
    }

    public int getVariance() {
        return variance;
    }
}