package com.personthecat.cavegenerator.noise;

import fastnoise.FastNoise;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class DummyGenerator extends FastNoise {

    private final float output;

    @Override
    public float GetAdjustedNoise(float x, float y) {
        return output;
    }

    @Override
    public float GetAdjustedNoise(float x, float y, float z) {
        return output;
    }

    @Override
    public boolean GetBoolean(float x, float y) {
        return true;
    }

    @Override
    public boolean GetBoolean(float x, float y, float z) {
        return true;
    }

    @Override
    public float GetNoise(float x, float y) {
        return 0F;
    }

    @Override
    public float GetNoise(float x, float y, float z) {
        return 0F;
    }
}
