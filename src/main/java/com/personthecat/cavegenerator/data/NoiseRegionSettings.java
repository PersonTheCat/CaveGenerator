package com.personthecat.cavegenerator.data;

import com.personthecat.cavegenerator.model.FloatRange;
import com.personthecat.cavegenerator.model.Range;
import com.personthecat.cavegenerator.util.HjsonMapper;
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
 *
 * Todo: update and say "this is separate from noise settings because it allows us to create an interface..."
 */
@FieldNameConstants
@Builder(toBuilder = true)
@FieldDefaults(level = AccessLevel.PUBLIC, makeFinal = true)
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class NoiseRegionSettings {

    /** A seed used for <b>producing</b> new seeds from a given value. */
    @Default Optional<Integer> seed = empty();

    /** Converts a range of 0-1 into a threshold for accepting output values. */
    @Default FloatRange threshold = Range.of(0.0F);

    /** The target waveform frequency produced by the generator. */
    @Default float frequency = 1.0f;

    /** Whether to invert the output of the generator. */
    @Default boolean invert = false;

    /** The type of noise generator to run. */
    @Default NoiseType type = NoiseType.SimplexFractal;

    public static NoiseRegionSettings from(JsonObject json, NoiseRegionSettings defaults) {
        return copyInto(json, defaults.toBuilder());
    }

    public static NoiseRegionSettings from(JsonObject json) {
        return copyInto(json, builder());
    }

    private static NoiseRegionSettings copyInto(JsonObject json, NoiseRegionSettingsBuilder builder) {
        return new HjsonMapper(json)
            .mapInt(Fields.seed, i -> builder.seed(full(i)))
            .mapFloat(Fields.frequency, builder::frequency)
            .mapFloatRange(Fields.threshold, builder::threshold)
            .mapNoiseType(Fields.type, builder::type)
            .release(builder::build);
    }

    public FastNoise getGenerator(World world) {
        return new FastNoise(getSeed(world))
            .SetNoiseType(type)
            .SetThreshold(threshold.min, threshold.max)
            .SetFrequency(frequency)
            .SetInvert(invert)
            .SetInterp(FastNoise.Interp.Hermite);
    }

    /** Generates a new seed from the input `base` value. */
    private int getSeed(World world) {
        return seed.map(num -> {
            final Random rand = new Random(world.getSeed());
            final FastNoise simple = new FastNoise(rand.nextInt());
            return Float.floatToIntBits(simple.GetNoise(num));
        }).orElseGet(world.rand::nextInt);
    }
}