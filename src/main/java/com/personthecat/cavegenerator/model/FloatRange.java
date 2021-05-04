package com.personthecat.cavegenerator.model;

import lombok.EqualsAndHashCode;

import java.util.Random;

import static com.personthecat.cavegenerator.util.CommonMethods.f;
import static com.personthecat.cavegenerator.util.CommonMethods.numBetween;

@EqualsAndHashCode
public class FloatRange {
    public final float min, max;

    public FloatRange(float min, float max) {
        this.min = min;
        this.max = max;
    }

    public FloatRange(float a) {
        this(a, a);
    }

    public float rand(Random rand) {
        return numBetween(rand, min, max);
    }

    public float diff() {
        return max - min;
    }

    @Override
    public String toString() {
        return f("Range[{}~{}]", min, max);
    }
}
