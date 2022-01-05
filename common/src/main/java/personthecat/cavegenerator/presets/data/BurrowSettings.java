package personthecat.cavegenerator.presets.data;

import com.mojang.serialization.Codec;
import lombok.Builder;
import lombok.experimental.FieldNameConstants;
import org.jetbrains.annotations.Nullable;
import personthecat.catlib.data.Range;
import personthecat.cavegenerator.world.config.BurrowConfig;
import personthecat.cavegenerator.world.config.ConditionConfig;
import personthecat.cavegenerator.world.config.DecoratorConfig;
import personthecat.cavegenerator.world.config.TunnelConfig;
import personthecat.fastnoise.FastNoise;
import personthecat.fastnoise.data.DomainWarpType;

import java.util.Random;

import static personthecat.catlib.serialization.CodecUtils.dynamic;
import static personthecat.catlib.serialization.DynamicField.extend;
import static personthecat.catlib.serialization.DynamicField.field;

@Builder(toBuilder = true)
@FieldNameConstants
public class BurrowSettings implements ConfigProvider<BurrowSettings, BurrowConfig> {
    @Nullable public final ConditionSettings conditions;
    @Nullable public final DecoratorSettings decorators;
    @Nullable public final NoiseSettings map;
    @Nullable public final NoiseSettings offset;
    @Nullable public final Float radius;
    @Nullable public final Float target;
    @Nullable public final Float stretch;
    @Nullable public final Float exponent;
    @Nullable public final Float shift;
    @Nullable public final Float wallDistance;
    @Nullable public final Float wallExponent;
    @Nullable public final TunnelSettings branches;

    private static final NoiseSettings DEFAULT_MAP =
        NoiseSettings.builder().frequency(0.005F).warp(DomainWarpType.BASIC_GRID).warpAmplitude(0.1F).warpFrequency(2.5F).build();
    private static final NoiseSettings DEFAULT_OFFSET =
        NoiseSettings.builder().frequency(0.01F).range(Range.of(10, 30)).build();
    private static final ConditionSettings DEFAULT_CONDITIONS =
        ConditionSettings.builder().height(Range.of(10, 50)).build();

    private static final Codec<NoiseSettings> DEFAULTED_MAP = NoiseSettings.defaultedMap(DEFAULT_MAP);
    private static final Codec<NoiseSettings> DEFAULTED_OFFSET = NoiseSettings.defaultedMap(DEFAULT_OFFSET);

    public static final Codec<BurrowSettings> CODEC = dynamic(BurrowSettings::builder, BurrowSettingsBuilder::build).create(
        extend(ConditionSettings.CODEC, Fields.conditions, s -> s.conditions, (s, c) -> s.conditions = c),
        extend(DecoratorSettings.CODEC, Fields.decorators, s -> s.decorators, (s, d) -> s.decorators = d),
        field(DEFAULTED_MAP, Fields.map, s -> s.map, (s, m) -> s.map = m),
        field(DEFAULTED_OFFSET, Fields.offset, s -> s.offset, (s, o) -> s.offset = o),
        field(Codec.FLOAT, Fields.radius, s -> s.radius, (s, r) -> s.radius = r),
        field(Codec.FLOAT, Fields.target, s -> s.target, (s, t) -> s.target = t),
        field(Codec.FLOAT, Fields.stretch, s -> s.stretch, (s, t) -> s.stretch = t),
        field(Codec.FLOAT, Fields.exponent, s -> s.exponent, (s, e) -> s.exponent = e),
        field(Codec.FLOAT, Fields.shift, s -> s.shift, (s, f) -> s.shift = f),
        field(Codec.FLOAT, Fields.wallDistance, s -> s.wallDistance, (s, d) -> s.wallDistance = d),
        field(Codec.FLOAT, Fields.wallExponent, s -> s.wallExponent, (s, e) -> s.wallExponent = e),
        field(TunnelSettings.CODEC, Fields.branches, s -> s.branches, (s, b) -> s.branches = b)
    );

    @Override
    public Codec<BurrowSettings> codec() {
        return CODEC;
    }

    @Override
    public BurrowSettings withOverrides(final OverrideSettings o) {
        final ConditionSettings conditions = this.conditions != null ? this.conditions : ConditionSettings.EMPTY;
        final DecoratorSettings decorators = this.decorators != null ? this.decorators : DecoratorSettings.EMPTY;
        final TunnelSettings branches = this.branches != null ? this.branches : TunnelSettings.EMPTY;
        return this.toBuilder()
            .conditions(conditions.withOverrides(o))
            .decorators(decorators.withOverrides(o))
            .branches(branches.withOverrides(o))
            .build();
    }

    @Override
    public BurrowConfig compile(final Random rand, final long seed) {
        final ConditionSettings conditionsCfg = this.conditions != null ? this.conditions : ConditionSettings.EMPTY;
        final DecoratorSettings decoratorsCfg = this.decorators != null ? this.decorators : DecoratorSettings.EMPTY;
        final NoiseSettings mapCfg = this.map != null ? this.map : DEFAULT_MAP;
        final NoiseSettings offsetCfg = this.offset != null ? this.offset : DEFAULT_OFFSET;
        final float radius = this.radius != null ? this.radius : 4.5F;
        final float target = this.target != null ? this.target : 0.1F;
        final float stretch = this.stretch != null ? this.stretch : 1.0F;
        final float exponent = this.exponent != null ? this.exponent : 4.0F;
        final float shift = this.shift != null ? this.shift : 0.0F;
        final float wallDistance = this.wallDistance != null ? this.wallDistance : 18.0F;
        final float wallExponent = this.wallExponent != null ? this.wallExponent : 2.0F;

        final ConditionConfig conditions = conditionsCfg.withDefaults(DEFAULT_CONDITIONS).compile(rand, seed);
        final DecoratorConfig decorators = decoratorsCfg.compile(rand, seed);
        final FastNoise map = mapCfg.getGenerator(rand, seed);
        final FastNoise offset = offsetCfg.getGenerator(rand, seed);
        final TunnelConfig branches = this.branches != null ? this.branches.compile(rand, seed) : null;

        return new BurrowConfig(conditions, decorators, map, offset, radius, target,
            stretch, exponent, shift, wallDistance, wallExponent, branches);
    }
}
