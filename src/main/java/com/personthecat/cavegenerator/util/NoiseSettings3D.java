package com.personthecat.cavegenerator.util;

/**
 * Variant of NoiseSettings2D
 */
public class NoiseSettings3D {
    private final float selectionThreshold; // Calculated from scale.
    private final float frequency;
    private final float scaleY;
    private final int octaves;

    /** Where @param scale is a value between 0.0 and 1.0. */
    public NoiseSettings3D(float scale, float frequency, float scaleY, int octaves) {
        // Convert scale into a range from -1.0 to +1.0.
        this.selectionThreshold = (scale * 2.0f) - 1.0f;
        this.frequency = frequency;
        this.scaleY = scaleY;
        this.octaves = octaves;
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
}