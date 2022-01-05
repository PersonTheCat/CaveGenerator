package personthecat.cavegenerator.presets.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import lombok.Builder;
import lombok.experimental.FieldNameConstants;
import personthecat.catlib.data.Range;
import personthecat.cavegenerator.model.ScalableFloat;
import personthecat.cavegenerator.presets.validator.RavineValidator;
import personthecat.cavegenerator.world.config.ConditionConfig;
import personthecat.cavegenerator.world.config.DecoratorConfig;
import personthecat.cavegenerator.world.config.RavineConfig;
import personthecat.fastnoise.FastNoise;

import javax.annotation.Nullable;
import java.util.Random;

import static personthecat.catlib.serialization.CodecUtils.dynamic;
import static personthecat.catlib.serialization.DynamicField.*;
import static personthecat.catlib.serialization.DynamicField.field;
import static personthecat.catlib.util.Shorthand.invert;

@Builder(toBuilder = true)
@FieldNameConstants
public class RavineSettings {
    @Nullable public final ConditionSettings conditions;
    @Nullable public final DecoratorSettings decorators;
    @Nullable public final Range originHeight;
    @Nullable public final Float noiseYFactor;
    @Nullable public final ScalableFloat dYaw;
    @Nullable public final ScalableFloat dPitch;
    @Nullable public final ScalableFloat scale;
    @Nullable public final ScalableFloat stretch;
    @Nullable public final ScalableFloat yaw;
    @Nullable public final ScalableFloat pitch;
    @Nullable public final Integer distance;
    @Nullable public final Integer chance;
    @Nullable public final Integer resolution;
    @Nullable public final Double cutoffStrength;
    @Nullable public final Boolean checkWater;
    @Nullable public final NoiseSettings walls;

    private static final ConditionSettings DEFAULT_CONDITIONS =
        ConditionSettings.builder().height(Range.of(20, 66)).build();

    private static final ScalableFloat DEFAULT_D_YAW = new ScalableFloat(0.0F, 0.0F, 0.5F, 4.0F, 1.0F);
    private static final ScalableFloat DEFAULT_D_PITCH = new ScalableFloat(0.0F, 0.0F, 0.8F, 2.0F, 1.0F);
    private static final ScalableFloat DEFAULT_SCALE = new ScalableFloat(0.0F, 2.0F, 1.0F, 0.0F, 1.0F);
    private static final ScalableFloat DEFAULT_STRETCH = new ScalableFloat(3.0F, 1.0F, 1.0F, 0.0F, 1.0F);
    private static final ScalableFloat DEFAULT_YAW = new ScalableFloat(0.0F, 1.0F, 1.0F, 0.0F, 1.0F);
    private static final ScalableFloat DEFAULT_PITCH = new ScalableFloat(0.0F, 0.25F, 1.0F, 0.0F, 1.0F);

    private static final Codec<ScalableFloat> D_YAW_CODEC = ScalableFloat.defaultedCodec(DEFAULT_D_YAW);
    private static final Codec<ScalableFloat> D_PITCH_CODEC = ScalableFloat.defaultedCodec(DEFAULT_D_PITCH);
    private static final Codec<ScalableFloat> SCALE_CODEC = ScalableFloat.defaultedCodec(DEFAULT_SCALE);
    private static final Codec<ScalableFloat> STRETCH_CODEC = ScalableFloat.defaultedCodec(DEFAULT_STRETCH);
    private static final Codec<ScalableFloat> YAW_CODEC = ScalableFloat.defaultedCodec(DEFAULT_YAW);
    private static final Codec<ScalableFloat> PITCH_CODEC = ScalableFloat.defaultedCodec(DEFAULT_PITCH);

    private static final NoiseSettings DEFAULT_WALLS =
        NoiseSettings.builder().frequency(0.5F).range(Range.of(0, 4)).build();

    private static final Codec<NoiseSettings> DEFAULTED_WALLS = NoiseSettings.defaultedMap(DEFAULT_WALLS);

    public static final Codec<RavineSettings> CODEC = dynamic(RavineSettings::builder, RavineSettingsBuilder::build).create(
        extend(ConditionSettings.CODEC, Fields.conditions, s -> s.conditions, (s, c) -> s.conditions = c),
        extend(DecoratorSettings.CODEC, Fields.decorators, s -> s.decorators, (s, c) -> s.decorators = c),
        field(Range.CODEC, Fields.originHeight, s -> s.originHeight, (s, h) -> s.originHeight = h),
        field(Codec.FLOAT, Fields.noiseYFactor, s -> s.noiseYFactor, (s, f) -> s.noiseYFactor = f),
        field(D_YAW_CODEC, Fields.dYaw, s -> s.dYaw, (s, f) -> s.dYaw = f),
        field(D_PITCH_CODEC, Fields.dPitch, s -> s.dPitch, (s, f) -> s.dPitch = f),
        field(SCALE_CODEC, Fields.scale, s -> s.scale, (s, f) -> s.scale = f),
        field(STRETCH_CODEC, Fields.stretch, s -> s.stretch, (s, f) -> s.stretch = f),
        field(YAW_CODEC, Fields.yaw, s -> s.yaw, (s, f) -> s.yaw = f),
        field(PITCH_CODEC, Fields.pitch, s -> s.pitch, (s, f) -> s.pitch = f),
        field(Codec.INT, Fields.distance, s -> s.distance, (s, d) -> s.distance = d),
        field(Codec.DOUBLE, Fields.chance, s -> invert(s.chance), (s, c) -> s.chance = invert(c)),
        field(Codec.INT, Fields.resolution, s -> s.resolution, (s, r) -> s.resolution = r),
        field(Codec.DOUBLE, Fields.cutoffStrength, s -> s.cutoffStrength, (s, c) -> s.cutoffStrength = c),
        field(Codec.BOOL, Fields.checkWater, s -> s.checkWater, (s, c) -> s.checkWater = c),
        field(DEFAULTED_WALLS, Fields.walls, s -> s.walls, (s, w) -> s.walls = w)
    ).flatXmap(RavineValidator::apply, DataResult::success);

    public Codec<RavineSettings> codec() {
        return CODEC;
    }

    public RavineSettings withOverrides(final OverrideSettings o) {
        final ConditionSettings conditions = this.conditions != null ? this.conditions : ConditionSettings.EMPTY;
        final DecoratorSettings decorators = this.decorators != null ? this.decorators : DecoratorSettings.EMPTY;
        return this.toBuilder().conditions(conditions.withOverrides(o)).decorators(decorators.withOverrides(o)).build();
    }

    public RavineConfig compile(final Random rand, final long seed) {
        final ConditionSettings conditionsCfg = this.conditions != null ? this.conditions : ConditionSettings.EMPTY;
        final DecoratorSettings decoratorsCfg = this.decorators != null ? this.decorators : DecoratorSettings.EMPTY;
        final Range originHeight = this.originHeight != null ? this.originHeight : Range.of(20, 66);
        final float noiseYFactor = this.noiseYFactor != null ? this.noiseYFactor : 0.7F;
        final ScalableFloat dYaw = this.dYaw != null ? this.dYaw : DEFAULT_D_YAW;
        final ScalableFloat dPitch = this.dPitch != null ? this.dPitch : DEFAULT_D_PITCH;
        final ScalableFloat scale = this.scale != null ? this.scale : DEFAULT_SCALE;
        final ScalableFloat stretch = this.stretch != null ? this.stretch : DEFAULT_STRETCH;
        final ScalableFloat yaw = this.yaw != null ? this.yaw : DEFAULT_YAW;
        final ScalableFloat pitch = this.pitch != null ? this.pitch : DEFAULT_PITCH;
        final int distance = this.distance != null ? this.distance : 0;
        final int chance = this.chance != null ? this.chance : 5;
        final int resolution = this.resolution != null ? this.resolution : 4;
        final double cutoffStrength = this.cutoffStrength != null ? this.cutoffStrength : 5.0;
        final boolean checkWater = this.checkWater != null ? this.checkWater : true;

        final ConditionConfig conditions = conditionsCfg.withDefaults(DEFAULT_CONDITIONS).compile(rand, seed);
        final DecoratorConfig decorators = decoratorsCfg.compile(rand, seed);
        final FastNoise walls = this.walls != null ? this.walls.getGenerator(rand, seed) : null;
        final boolean useWallNoise = this.walls != null;

        return new RavineConfig(conditions, decorators, originHeight, noiseYFactor, dYaw, dPitch, scale,
            stretch, yaw, pitch, distance, chance, resolution, cutoffStrength, useWallNoise, checkWater, walls);
    }
}
