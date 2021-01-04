package com.personthecat.cavegenerator.model;

import fastnoise.FastNoise;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.experimental.FieldDefaults;

import java.util.Optional;
import java.util.Random;

import static com.personthecat.cavegenerator.util.CommonMethods.empty;

/**
 * Contains all of the generic info related to spawning noise-based features in the
 * world. This is applicable for 2-dimensional noise only. With most of these parameters
 * now being merged directly into FastNoise, this class should soon be obsolete. The only
 * holdup is that it facilitates retrieving values from a preset in that it contains only
 * the values we need.
 */
@Builder(toBuilder = true)
@FieldDefaults(level = AccessLevel.PUBLIC, makeFinal = true)
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class NoiseSettings2D {

    /** A seed used for <b>producing</b> new seeds from a given value. */
    @Default Optional<Integer> seed = empty();

    /** Converts a range of 0-1 into a threshold for accepting output values. */
    @Default float scale = 0.5f;

    /** The target waveform frequency produced by the generator. */
    @Default float frequency = 1.0f;

    /** The minimum value to be produced by the generator. */
    @Default int min = -1;

    /** The maximum value to be produced by the generator. */
    @Default int max = 1;

    public FastNoise getGenerator(long seed) {
        return new FastNoise(getSeed(seed))
            .SetNoiseType(FastNoise.NoiseType.SimplexFractal)
            .SetScale(scale)
            .SetFrequency(frequency)
            .SetRange(min, max)
            .SetInterp(FastNoise.Interp.Hermite);
    }

    /** Generates a new seed from the input `base` value. */
    private int getSeed(long base) {
        final Random rand = new Random(base);
        final int next = rand.nextInt();
        return seed.map(num -> {
            final FastNoise simple = new FastNoise(next);
            return Float.floatToIntBits(simple.GetNoise(num));
        }).orElse(next);
    }
}