package com.personthecat.cavegenerator.data;

import com.personthecat.cavegenerator.config.PresetTester;
import com.personthecat.cavegenerator.model.FloatRange;
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
 * This class contains all of the information needed specifically for spawning features in
 * noise-based regions in the world. It is separate from regular {@link NoiseSettings}
 * essentially because it defines an interface which outlines exactly which fields will get
 * used in <code>region</code> objects. This way, users can be notified of the exact settings
 * getting used in their presets via {@link PresetTester}.
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

    /** Whether to cache the output for equivalent generators in the current chunk. */
    @Default boolean cache = false;

    /** Whether to treat this noise generator as a single value, improving performance. */
    @Default boolean dummy = false;

    /** The output to use if this generator is a dummy. */
    @Default float dummyOutput = 1.0F;

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
            .mapBool(Fields.cache, builder::cache)
            .mapBool(Fields.dummy, builder::dummy)
            .mapFloat(Fields.dummyOutput, builder::dummyOutput)
            .mapNoiseType(Fields.type, builder::type)
            .release(builder::build);
    }

    public FastNoise getGenerator(World world) {
        if (dummy) {
            return new DummyGenerator(dummyOutput);
        }
        final FastNoise noise = new FastNoise(getSeed(world))
            .SetNoiseType(type)
            .SetThreshold(threshold.min, threshold.max)
            .SetFrequency(frequency)
            .SetInvert(invert)
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