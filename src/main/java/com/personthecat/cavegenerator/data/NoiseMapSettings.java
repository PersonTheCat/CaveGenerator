package com.personthecat.cavegenerator.data;

import com.personthecat.cavegenerator.model.Range;
import com.personthecat.cavegenerator.noise.CachedNoiseGenerator;
import com.personthecat.cavegenerator.noise.DummyGenerator;
import com.personthecat.cavegenerator.util.HjsonMapper;
import com.personthecat.cavegenerator.util.XoRoShiRo;
import fastnoise.FastNoise;
import fastnoise.FastNoise.NoiseType;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import net.minecraft.world.World;
import org.hjson.JsonObject;

import java.util.Optional;
import java.util.Random;

import static com.personthecat.cavegenerator.util.CommonMethods.empty;
import static com.personthecat.cavegenerator.util.CommonMethods.full;

/**
 * Contains all of the generic info related to spawning noise-based features in the
 * world. This is applicable for 2-dimensional noise only. With most of these parameters
 * now being merged directly into FastNoise, this class should soon be obsolete. The only
 * holdup is that it facilitates retrieving values from a preset in that it contains only
 * the values we need.
 */
@FieldNameConstants
@Builder(toBuilder = true)
@FieldDefaults(level = AccessLevel.PUBLIC, makeFinal = true)
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class NoiseMapSettings {

    /** A seed used for <b>producing</b> new seeds from a given value. */
    @Default Optional<Integer> seed = empty();

    /** The target waveform frequency produced by the generator. */
    @Default float frequency = 1.0f;

    /** The type of noise generator to run. */
    @Default NoiseType type = NoiseType.SimplexFractal;

    /** The number of fractal generation passes. */
    @Default int octaves = 1;

    /** The range of values produced by the generator */
    @Default Range range = Range.of(-1, 1);

    /** Whether to cache the output for equivalent generators in the current chunk. */
    @Default boolean cache = false;

    /** Whether to treat this noise generator as a single value, improving performance. */
    @Default boolean dummy = false;

    public static NoiseMapSettings from(JsonObject json, NoiseMapSettings defaults) {
        return copyInto(json, defaults.toBuilder());
    }

    public static NoiseMapSettings from(JsonObject json) {
        return copyInto(json, builder());
    }

    private static NoiseMapSettings copyInto(JsonObject json, NoiseMapSettings.NoiseMapSettingsBuilder builder) {
        return new HjsonMapper(json)
            .mapInt(Fields.seed, i -> builder.seed(full(i)))
            .mapFloat(Fields.frequency, builder::frequency)
            .mapNoiseType(Fields.type, builder::type)
            .mapRange(Fields.range, builder::range)
            .mapBool(Fields.cache, builder::cache)
            .mapBool(Fields.dummy, builder::dummy)
            .release(builder::build);
    }

    public FastNoise getGenerator(World world) {
        if (dummy) {
            return new DummyGenerator((range.min + range.max) / 2.0F);
        }
        final FastNoise noise = new FastNoise(getSeed(world))
            .SetNoiseType(type)
            .SetFrequency(frequency)
            .SetFractalOctaves(octaves)
            .SetRange(range.min, range.max)
            .SetInterp(FastNoise.Interp.Hermite);
        return cache ? new CachedNoiseGenerator(noise) : noise;
    }

    /** Generates a new seed from the input `base` value. */
    private int getSeed(World world) {
        return seed.map(num -> {
            final Random rand = new XoRoShiRo(world.getSeed());
            final FastNoise simple = new FastNoise(rand.nextInt());
            return Float.floatToIntBits(simple.GetNoise(num));
        }).orElseGet(world.rand::nextInt);
    }
}