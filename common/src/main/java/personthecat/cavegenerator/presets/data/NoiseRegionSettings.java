package personthecat.cavegenerator.presets.data;

import personthecat.catlib.data.FloatRange;
import personthecat.catlib.data.Range;
import personthecat.catlib.util.HjsonMapper;
import personthecat.cavegenerator.presets.init.PresetTester;
import personthecat.cavegenerator.noise.CachedNoiseGenerator;
import personthecat.cavegenerator.noise.DummyGenerator;
import personthecat.fastnoise.FastNoise;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.hjson.JsonObject;
import personthecat.fastnoise.data.FractalType;
import personthecat.fastnoise.data.NoiseDescriptor;
import personthecat.fastnoise.data.NoiseType;
import personthecat.fastnoise.generator.PerlinNoise;

import java.util.Optional;
import java.util.Random;

import static java.util.Optional.empty;
import static personthecat.catlib.util.Shorthand.full;

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
    @Default NoiseType type = NoiseType.SIMPLEX;

    /** Determines how the noise will be fractalized, if applicable. */
    @Default FractalType fractal = FractalType.NONE;

    public static NoiseRegionSettings from(final JsonObject json, final NoiseRegionSettings defaults) {
        return copyInto(json, defaults.toBuilder());
    }

    public static NoiseRegionSettings from(final JsonObject json) {
        return copyInto(json, builder());
    }

    private static NoiseRegionSettings copyInto(final JsonObject json, final NoiseRegionSettingsBuilder builder) {
        return new HjsonMapper<>("<region>", NoiseRegionSettingsBuilder::build)
            .mapInt(Fields.seed, (b, i) -> b.seed(full(i)))
            .mapFloat(Fields.frequency, NoiseRegionSettingsBuilder::frequency)
            .mapFloatRange(Fields.threshold, NoiseRegionSettingsBuilder::threshold)
            .mapBool(Fields.cache, NoiseRegionSettingsBuilder::cache)
            .mapBool(Fields.dummy, NoiseRegionSettingsBuilder::dummy)
            .mapFloat(Fields.dummyOutput, NoiseRegionSettingsBuilder::dummyOutput)
            .mapNoiseType(Fields.type, NoiseRegionSettingsBuilder::type)
            .create(builder, json);
    }

    public FastNoise getGenerator(final Random rand, final long seed) {
        if (dummy) {
            return new DummyGenerator(dummyOutput);
        }
        final NoiseDescriptor cfg = FastNoise.createDescriptor()
            .seed(getSeed(rand, seed))
            .noise(type)
            .fractal(fractal)
            .threshold(threshold.min, threshold.max)
            .frequency(frequency)
            .invert(invert);

        final FastNoise generator = cfg.generate();

        return cache ? new CachedNoiseGenerator(cfg, generator) : generator;
    }

    private int getSeed(final Random rand, final long seed) {
        return this.seed.map(num -> {
            final FastNoise simple = new PerlinNoise(new Random(seed).nextInt());
            return Float.floatToIntBits(simple.getNoise(num));
        }).orElseGet(rand::nextInt);
    }
}