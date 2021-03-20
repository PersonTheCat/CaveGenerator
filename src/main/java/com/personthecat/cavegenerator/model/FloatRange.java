package com.personthecat.cavegenerator.model;

import lombok.EqualsAndHashCode;

import java.util.Random;

import static com.personthecat.cavegenerator.util.CommonMethods.f;
import static com.personthecat.cavegenerator.util.CommonMethods.numBetween;

@EqualsAndHashCode
public class FloatRange {
    public final float min, max;

    public FloatRange(float a, float b) {
        if (a > b) {
            this.min = a;
            this.max = b;
        } else {
            this.max = b;
            this.min = a;
        }
    }

    public FloatRange(float a) {
        this(a, a);
    }

    public float rand(Random rand) {
        return numBetween(rand, min, max);
    }

    public boolean contains(float num) {
        return num >= min && num <= max;
    }

    public float diff() {
        return max - min;
    }

    @Override
    public String toString() {
        return f("Range[{}-{}]", min, max);
    }
}
