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

import java.util.Optional;

import static com.personthecat.cavegenerator.util.CommonMethods.empty;
import static com.personthecat.cavegenerator.util.CommonMethods.full;
import static com.personthecat.cavegenerator.util.CommonMethods.invert;

@FieldNameConstants
@Builder(toBuilder = true)
@FieldDefaults(level = AccessLevel.PUBLIC, makeFinal = true)
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class TunnelSettings {

    /** The name of this feature to be used globally in serialization. */
    private static final String FEATURE_NAME = CavePreset.Fields.tunnels;

    /** Default spawn conditions for all tunnel generators. */
    private static final ConditionSettings DEFAULT_CONDITIONS = ConditionSettings.builder()
        .height(Range.of(8, 128)).build();

    /** Default decorator settings for all tunnel generators. */
    private static final DecoratorSettings DEFAULT_DECORATORS = DecoratorSettings.DEFAULTS;

    /** Conditions for these tunnels to spawn. */
    @Default ConditionSettings conditions = DEFAULT_CONDITIONS;

    /** Cave blocks and wall decorators applied to these tunnels. */
    @Default DecoratorSettings decorators = DEFAULT_DECORATORS;

    /** Controls a vanilla function for reducing vertical noise. */
    @Default boolean noiseYReduction = true;

    /** Controls whether tunnels shrink when branching apart, as in vanilla. */
    @Default boolean resizeBranches = true;

    /** An optional different kind of tunnel to spawn when these tunnels branch apart. */
    @Default Optional<TunnelSettings> branches = empty();

    /** Settings for how rooms should spawn at tunnel system origins. */
    @Default RoomSettings rooms = RoomSettings.builder().build();

    /** Horizontal rotation. */
    @Default ScalableFloat dYaw = new ScalableFloat(0.0F, 0.0F, 0.75F, 4.0F, 1.0F);

    /** Vertical rotation. */
    @Default ScalableFloat dPitch = new ScalableFloat(0.0f, 0.0f, 0.9f, 2.0F, 1.0F);

    /** Overall scale. */
    @Default ScalableFloat scale = new ScalableFloat(0.0F, 1.0F, 1.0F, 0.0F, 1.0F);

    /** Vertical scale ratio. */
    @Default ScalableFloat stretch = new ScalableFloat(1.0F, 1.0F, 1.0F, 0.0F, 1.0F);

    /** Horizontal angle in radians. */
    @Default ScalableFloat yaw = new ScalableFloat(0.0F, 1.0F, 1.0F, 0.0F, 1.0F);

    /** Vertical angle in radians. */
    @Default ScalableFloat pitch = new ScalableFloat(0.0F, 0.25F, 1.0F, 0.0F, 1.0F);

    /** The chance that this tunnel will spawn as part of a system. */
    @Default int systemChance = 4;

    /** The chance will spawn successfully. Lower value distance between tunnel systems. */
    @Default int chance = 7;

    /** The number of branches spawned when a tunnel system is created. */
    @Default int systemDensity = 4;

    /** The expected distance of the first cave generated in this system. 0 -> (132 to 136)? */
    @Default int distance = 0;

    /** The number of tunnel origins per chunk. */
    @Default int count = 15;

    /** The 1/x chance of tunnel segments being skipped. */
    @Default int resolution = 4;

    public static TunnelSettings from(JsonObject json, OverrideSettings overrides) {
        final ConditionSettings conditions = overrides.apply(DEFAULT_CONDITIONS.toBuilder()).build();
        final DecoratorSettings decorators = overrides.apply(DEFAULT_DECORATORS.toBuilder()).build();
        return copyInto(json, builder().conditions(conditions).decorators(decorators));
    }

    public static TunnelSettings from(JsonObject json) {
        return copyInto(json, builder());
    }

    private static TunnelSettings copyInto(JsonObject json, TunnelSettingsBuilder builder) {
        final TunnelSettings original = builder.build();
        return new HjsonMapper(json)
            .mapSelf(o -> builder.conditions(ConditionSettings.from(o, original.conditions)))
            .mapSelf(o -> builder.decorators(DecoratorSettings.from(o, original.decorators)))
            .mapBool(Fields.noiseYReduction, builder::noiseYReduction)
            .mapBool(Fields.resizeBranches, builder::resizeBranches)
            .mapObject(Fields.branches, o -> builder.branches(full(from(o))))
            .mapObject(Fields.rooms, o -> builder.rooms(RoomSettings.from(o)))
            .mapScalableFloat(Fields.dYaw, original.dYaw, builder::dYaw)
            .mapScalableFloat(Fields.dPitch, original.dPitch, builder::dPitch)
            .mapScalableFloat(Fields.scale, original.scale, builder::scale)
            .mapScalableFloat(Fields.stretch, original.stretch, builder::stretch)
            .mapScalableFloat(Fields.yaw, original.yaw, builder::yaw)
            .mapScalableFloat(Fields.pitch, original.pitch, builder::pitch)
            .mapFloat(Fields.systemChance, f -> builder.systemChance(invert(f)))
            .mapFloat(Fields.chance, f -> builder.chance(invert(f)))
            .mapInt(Fields.systemDensity, builder::systemDensity)
            .mapInt(Fields.distance, builder::distance)
            .mapInt(Fields.count, builder::count)
            .mapInt(Fields.resolution, builder::resolution)
            .release(builder::build);
    }
}
