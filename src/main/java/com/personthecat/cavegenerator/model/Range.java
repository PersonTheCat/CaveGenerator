package com.personthecat.cavegenerator.model;

import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.Random;

import static com.personthecat.cavegenerator.util.CommonMethods.f;
import static com.personthecat.cavegenerator.util.CommonMethods.numBetween;

@EqualsAndHashCode
public class Range implements Iterable<Integer> {
    public final int min, max;

    public Range(int min, int max) {
        this.min = min;
        this.max = max;
    }

    public Range(int max) {
        this(max, max);
    }

    public static Range of(int a, int b) {
        return a > b ? new Range(b, a) : new Range(a, b);
    }

    public static Range of(int max) {
        return new Range(max);
    }

    public static FloatRange of(float a, float b) {
        return a > b ? new FloatRange(b, a) : new FloatRange(a, b);
    }

    public static FloatRange of(float a) {
        return new FloatRange(a);
    }

    public static Range checkedOrEmpty(int min, int max) {
        return max > min ? new Range(min, max) : EmptyRange.get();
    }

    public static EmptyRange empty() {
        return EmptyRange.get();
    }

    public int rand(Random rand) {
        return numBetween(rand, min, max);
    }

    public boolean contains(int num) {
        return num >= min && num < max;
    }

    public int diff() {
        return max - min;
    }

    public boolean isEmpty() {
        return false;
    }

    @NotNull
    @Override
    public Iterator<Integer> iterator() {
        return new Iterator<Integer>() {
            int i = min;

            @Override
            public boolean hasNext() {
                return i < max;
            }

            @Override
            public Integer next() {
                return i++;
            }
        };
    }

    @Override
    public String toString() {
        return f("Range[{}~{}]", min, max);
    }
}