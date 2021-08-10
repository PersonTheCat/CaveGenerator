package personthecat.cavegenerator.data;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import net.minecraft.world.level.Level;
import org.hjson.JsonObject;
import personthecat.catlib.data.Range;
import personthecat.catlib.util.HjsonMapper;
import personthecat.cavegenerator.noise.CachedNoiseGenerator;
import personthecat.cavegenerator.noise.DummyGenerator;
import personthecat.fastnoise.FastNoise;
import personthecat.fastnoise.data.DomainWarpType;
import personthecat.fastnoise.data.FractalType;
import personthecat.fastnoise.data.NoiseDescriptor;
import personthecat.fastnoise.data.NoiseType;

import java.util.Optional;

import static java.util.Optional.empty;
import static personthecat.catlib.util.Shorthand.full;

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
    @Default NoiseType type = NoiseType.SIMPLEX;

    /** Determines how the noise will be fractalized, if applicable. */
    @Default FractalType fractal = FractalType.NONE;

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

    public static NoiseMapSettings from(final JsonObject json, final NoiseMapSettings defaults) {
        return copyInto(json, defaults.toBuilder());
    }

    public static NoiseMapSettings from(final JsonObject json) {
        return copyInto(json, builder());
    }

    private static NoiseMapSettings copyInto(final JsonObject json, final NoiseMapSettingsBuilder builder) {
        return new HjsonMapper<>("<map>", NoiseMapSettingsBuilder::build)
            .mapInt(Fields.seed, (b, i) -> b.seed(full(i)))
            .mapFloat(Fields.frequency, NoiseMapSettingsBuilder::frequency)
            .mapNoiseType(Fields.type, NoiseMapSettingsBuilder::type)
            .mapFractalType(Fields.fractal, NoiseMapSettingsBuilder::fractal)
            .mapFloat(Fields.perturbAmp, NoiseMapSettingsBuilder::perturbAmp)
            .mapFloat(Fields.perturbFreq, NoiseMapSettingsBuilder::perturbFreq)
            .mapInt(Fields.octaves, NoiseMapSettingsBuilder::octaves)
            .mapRange(Fields.range, NoiseMapSettingsBuilder::range)
            .mapBool(Fields.perturb, NoiseMapSettingsBuilder::perturb)
            .mapBool(Fields.invert, NoiseMapSettingsBuilder::invert)
            .mapBool(Fields.cache, NoiseMapSettingsBuilder::cache)
            .mapBool(Fields.dummy, NoiseMapSettingsBuilder::dummy)
            .mapFloat(Fields.dummyOutput, (b, f) -> b.dummyOutput(full(f)))
            .create(json, builder);
    }

    public FastNoise getGenerator(final Level level) {
        if (dummy) {
            return new DummyGenerator(dummyOutput.orElseGet(() -> (range.min + range.max) / 2.0F));
        }
        final NoiseDescriptor cfg = FastNoise.createDescriptor()
            .seed(getSeed(level))
            .noise(type)
            .fractal(fractal)
            .frequency(frequency)
            .octaves(octaves)
            .range(range.min, range.max)
            .warp(perturb ? DomainWarpType.BASIC_GRID : DomainWarpType.NONE)
            .warpAmplitude(perturbAmp)
            .warpFrequency(perturbFreq)
            .invert(invert);

        final FastNoise generator = cfg.generate();

        return cache ? new CachedNoiseGenerator(cfg, generator) : generator;
    }

    private int getSeed(final Level level) {
        return level.random.nextInt();
    }

//    /** Generates a new seed from the input `base` value. */
//    private int getSeed(final Level level) {
//        return seed.map(num -> {
//            final Random rand = new XoRoShiRo(world.getSeed());
//            final FastNoise simple = new FastNoise(rand.nextInt());
//            return Float.floatToIntBits(simple.GetNoise(num));
//        }).orElseGet(world.rand::nextInt);
//    }
}