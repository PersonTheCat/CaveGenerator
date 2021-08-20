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

import java.util.Optional;

import static java.util.Optional.empty;
import static personthecat.catlib.util.Shorthand.full;
import static personthecat.catlib.util.Shorthand.invert;

@Builder
@FieldNameConstants
@FieldDefaults(level = AccessLevel.PUBLIC, makeFinal = true)
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class TunnelSettings {

    /** Default spawn conditions for all tunnel generators. */
    private static final ConditionSettings DEFAULT_CONDITIONS = ConditionSettings.builder()
        .height(Range.of(8, 128)).build();

    /** Default decorator settings for all tunnel generators. */
    private static final DecoratorSettings DEFAULT_DECORATORS = DecoratorSettings.DEFAULTS;

    /** Conditions for these tunnels to spawn. */
    @Default ConditionSettings conditions = DEFAULT_CONDITIONS;

    /** Cave blocks and wall decorators applied to these tunnels. */
    @Default DecoratorSettings decorators = DEFAULT_DECORATORS;

    /** The height at which any tunnel system can originate. */
    @Default Range originHeight = Range.of(8, 128);

    /** Controls a vanilla function for reducing vertical noise. */
    @Default boolean noiseYReduction = true;

    /** Controls whether tunnels shrink when branching apart, as in vanilla. */
    @Default boolean resizeBranches = true;

    /** An optional different kind of tunnel to spawn when these tunnels branch apart. */
    @Default Optional<TunnelSettings> branches = empty();

    /** Settings for how rooms should spawn at tunnel system origins. */
    @Default Optional<RoomSettings> rooms = empty();

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

    /** A constant seed to use for these tunnels. */
    @Default Optional<Long> seed = empty();

    /** Whether to generate a fresh seed for tunnel branches. */
    @Default boolean reseedBranches = true;

    /** Whether this feature should have any branches at all. */
    @Default boolean hasBranches = true;

    /** Whether to test for water before spawning to avoid water walls. */
    @Default boolean checkWater = true;

    public static TunnelSettings from(final JsonObject json, final OverrideSettings overrides) {
        final ConditionSettings conditions = overrides.apply(DEFAULT_CONDITIONS.toBuilder()).build();
        final DecoratorSettings decorators = overrides.apply(DEFAULT_DECORATORS.toBuilder()).build();
        final TunnelSettingsBuilder builder = overrides.apply(builder());
        return copyInto(json, builder.conditions(conditions).decorators(decorators));
    }

    public static TunnelSettings from(final JsonObject json, final ConditionSettings conditions, final DecoratorSettings decorators) {
        return copyInto(json, builder().conditions(conditions).decorators(decorators));
    }

    public static TunnelSettings from(final JsonObject json) {
        return copyInto(json, builder());
    }

    private static TunnelSettings copyInto(final JsonObject json, final TunnelSettingsBuilder builder) {
        final TunnelSettings original = builder.build();
        return new HjsonMapper<>(CavePreset.Fields.tunnels, TunnelSettingsBuilder::build)
            .mapSelf((b, o) -> b.conditions(ConditionSettings.from(o, original.conditions)))
            .mapSelf((b, o) -> b.decorators(DecoratorSettings.from(o, original.decorators)))
            .mapRangeOrTry(Fields.originHeight, ConditionSettings.Fields.height, TunnelSettingsBuilder::originHeight)
            .mapBool(Fields.noiseYReduction, TunnelSettingsBuilder::noiseYReduction)
            .mapBool(Fields.resizeBranches, TunnelSettingsBuilder::resizeBranches)
            .mapObject(Fields.branches, (b, o) -> b.branches(full(from(o))))
            .mapObject(Fields.rooms, (b, o) -> b.rooms(full(RoomSettings.from(o))))
            .mapGeneric(Fields.dYaw, v -> ScalableFloat.fromValue(v, original.dYaw), TunnelSettingsBuilder::dYaw)
            .mapGeneric(Fields.dPitch, v -> ScalableFloat.fromValue(v, original.dPitch), TunnelSettingsBuilder::dPitch)
            .mapGeneric(Fields.scale, v -> ScalableFloat.fromValue(v, original.scale), TunnelSettingsBuilder::scale)
            .mapGeneric(Fields.stretch, v -> ScalableFloat.fromValue(v, original.stretch), TunnelSettingsBuilder::stretch)
            .mapGeneric(Fields.yaw, v -> ScalableFloat.fromValue(v, original.yaw), TunnelSettingsBuilder::yaw)
            .mapGeneric(Fields.pitch, v -> ScalableFloat.fromValue(v, original.pitch), TunnelSettingsBuilder::pitch)
            .mapFloat(Fields.systemChance, (b, f) -> b.systemChance(invert(f)))
            .mapFloat(Fields.chance, (b, f) -> b.chance(invert(f)))
            .mapInt(Fields.systemDensity, TunnelSettingsBuilder::systemDensity)
            .mapInt(Fields.distance, TunnelSettingsBuilder::distance)
            .mapInt(Fields.count, TunnelSettingsBuilder::count)
            .mapInt(Fields.resolution, TunnelSettingsBuilder::resolution)
            .mapInt(Fields.seed, (b, i) -> b.seed(full((long) i)))
            .mapBool(Fields.reseedBranches, TunnelSettingsBuilder::reseedBranches)
            .mapBool(Fields.hasBranches, TunnelSettingsBuilder::hasBranches)
            .mapBool(Fields.checkWater, TunnelSettingsBuilder::checkWater)
            .create(builder, json);
    }
}
