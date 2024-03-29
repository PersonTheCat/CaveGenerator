package personthecat.cavegenerator.presets.data;

import com.mojang.serialization.Codec;
import lombok.Builder;
import lombok.experimental.FieldNameConstants;
import org.jetbrains.annotations.Nullable;
import personthecat.catlib.data.FloatRange;
import personthecat.catlib.data.Range;
import personthecat.catlib.serialization.DynamicCodec;
import personthecat.catlib.serialization.NoiseCodecs;
import personthecat.cavegenerator.noise.CachedNoiseGenerator;
import personthecat.cavegenerator.noise.DummyGenerator;
import personthecat.fastnoise.FastNoise;
import personthecat.fastnoise.data.*;
import personthecat.fastnoise.generator.PerlinNoise;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static personthecat.catlib.util.Shorthand.coalesce;
import static personthecat.catlib.serialization.CodecUtils.dynamic;
import static personthecat.catlib.serialization.CodecUtils.ofEnum;
import static personthecat.catlib.serialization.DynamicField.field;
import static personthecat.catlib.serialization.DynamicField.recursive;

@FieldNameConstants
@Builder(toBuilder = true)
public class NoiseSettings {
    @Nullable public final Integer seed;
    @Nullable public final Float frequencyX;
    @Nullable public final Float frequencyY;
    @Nullable public final Float frequencyZ;
    @Nullable public final Float frequency;
    @Nullable public final Float stretch;
    @Nullable public final FloatRange threshold;
    @Nullable public final Float lacunarity;
    @Nullable public final Float gain;
    @Nullable public final Float warpAmplitude;
    @Nullable public final Float warpFrequency;
    @Nullable public final Float jitterX;
    @Nullable public final Float jitterY;
    @Nullable public final Float jitterZ;
    @Nullable public final Float jitter;
    @Nullable public final Integer octaves;
    @Nullable public final Integer offset;
    @Nullable public final Range range;
    @Nullable public final Boolean invert;
    @Nullable public final Boolean cache;
    @Nullable public final Boolean dummy;
    @Nullable public final Float dummyOutput;
    @Nullable public final Float pingPongStrength;
    @Nullable public final NoiseType type;
    @Nullable public final FractalType fractal;
    @Nullable public final DomainWarpType warp;
    @Nullable public final MultiType multi;
    @Nullable public final CellularDistanceType distFunc;
    @Nullable public final CellularReturnType returnType;
    @Nullable public final NoiseType cellularLookup;
    @Nullable public final List<NoiseSettings> references;

    public static final DynamicCodec<NoiseSettingsBuilder, NoiseSettings, NoiseSettings> COMMON_CODEC =
        dynamic(NoiseSettingsBuilder::new, NoiseSettingsBuilder::build).create(
            field(Codec.INT, Fields.seed, s -> s.seed, (s, i) -> s.seed = i),
            field(Codec.FLOAT, Fields.frequencyX, s -> s.frequencyX, (s, f) -> s.frequencyX = f),
            field(Codec.FLOAT, Fields.frequencyZ, s -> s.frequencyZ, (s, f) -> s.frequencyZ = f),
            field(Codec.FLOAT, Fields.frequency, s -> s.frequency, (s, f) -> s.frequency = f),
            field(Codec.FLOAT, Fields.lacunarity, s -> s.lacunarity, (s, l) -> s.lacunarity = l),
            field(Codec.FLOAT, Fields.gain, s -> s.gain, (s, g) -> s.gain = g),
            field(Codec.FLOAT, Fields.warpAmplitude, s -> s.warpAmplitude, (s, a) -> s.warpAmplitude = a),
            field(Codec.FLOAT, Fields.warpFrequency, s -> s.warpFrequency, (s, f) -> s.warpFrequency = f),
            field(Codec.FLOAT, Fields.jitterX, s -> s.jitterX, (s, j) -> s.jitterX = j),
            field(Codec.FLOAT, Fields.jitterZ, s -> s.jitterZ, (s, j) -> s.jitterZ = j),
            field(Codec.FLOAT, Fields.jitter, s -> s.jitter, (s, j) -> s.jitter = j),
            field(Codec.INT, Fields.octaves, s -> s.octaves, (s, o) -> s.octaves = o),
            field(Codec.BOOL, Fields.invert, s -> s.invert, (s, i) -> s.invert = i),
            field(Codec.BOOL, Fields.cache, s -> s.cache, (s, c) -> s.cache = c),
            field(Codec.BOOL, Fields.dummy, s -> s.dummy, (s, d) -> s.dummy = d),
            field(Codec.FLOAT, Fields.dummyOutput, s -> s.dummyOutput, (s, d) -> s.dummyOutput = d),
            field(Codec.FLOAT, Fields.pingPongStrength, s -> s.pingPongStrength, (s, p) -> s.pingPongStrength = p),
            field(NoiseCodecs.TYPE, Fields.type, s -> s.type, (s, t) -> s.type = t),
            field(NoiseCodecs.FRACTAL, Fields.fractal, s -> s.fractal, (s, f) -> s.fractal = f),
            field(NoiseCodecs.WARP, Fields.warp, s -> s.warp, (s, w) -> s.warp = w),
            field(ofEnum(MultiType.class), Fields.multi, s -> s.multi, (s, m) -> s.multi = m),
            field(NoiseCodecs.DISTANCE, Fields.distFunc, s -> s.distFunc, (s, d) -> s.distFunc = d),
            field(NoiseCodecs.RETURN, Fields.returnType, s -> s.returnType, (s, r) -> s.returnType = r),
            field(NoiseCodecs.TYPE, Fields.cellularLookup, s -> s.cellularLookup, (s, l) -> s.cellularLookup = l),
            recursive(Fields.references, s -> s.references, (s, r) -> s.references = r)
        );

    public static final DynamicCodec<NoiseSettingsBuilder, NoiseSettings, NoiseSettings> MAP =
        COMMON_CODEC.withMoreFields(
            field(Range.CODEC, Fields.range, s -> s.range, (s, r) -> s.range = r)
        );

    public static final DynamicCodec<NoiseSettingsBuilder, NoiseSettings, NoiseSettings> REGION =
        COMMON_CODEC.withMoreFields(
            field(FloatRange.CODEC, Fields.threshold, s -> s.threshold, (s, t) -> s.threshold = t)
        );

    public static final DynamicCodec<NoiseSettingsBuilder, NoiseSettings, NoiseSettings> NOISE =
        COMMON_CODEC.withMoreFields(
            field(FloatRange.CODEC, Fields.threshold, s -> s.threshold, (s, t) -> s.threshold = t),
            field(Codec.FLOAT, Fields.frequencyY, s -> s.frequencyY, (s, f) -> s.frequencyY = f),
            field(Codec.FLOAT, Fields.jitterY, s -> s.jitterY, (s, j) -> s.jitterY = j),
            field(Codec.INT, Fields.offset, s -> s.offset, (s, o) -> s.offset = o),
            field(Codec.FLOAT, Fields.stretch, s -> s.stretch, (s, f) -> s.stretch = f)
        );

    public static Codec<NoiseSettings> defaultedMap(final NoiseSettings defaults) {
        return MAP.withBuilder(defaults::toBuilder);
    }

    public static Codec<NoiseSettings> defaultedRegion(final NoiseSettings defaults) {
        return REGION.withBuilder(defaults::toBuilder);
    }

    public static Codec<NoiseSettings> defaultedNoise(final NoiseSettings defaults) {
        return NOISE.withBuilder(defaults::toBuilder);
    }

    public static FastNoise compile(final @Nullable NoiseSettings cfg, final Random rand, final long seed) {
        return cfg == null ? new DummyGenerator(0L) : cfg.getGenerator(rand, seed);
    }

    public static NoiseSettings withDefaults(final @Nullable NoiseSettings noise, final NoiseSettings defaults) {
        return noise != null ? noise.withDefaults(defaults) : defaults;
    }

    public NoiseSettings withDefaults(final NoiseSettings defaults) {
        return builder()
            .seed(this.seed != null ? this.seed : defaults.seed)
            .frequency(this.frequency != null ? this.frequency : defaults.frequency)
            .frequencyX(this.frequencyX != null ? this.frequencyX : defaults.frequencyX)
            .frequencyY(this.frequencyY != null ? this.frequencyY : defaults.frequencyY)
            .frequencyZ(this.frequencyZ != null ? this.frequencyZ : defaults.frequencyZ)
            .stretch(this.stretch != null ? this.stretch : defaults.stretch)
            .threshold(this.threshold != null ? this.threshold : defaults.threshold)
            .lacunarity(this.lacunarity != null ? this.lacunarity : defaults.lacunarity)
            .gain(this.gain != null ? this.gain : defaults.gain)
            .warpAmplitude(this.warpAmplitude != null ? this.warpAmplitude : defaults.warpAmplitude)
            .warpFrequency(this.warpFrequency != null ? this.warpFrequency : defaults.warpFrequency)
            .jitter(this.jitter != null ? this.jitter : defaults.jitter)
            .jitterX(this.jitterX != null ? this.jitterX : defaults.jitterX)
            .jitter(this.jitterY != null ? this.jitterY : defaults.jitterY)
            .jitter(this.jitterZ != null ? this.jitterZ : defaults.jitterZ)
            .octaves(this.octaves != null ? this.octaves : defaults.octaves)
            .offset(this.offset != null ? this.offset : defaults.offset)
            .range(this.range != null ? this.range : defaults.range)
            .invert(this.invert != null ? this.invert : defaults.invert)
            .cache(this.cache != null ? this.cache : defaults.cache)
            .dummy(this.dummy != null ? this.dummy : defaults.dummy)
            .dummyOutput(this.dummyOutput != null ? this.dummyOutput : defaults.dummyOutput)
            .pingPongStrength(this.pingPongStrength != null ? this.pingPongStrength : defaults.pingPongStrength)
            .type(this.type != null ? this.type : defaults.type)
            .fractal(this.fractal != null ? this.fractal : defaults.fractal)
            .warp(this.warp != null ? this.warp : defaults.warp)
            .multi(this.multi != null ? this.multi : defaults.multi)
            .distFunc(this.distFunc != null ? this.distFunc : defaults.distFunc)
            .returnType(this.returnType != null ? this.returnType : defaults.returnType)
            .cellularLookup(this.cellularLookup != null ? this.cellularLookup : defaults.cellularLookup)
            .references(this.references != null ? this.references : defaults.references)
            .build();
    }

    public FastNoise getGenerator(final Random rand, final long seed) {
        if (this.dummy == Boolean.TRUE) {
            return new DummyGenerator(this.dummyOutput != null ? this.dummyOutput : 1.0F);
        }
        final NoiseDescriptor cfg = this.getDescriptor(rand, seed);
        final FastNoise generator = cfg.generate();
        return this.cache == Boolean.TRUE ? new CachedNoiseGenerator(cfg, generator) : generator;
    }

    private NoiseDescriptor getDescriptor(final Random rand, final long seed) {
        final NoiseDescriptor cfg = FastNoise.createDescriptor();
        if (this.cellularLookup != null) cfg.noiseLookup(FastNoise.createDescriptor().noise(this.cellularLookup));

        if (this.references != null) {
            final List<NoiseDescriptor> descriptors = new ArrayList<>();
            for (final NoiseSettings settings : this.references) {
                descriptors.add(settings.getDescriptor(rand, seed));
            }
            cfg.noiseLookup(descriptors);
        }
        if (this.threshold != null) {
            cfg.threshold(this.threshold.min, this.threshold.max);
        } else {
            cfg.threshold(0.0F, 0.0F);
        }
        if (this.range != null) {
            cfg.range(this.range.min, this.range.max);
        } else {
            cfg.range(-1, 1);
        }
        cfg.frequencyX(coalesce(this.frequencyX, this.frequency, 0.01F))
            .frequencyY(coalesce(this.frequencyY, this.frequency, 0.01F))
            .frequencyZ(coalesce(this.frequencyZ, this.frequency, 0.01F));

        // experimental / temporary backwards compat.
        if (this.stretch != null) {
            if (this.frequency != null) {
                cfg.frequencyY(this.frequency / this.stretch);
            } else {
                cfg.frequencyY(((cfg.frequencyX() + cfg.frequencyZ()) / 2.0F) / this.stretch);
            }
        }
        cfg.seed(getSeed(rand, seed))
            .noise(this.type != null ? this.type : NoiseType.SIMPLEX)
            .fractal(this.fractal != null ? this.fractal : FractalType.NONE)
            .octaves(this.octaves != null ? this.octaves : 3)
            .invert(this.invert != null ? this.invert : false)
            .gain(this.gain != null ? this.gain : 0.5F)
            .cellularReturn(this.returnType != null ? this.returnType : CellularReturnType.DISTANCE2)
            .warpAmplitude(this.warpAmplitude != null ? this.warpAmplitude : 1.0F)
            .warpFrequency(this.warpFrequency != null ? this.warpFrequency : 1.0F)
            .warp(this.warp != null ? this.warp : DomainWarpType.NONE)
            .multi(this.multi != null ? this.multi : MultiType.SUM)
            .lacunarity(this.lacunarity != null ? this.lacunarity : 1.0F)
            .distance(this.distFunc != null ? this.distFunc : CellularDistanceType.EUCLIDEAN)
            .jitterX(coalesce(this.jitterX, this.jitter, 0.45F))
            .jitterY(coalesce(this.jitterY, this.jitter, 0.45F))
            .jitterZ(coalesce(this.jitterZ, this.jitter, 0.45F))
            .offset(this.offset != null ? this.offset : 0)
            .pingPongStrength(this.pingPongStrength != null ? this.pingPongStrength : 2.0F);
        return cfg;
    }

    private int getSeed(final Random rand, final long seed) {
        if (this.seed == null) {
            return rand.nextInt();
        }
        final FastNoise simple = new PerlinNoise(new Random(seed).nextInt());
        return Float.floatToIntBits(simple.getNoise(this.seed));
    }
}