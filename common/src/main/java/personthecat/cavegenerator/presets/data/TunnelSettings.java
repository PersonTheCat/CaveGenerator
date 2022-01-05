package personthecat.cavegenerator.presets.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import lombok.Builder;
import lombok.experimental.FieldNameConstants;
import org.jetbrains.annotations.Nullable;
import personthecat.catlib.data.Range;
import personthecat.cavegenerator.model.ScalableFloat;
import personthecat.cavegenerator.presets.validator.TunnelValidator;
import personthecat.cavegenerator.world.config.ConditionConfig;
import personthecat.cavegenerator.world.config.DecoratorConfig;
import personthecat.cavegenerator.world.config.RoomConfig;
import personthecat.cavegenerator.world.config.TunnelConfig;

import java.util.Random;

import static personthecat.catlib.serialization.CodecUtils.dynamic;
import static personthecat.catlib.serialization.DynamicField.extend;
import static personthecat.catlib.serialization.DynamicField.field;
import static personthecat.catlib.serialization.DynamicField.recursive;
import static personthecat.catlib.util.Shorthand.invert;

@Builder
@FieldNameConstants
public class TunnelSettings implements ConfigProvider<TunnelSettings, TunnelConfig> {
    @Nullable public final ConditionSettings conditions;
    @Nullable public final DecoratorSettings decorators;
    @Nullable public final Range originHeight;
    @Nullable public final Boolean noiseYReduction;
    @Nullable public final Boolean resizeBranches;
    @Nullable public final TunnelSettings branches;
    @Nullable public final RoomSettings rooms;
    @Nullable public final ScalableFloat dYaw;
    @Nullable public final ScalableFloat dPitch;
    @Nullable public final ScalableFloat scale;
    @Nullable public final ScalableFloat stretch;
    @Nullable public final ScalableFloat yaw;
    @Nullable public final ScalableFloat pitch;
    @Nullable public final Integer systemChance;
    @Nullable public final Integer chance;
    @Nullable public final Integer systemDensity;
    @Nullable public final Integer distance;
    @Nullable public final Integer count;
    @Nullable public final Integer resolution;
    @Nullable public final Long seed;
    @Nullable public final Boolean reseedBranches;
    @Nullable public final Boolean hasBranches;
    @Nullable public final Boolean checkWater;

    private static final ConditionSettings DEFAULT_CONDITIONS =
        ConditionSettings.builder().height(Range.of(8, 128)).build();

    public static final TunnelSettings EMPTY = 
        new TunnelSettings(null, null, null, null, null, null, null, null, null, null, 
        null, null, null, null, null, null, null, null, null, null, null, null, null);

    private static final ScalableFloat DEFAULT_D_YAW = new ScalableFloat(0.0F, 0.0F, 0.75F, 4.0F, 1.0F);
    private static final ScalableFloat DEFAULT_D_PITCH = new ScalableFloat(0.0f, 0.0f, 0.9f, 2.0F, 1.0F);
    private static final ScalableFloat DEFAULT_SCALE = new ScalableFloat(0.0F, 1.0F, 1.0F, 0.0F, 1.0F);
    private static final ScalableFloat DEFAULT_STRETCH = new ScalableFloat(1.0F, 1.0F, 1.0F, 0.0F, 1.0F);
    private static final ScalableFloat DEFAULT_YAW = new ScalableFloat(0.0F, 1.0F, 1.0F, 0.0F, 1.0F);
    private static final ScalableFloat DEFAULT_PITCH = new ScalableFloat(0.0F, 0.25F, 1.0F, 0.0F, 1.0F);

    private static final Codec<ScalableFloat> D_YAW_CODEC = ScalableFloat.defaultedCodec(DEFAULT_D_YAW);
    private static final Codec<ScalableFloat> D_PITCH_CODEC = ScalableFloat.defaultedCodec(DEFAULT_D_PITCH);
    private static final Codec<ScalableFloat> SCALE_CODEC = ScalableFloat.defaultedCodec(DEFAULT_SCALE);
    private static final Codec<ScalableFloat> STRETCH_CODEC = ScalableFloat.defaultedCodec(DEFAULT_STRETCH);
    private static final Codec<ScalableFloat> YAW_CODEC = ScalableFloat.defaultedCodec(DEFAULT_YAW);
    private static final Codec<ScalableFloat> PITCH_CODEC = ScalableFloat.defaultedCodec(DEFAULT_PITCH);

    public static final Codec<TunnelSettings> CODEC = dynamic(TunnelSettings::builder, TunnelSettingsBuilder::build).create(
        extend(ConditionSettings.CODEC, Fields.conditions, s -> s.conditions, (s, c) -> s.conditions = c),
        extend(DecoratorSettings.CODEC, Fields.decorators, s -> s.decorators, (s, c) -> s.decorators = c),
        field(Range.CODEC, Fields.originHeight, s -> s.originHeight, (s, h) -> s.originHeight = h),
        field(Codec.BOOL, Fields.noiseYReduction, s -> s.noiseYReduction, (s, r) -> s.noiseYReduction = r),
        field(Codec.BOOL, Fields.resizeBranches, s -> s.resizeBranches, (s, r) -> s.resizeBranches = r),
        recursive(Fields.branches, s -> s.branches, (s, b) -> s.branches = b),
        field(RoomSettings.CODEC, Fields.rooms, s -> s.rooms, (s, r) -> s.rooms = r),
        field(D_YAW_CODEC, Fields.dYaw, s -> s.dYaw, (s, f) -> s.dYaw = f),
        field(D_PITCH_CODEC, Fields.dPitch, s -> s.dPitch, (s, f) -> s.dPitch = f),
        field(SCALE_CODEC, Fields.scale, s -> s.scale, (s, f) -> s.scale = f),
        field(STRETCH_CODEC, Fields.stretch, s -> s.stretch, (s, f) -> s.stretch = f),
        field(YAW_CODEC, Fields.yaw, s -> s.yaw, (s, f) -> s.yaw = f),
        field(PITCH_CODEC, Fields.pitch, s -> s.pitch, (s, f) -> s.pitch = f),
        field(Codec.DOUBLE, Fields.systemChance, s -> invert(s.systemChance), (s, c) -> s.systemChance = invert(c)),
        field(Codec.DOUBLE, Fields.chance, s -> invert(s.chance), (s, c) -> s.chance = invert(c)),
        field(Codec.INT, Fields.systemDensity, s -> s.systemDensity, (s, d) -> s.systemDensity = d),
        field(Codec.INT, Fields.distance, s -> s.distance, (s, d) -> s.distance = d),
        field(Codec.INT, Fields.count, s -> s.count, (s, c) -> s.count = c),
        field(Codec.INT, Fields.resolution, s -> s.resolution, (s, r) -> s.resolution = r),
        field(Codec.LONG, Fields.seed, s -> s.seed, (s, l) -> s.seed = l),
        field(Codec.BOOL, Fields.reseedBranches, s -> s.reseedBranches, (s, b) -> s.reseedBranches = b),
        field(Codec.BOOL, Fields.hasBranches, s -> s.hasBranches, (s, b) -> s.hasBranches = b),
        field(Codec.BOOL, Fields.checkWater, s -> s.checkWater, (s, c) -> s.checkWater = c)
    ).flatXmap(TunnelValidator::apply, DataResult::success);

    @Override
    public Codec<TunnelSettings> codec() {
        return CODEC;
    }

    @Override
    public TunnelSettings withOverrides(final OverrideSettings o) {
        final ConditionSettings conditions = this.conditions != null
            ? this.conditions : ConditionSettings.EMPTY;
        final DecoratorSettings decorators = this.decorators != null
            ? this.decorators : DecoratorSettings.EMPTY;

        return builder()
            .conditions(conditions.withOverrides(o))
            .decorators(decorators.withOverrides(o))
            .branches(this.branches != null ? this.branches : o.branches)
            .rooms(this.rooms != null ? this.rooms : o.rooms)
            .build();
    }

    @Override
    public TunnelConfig compile(final Random rand, final long seed) {
        final ConditionSettings conditionCfg = this.conditions != null
            ? this.conditions : ConditionSettings.EMPTY;
        final DecoratorSettings decoratorCfg = this.decorators != null
            ? this.decorators : DecoratorSettings.EMPTY;
        final Range originHeight = this.originHeight != null ? this.originHeight : Range.of(0, 128);
        final boolean noiseYReduction = this.noiseYReduction != null ? this.noiseYReduction : true;
        final boolean resizeBranches = this.resizeBranches != null ? this.resizeBranches : true;
        final ScalableFloat dYaw = this.dYaw != null ? this.dYaw : DEFAULT_D_YAW;
        final ScalableFloat dPitch = this.dPitch != null ? this.dPitch : DEFAULT_D_PITCH;
        final ScalableFloat scale = this.scale != null ? this.scale : DEFAULT_SCALE;
        final ScalableFloat stretch = this.stretch != null ? this.stretch : DEFAULT_STRETCH;
        final ScalableFloat yaw = this.yaw != null ? this.yaw : DEFAULT_YAW;
        final ScalableFloat pitch = this.pitch != null ? this.pitch : DEFAULT_PITCH;
        final int systemChance = this.systemChance != null ? this.systemChance : 4;
        final int chance = this.chance != null ? this.chance : 7;
        final int systemDensity = this.systemDensity != null ? this.systemDensity : 4;
        final int distance = this.distance != null ? this.distance : 0;
        final int count = this.count != null ? this.count : 15;
        final int resolution = this.resolution != null ? this.resolution : 4;
        final boolean reseedBranches = this.reseedBranches != null ? this.reseedBranches : true;
        final boolean hasBranches = this.hasBranches != null ? this.hasBranches : true;
        final boolean checkWater = this.checkWater != null ? this.checkWater : true;

        final ConditionConfig conditions = conditionCfg.withDefaults(DEFAULT_CONDITIONS).compile(rand, seed);
        final DecoratorConfig decorators = decoratorCfg.compile(rand, seed);
        final TunnelConfig branches = this.branches != null
            ? this.branches.compile(rand, seed) : null;
        final RoomConfig rooms = this.rooms != null
            ? this.rooms.compile(rand, seed) : null;

        return new TunnelConfig(conditions, decorators, originHeight, noiseYReduction, resizeBranches,
            branches, rooms, dYaw, dPitch, scale, stretch, yaw, pitch, systemChance, chance, systemDensity,
            distance, count, resolution, seed, reseedBranches, hasBranches, checkWater);
    }
}
