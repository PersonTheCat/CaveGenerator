package com.personthecat.cavegenerator.data;

import com.personthecat.cavegenerator.config.CavePreset;
import com.personthecat.cavegenerator.model.Range;
import com.personthecat.cavegenerator.model.ScalableFloat;
import com.personthecat.cavegenerator.util.HjsonMapper;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.hjson.JsonObject;

import static com.personthecat.cavegenerator.util.CommonMethods.invert;

@Builder
@FieldNameConstants
@FieldDefaults(level = AccessLevel.PUBLIC, makeFinal = true)
public class RavineSettings {

    /** The name of this feature to be used globally in serialization. */
    private static final String FEATURE_NAME = CavePreset.Fields.ravines;

    /** Default spawn conditions for all ravine generators. */
    private static final ConditionSettings DEFAULT_CONDITIONS = ConditionSettings.builder()
        .height(Range.of(20, 66)).build();

    /** Default decorator settings for all caverns generators. */
    private static final DecoratorSettings DEFAULT_DECORATORS = DecoratorSettings.DEFAULTS;

    /** Conditions for these ravines to spawn. */
    @Default ConditionSettings conditions = DEFAULT_CONDITIONS;

    /** Cave blocks and wall decorators applied to these ravines. */
    @Default DecoratorSettings decorators = DEFAULT_DECORATORS;

    /** The height at which any ravine can originate. */
    @Default Range originHeight = Range.of(20, 66);

    /** Statically reduces vertical noise. */
    @Default float noiseYFactor = 0.7F;

    /** Horizontal rotation. */
    @Default ScalableFloat dYaw = new ScalableFloat(0.0F, 0.0F, 0.5F, 4.0F, 1.0F);

    /** Vertical rotation. */
    @Default ScalableFloat dPitch = new ScalableFloat(0.0F, 0.0F, 0.8F, 2.0F, 1.0F);

    /** Overall scale. */
    @Default ScalableFloat scale = new ScalableFloat(0.0F, 2.0F, 1.0F, 0.0F, 1.0F);

    /** Vertical scale ratio. */
    @Default ScalableFloat stretch = new ScalableFloat(3.0F, 1.0F, 1.0F, 0.0F, 1.0F);

    /** Horizontal angle in radians. */
    @Default ScalableFloat yaw = new ScalableFloat(0.0F, 1.0F, 1.0F, 0.0F, 1.0F);

    /** Vertical angle in radians. */
    @Default ScalableFloat pitch = new ScalableFloat(0.0F, 0.25F, 1.0F, 0.0F, 1.0F);

    /** The expected distance of this ravine. 0 -> (132 to 136)? */
    @Default int distance = 0;

    /** The 1/x chance of this ravine spawning. */
    @Default int chance = 5;

    /** The 1/x chance of ravine segments being skipped. */
    @Default int resolution = 4;

    /** A ratio of how much to flatten the bottom and top of this feature. */
    @Default double cutoffStrength = 5.0;

    /** Whether to use a FastNoise generator for the wall of this ravine. */
    @Default boolean useWallNoise = false;

    /** Whether to test for water before spawning to avoid water walls. */
    @Default boolean checkWater = true;

    /** Settings for how to generate these walls, if applicable. */
    @Default NoiseMapSettings walls = NoiseMapSettings.builder()
        .frequency(0.5F).range(Range.of(0, 4)).build();

    public static RavineSettings from(JsonObject json, OverrideSettings overrides) {
        final ConditionSettings conditions = overrides.apply(DEFAULT_CONDITIONS.toBuilder()).build();
        final DecoratorSettings decorators = overrides.apply(DEFAULT_DECORATORS.toBuilder()).build();
        return copyInto(json, builder().conditions(conditions).decorators(decorators));
    }

    public static RavineSettings from(JsonObject json) {
        return copyInto(json, builder());
    }

    private static RavineSettings copyInto(JsonObject json, RavineSettingsBuilder builder) {
        final RavineSettings original = builder.build();
        return new HjsonMapper(json)
            .mapSelf(o -> builder.conditions(ConditionSettings.from(o, original.conditions)))
            .mapSelf(o -> builder.decorators(DecoratorSettings.from(o, original.decorators)))
            .mapRangeOrTry(Fields.originHeight, ConditionSettings.Fields.height, builder::originHeight)
            .mapFloat(Fields.noiseYFactor, builder::noiseYFactor)
            .mapScalableFloat(Fields.dYaw, original.dYaw, builder::dYaw)
            .mapScalableFloat(Fields.dPitch, original.dPitch, builder::dPitch)
            .mapScalableFloat(Fields.scale, original.scale, builder::scale)
            .mapScalableFloat(Fields.stretch, original.stretch, builder::stretch)
            .mapScalableFloat(Fields.yaw, original.yaw, builder::yaw)
            .mapScalableFloat(Fields.pitch, original.pitch, builder::pitch)
            .mapInt(Fields.distance, builder::distance)
            .mapFloat(Fields.chance, f -> builder.chance(invert(f)))
            .mapInt(Fields.resolution, builder::resolution)
            .mapFloat(Fields.cutoffStrength, builder::cutoffStrength)
            .mapObject(Fields.walls, o -> copyWallNoise(o, original, builder))
            .mapBool(Fields.useWallNoise, builder::useWallNoise)
            .mapBool(Fields.checkWater, builder::checkWater)
            .release(builder::build);
    }

    private static void copyWallNoise(JsonObject json, RavineSettings original, RavineSettingsBuilder builder) {
        builder.useWallNoise(true).walls(NoiseMapSettings.from(json, original.walls));
    }

}
