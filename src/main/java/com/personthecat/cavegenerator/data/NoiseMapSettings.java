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

    /** The maximum amount to warp coordinates when perturb is enabled. */
    @Default float perturbAmp = 1.0F;

    /** The frequency used in warping input coordinates. */
    @Default float perturbFreq = 1.0F;

    /** The number of fractal generation passes. */
    @Default int octaves = 1;

    /** The range of values produced by the generator */
    @Default Range range = Range.of(-1, 1);

    /** Whether to apply a gradient perturb function. */
    @Default boolean perturb = false;

    /** Whether to invert the output of this generator. */
    @Default boolean invert = false;

    /** Whether to cache the output for equivalent generators in the current chunk. */
    @Default boolean cache = false;

    /** Whether to treat this noise generator as a single value, improving performance. */
    @Default boolean dummy = false;

    /** The output to use if this generator is a dummy. */
    @Default Optional<Float> dummyOutput = empty();

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
            .mapFloat(Fields.perturbAmp, builder::perturbAmp)
            .mapFloat(Fields.perturbFreq, builder::perturbFreq)
            .mapInt(Fields.octaves, builder::octaves)
            .mapRange(Fields.range, builder::range)
            .mapBool(Fields.perturb, builder::perturb)
            .mapBool(Fields.invert, builder::invert)
            .mapBool(Fields.cache, builder::cache)
            .mapBool(Fields.dummy, builder::dummy)
            .mapFloat(Fields.dummyOutput, f -> builder.dummyOutput(full(f)))
            .release(builder::build);
    }

    public FastNoise getGenerator(World world) {
        if (dummy) {
            return new DummyGenerator(dummyOutput.orElseGet(() -> (range.min + range.max) / 2.0F));
        }
        final FastNoise noise = new FastNoise(getSeed(world))
            .SetNoiseType(type)
            .SetFrequency(frequency)
            .SetFractalOctaves(octaves)
            .SetRange(range.min, range.max)
            .SetGradientPerturb(perturb)
            .SetGradientPerturbAmp(perturbAmp)
            .SetGradientPerturbFrequency(perturbFreq)
            .SetInterp(FastNoise.Interp.Hermite)
            .SetInvert(invert);
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