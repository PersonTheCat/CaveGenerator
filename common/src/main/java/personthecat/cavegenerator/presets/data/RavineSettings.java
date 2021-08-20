package personthecat.cavegenerator.presets.data;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.hjson.JsonObject;
import personthecat.catlib.data.Range;
import personthecat.catlib.util.HjsonMapper;
import personthecat.cavegenerator.presets.CavePreset;
import personthecat.cavegenerator.model.ScalableFloat;

import static personthecat.catlib.util.Shorthand.invert;

@Builder
@FieldNameConstants
@FieldDefaults(level = AccessLevel.PUBLIC, makeFinal = true)
public class RavineSettings {

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

    public static RavineSettings from(final JsonObject json, final OverrideSettings overrides) {
        final ConditionSettings conditions = overrides.apply(DEFAULT_CONDITIONS.toBuilder()).build();
        final DecoratorSettings decorators = overrides.apply(DEFAULT_DECORATORS.toBuilder()).build();
        return copyInto(json, builder().conditions(conditions).decorators(decorators));
    }

    public static RavineSettings from(final JsonObject json) {
        return copyInto(json, builder());
    }

    private static RavineSettings copyInto(final JsonObject json, final RavineSettingsBuilder builder) {
        final RavineSettings original = builder.build();
        return new HjsonMapper<>(CavePreset.Fields.ravines, RavineSettingsBuilder::build)
            .mapSelf((b, o) -> b.conditions(ConditionSettings.from(o, original.conditions)))
            .mapSelf((b, o) -> b.decorators(DecoratorSettings.from(o, original.decorators)))
            .mapRangeOrTry(Fields.originHeight, ConditionSettings.Fields.height, RavineSettingsBuilder::originHeight)
            .mapFloat(Fields.noiseYFactor, RavineSettingsBuilder::noiseYFactor)
            .mapGeneric(Fields.dYaw, v -> ScalableFloat.fromValue(v, original.dYaw), RavineSettingsBuilder::dYaw)
            .mapGeneric(Fields.dPitch, v -> ScalableFloat.fromValue(v, original.dPitch), RavineSettingsBuilder::dPitch)
            .mapGeneric(Fields.scale, v -> ScalableFloat.fromValue(v, original.scale), RavineSettingsBuilder::scale)
            .mapGeneric(Fields.stretch, v -> ScalableFloat.fromValue(v, original.stretch), RavineSettingsBuilder::stretch)
            .mapGeneric(Fields.yaw, v -> ScalableFloat.fromValue(v, original.yaw), RavineSettingsBuilder::yaw)
            .mapGeneric(Fields.pitch, v -> ScalableFloat.fromValue(v, original.pitch), RavineSettingsBuilder::pitch)
            .mapInt(Fields.distance, RavineSettingsBuilder::distance)
            .mapFloat(Fields.chance, (b, f) -> b.chance(invert(f)))
            .mapInt(Fields.resolution, RavineSettingsBuilder::resolution)
            .mapFloat(Fields.cutoffStrength, RavineSettingsBuilder::cutoffStrength)
            .mapObject(Fields.walls, (b, o) -> copyWallNoise(o, original, b))
            .mapBool(Fields.useWallNoise, RavineSettingsBuilder::useWallNoise)
            .mapBool(Fields.checkWater, RavineSettingsBuilder::checkWater)
            .create(builder, json);
    }

    private static void copyWallNoise(final JsonObject json, final RavineSettings original, final RavineSettingsBuilder builder) {
        builder.useWallNoise(true).walls(NoiseMapSettings.from(json, original.walls));
    }

}
