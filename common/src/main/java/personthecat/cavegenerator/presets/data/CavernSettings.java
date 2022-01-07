package personthecat.cavegenerator.presets.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import lombok.Builder;
import lombok.experimental.FieldNameConstants;
import org.jetbrains.annotations.Nullable;
import personthecat.catlib.data.Range;
import personthecat.cavegenerator.presets.validator.CavernValidator;
import personthecat.cavegenerator.world.config.CavernConfig;

import java.util.Collections;
import java.util.List;
import java.util.Random;

import static personthecat.catlib.util.Shorthand.map;
import static personthecat.catlib.serialization.CodecUtils.dynamic;
import static personthecat.catlib.serialization.CodecUtils.easyList;
import static personthecat.catlib.serialization.DynamicField.extend;
import static personthecat.catlib.serialization.DynamicField.field;

// Todo: simplify default value handling
@Builder(toBuilder = true)
@FieldNameConstants
public class CavernSettings implements ConfigProvider<CavernSettings, CavernConfig> {
    @Nullable public final ConditionSettings conditions;
    @Nullable public final DecoratorSettings decorators;
    @Nullable public final Integer resolution;
    @Nullable public final NoiseSettings offset;
    @Nullable public final NoiseSettings walls;
    @Nullable public final NoiseSettings wallOffset;
    @Nullable public final Float wallCurveRatio;
    @Nullable public final Boolean wallInterpolation;
    @Nullable public final List<NoiseSettings> generators;
    @Nullable public final TunnelSettings branches;

    private static final NoiseSettings DEFAULT_GENERATOR =
        NoiseSettings.builder().frequency(0.0143F).threshold(Range.of(-0.6F)).frequencyY(0.0268F).octaves(1).build();
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
    ).flatXmap(CavernValidator::apply, DataResult::success);

    @Override
    public Codec<CavernSettings> codec() {
        return CODEC;
    }

    @Override
    public CavernSettings withOverrides(final OverrideSettings o) {
        final ConditionSettings conditions = this.conditions != null ? this.conditions : ConditionSettings.EMPTY;
        final DecoratorSettings decorators = this.decorators != null ? this.decorators : DecoratorSettings.EMPTY;
        return this.toBuilder()
            .conditions(conditions.withOverrides(o))
            .decorators(decorators.withOverrides(o))
            .branches(this.branches != null ? this.branches.withOverrides(o) : null)
            .build();
    }

    @Override
    public CavernConfig compile(final Random rand, final long seed) {
        final ConditionSettings conditionCfg = this.conditions != null
            ? this.conditions : ConditionSettings.EMPTY;
        final DecoratorSettings decoratorCfg = this.decorators != null
            ? this.decorators : DecoratorSettings.EMPTY;
        final List<NoiseSettings> generators = this.generators != null
            ? this.generators : Collections.singletonList(DEFAULT_GENERATOR);

        final ConditionSettings defaultedConditions = conditionCfg.withDefaults(DEFAULT_CONDITIONS)
            .withDefaultCeiling(DEFAULT_CEIL_NOISE).withDefaultFloor(DEFAULT_FLOOR_NOISE);
        final NoiseSettings offset = this.offset != null ? this.offset : DEFAULT_HEIGHT_OFFSET;
        final NoiseSettings walls = this.walls != null ? this.walls : DEFAULT_WALL_NOISE;
        final NoiseSettings wallOffset = this.wallOffset != null ? this.wallOffset : DEFAULT_WALL_OFFSET;

        return new CavernConfig(
            defaultedConditions.compile(rand, seed),
            decoratorCfg.compile(rand, seed),
            this.resolution != null ? this.resolution : 1,
            offset.getGenerator(rand, seed),
            walls.getGenerator(rand, seed),
            wallOffset.getGenerator(rand, seed),
            this.wallCurveRatio != null ? this.wallCurveRatio : 1.0F,
            this.wallInterpolation != null ? this.wallInterpolation : false,
            map(generators, g -> g.getGenerator(rand, seed)),
            this.branches != null ? this.branches.compile(rand, seed) : null
        );
    }
}
