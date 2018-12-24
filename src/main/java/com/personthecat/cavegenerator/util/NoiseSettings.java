package com.personthecat.cavegenerator.util;

public class NoiseSettings {
    private final float selectionThreshold; // Calculated from scale.
    private final float frequency;
    private final float scaleY;
    private final float amplitude; // May have a small effect.

    public NoiseSettings(float scale, float frequency, float scaleY, float amplitude) {
        this.selectionThreshold = (scale * -2.0f) + 1.0f;
        this.frequency = frequency;
        this.scaleY = scaleY;
        this.amplitude = amplitude;
    }

    public float getSelectionThreshold() {
        return selectionThreshold;
    }

    public float getFrequency() {
        return frequency;
    }

    public float getScaleY() {
        return scaleY;
    }

    public float getAmplitude() {
        return amplitude;
    }
}