package personthecat.cavegenerator.presets.data;

import com.mojang.serialization.Codec;
import lombok.Builder;
import lombok.NonNull;
import lombok.experimental.FieldNameConstants;
import org.jetbrains.annotations.Nullable;
import personthecat.catlib.data.Range;
import personthecat.cavegenerator.world.config.CavernConfig;
import personthecat.cavegenerator.world.config.ConditionConfig;

import java.util.Collections;
import java.util.List;
import java.util.Random;

import static personthecat.catlib.util.Shorthand.map;
import static personthecat.catlib.serialization.CodecUtils.dynamic;
import static personthecat.catlib.serialization.CodecUtils.easyList;
import static personthecat.catlib.serialization.DynamicField.extend;
import static personthecat.catlib.serialization.DynamicField.field;

@Builder(toBuilder = true)
@FieldNameConstants
public class CavernSettings implements ConfigProvider<CavernSettings, CavernConfig> {
    @NonNull public final ConditionSettings conditions;
    @NonNull public final DecoratorSettings decorators;
    @Nullable public final Integer resolution;
    @Nullable public final NoiseSettings offset;
    @Nullable public final NoiseSettings walls;
    @Nullable public final NoiseSettings wallOffset;
    @Nullable public final Float wallCurveRatio;
    @Nullable public final Boolean wallInterpolation;
    @Nullable public final List<NoiseSettings> generators;
    @Nullable public final TunnelSettings branches;

    private static final NoiseSettings DEFAULT_GENERATOR =
        NoiseSettings.builder().frequency(0.0143F).threshold(Range.of(-0.6F)).frequencyY(0.0268F).build();
    private static final NoiseSettings DEFAULT_CEIL_NOISE =
        NoiseSettings.builder().frequency(0.02F).range(Range.of(-17, -3)).build();
    private static final NoiseSettings DEFAULT_FLOOR_NOISE =
        NoiseSettings.builder().frequency(0.02F).range(Range.of(0, 8)).build();
    private static final NoiseSettings DEFAULT_HEIGHT_OFFSET =
        NoiseSettings.builder().frequency(0.005F).range(Range.of(0, 50)).build();
    private static final NoiseSettings DEFAULT_WALL_NOISE =
        NoiseSettings.builder().frequency(0.02F).range(Range.of(9, 15)).build();
    private static final NoiseSettings DEFAULT_WALL_OFFSET =
        NoiseSettings.builder().frequency(0.05F).range(Range.of(0, 255)).build();
    private static final ConditionSettings DEFAULT_CONDITIONS =
        ConditionSettings.builder().height(Range.of(10, 50))
            .ceiling(DEFAULT_CEIL_NOISE).floor(DEFAULT_FLOOR_NOISE).build();

    private static final Codec<NoiseSettings> DEFAULTED_GENERATOR = NoiseSettings.defaultedNoise(DEFAULT_GENERATOR);
    private static final Codec<NoiseSettings> DEFAULTED_OFFSET = NoiseSettings.defaultedMap(DEFAULT_HEIGHT_OFFSET);
    private static final Codec<NoiseSettings> DEFAULTED_WALL = NoiseSettings.defaultedMap(DEFAULT_WALL_NOISE);
    private static final Codec<NoiseSettings> DEFAULTED_WALL_OFFSET = NoiseSettings.defaultedMap(DEFAULT_WALL_OFFSET);

    public static final Codec<CavernSettings> CODEC = dynamic(CavernSettings::builder, CavernSettingsBuilder::build).create(
        extend(ConditionSettings.CODEC, Fields.conditions, s -> s.conditions, (s, c) -> s.conditions = c),
        extend(DecoratorSettings.CODEC, Fields.decorators, s -> s.decorators, (s, d) -> s.decorators = d),
        field(Codec.INT, Fields.resolution, s -> s.resolution, (s, r) -> s.resolution = r),
        field(DEFAULTED_OFFSET, Fields.offset, s -> s.offset, (s, o) -> s.offset = o),
        field(DEFAULTED_WALL, Fields.walls, s -> s.walls, (s, w) -> s.walls = w),
        field(DEFAULTED_WALL_OFFSET, Fields.wallOffset, s -> s.wallOffset, (s, o) -> s.wallOffset = o),
        field(Codec.FLOAT, Fields.wallCurveRatio, s -> s.wallCurveRatio, (s, r) -> s.wallCurveRatio = r),
        field(Codec.BOOL, Fields.wallInterpolation, s -> s.wallInterpolation, (s, i) -> s.wallInterpolation = i),
        field(easyList(DEFAULTED_GENERATOR), Fields.generators, s -> s.generators, (s, g) -> s.generators = g),
        field(TunnelSettings.CODEC, Fields.branches, s -> s.branches, (s, b) -> s.branches = b)
    );

    @Override
    public Codec<CavernSettings> codec() {
        return CODEC;
    }

    @Override
    public CavernSettings withOverrides(final OverrideSettings o) {
        return this.toBuilder()
            .conditions(this.conditions.withOverrides(o))
            .decorators(this.decorators.withOverrides(o))
            .branches(this.branches != null ? this.branches.withOverrides(o) : null)
            .build();
    }

    @Override
    public CavernConfig compile(final Random rand, final long seed) {
        final List<NoiseSettings> generators = this.generators != null
            ? this.generators : Collections.singletonList(DEFAULT_GENERATOR);

        final ConditionSettings conditions = this.conditions.withDefaults(DEFAULT_CONDITIONS)
            .withDefaultCeiling(DEFAULT_CEIL_NOISE).withDefaultFloor(DEFAULT_FLOOR_NOISE);
        final ConditionConfig conditionCfg = conditions.compile(rand, seed);

        return new CavernConfig(
            conditionCfg,
            decorators.compile(rand, seed),
            getBounds(conditions, conditionCfg.height),
            this.resolution != null ? this.resolution : 1,
            NoiseSettings.compile(this.offset, rand, seed),
            this.walls != null ? this.walls.getGenerator(rand, seed) : null,
            NoiseSettings.compile(this.wallOffset, rand, seed),
            this.wallCurveRatio != null ? this.wallCurveRatio : 1.0F,
            this.wallInterpolation != null ? this.wallInterpolation : false,
            map(generators, g -> g.getGenerator(rand, seed)),
            this.branches != null ? this.branches.compile(rand, seed) : null
        );
    }

    @SuppressWarnings("ConstantConditions")
    private static Range getBounds(final ConditionSettings conditions, final Range height) {
        final int minFloor;
        if (conditions.floor != null && conditions.floor.range != null) {
            minFloor = conditions.floor.range.min;
        } else {
            minFloor = DEFAULT_FLOOR_NOISE.range.min;
        }
        final int maxCeil;
        if (conditions.ceiling != null && conditions.ceiling.range != null) {
            maxCeil = conditions.ceiling.range.max;
        } else {
            maxCeil = DEFAULT_CEIL_NOISE.range.max;
        }
        return Range.of(height.min + minFloor, height.max + maxCeil);
    }
}
