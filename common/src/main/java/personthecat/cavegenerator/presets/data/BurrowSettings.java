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

import java.util.Optional;

import static java.util.Optional.empty;
import static personthecat.catlib.util.Shorthand.full;

@Builder
@FieldNameConstants
@FieldDefaults(level = AccessLevel.PUBLIC, makeFinal = true)
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class BurrowSettings {

    /** The default noise settings used for the map generator. */
    private static final NoiseMapSettings DEFAULT_MAP =
        NoiseMapSettings.builder().frequency(0.005F).perturb(true).perturbAmp(0.1F).perturbFreq(2.5F).build();

    /** The default noise settings used for the offset generator. */
    private static final NoiseMapSettings DEFAULT_OFFSET =
        NoiseMapSettings.builder().frequency(0.01F).range(Range.of(10, 30)).build();

    /** Default spawn conditions for all cavern generators. */
    private static final ConditionSettings DEFAULT_CONDITIONS =
        ConditionSettings.builder().height(Range.of(10, 50)).build();

    /** Default decorator settings for all caverns generators. */
    private static final DecoratorSettings DEFAULT_DECORATORS = DecoratorSettings.DEFAULTS;

    /** Conditions for these caverns to spawn. */
    @Default ConditionSettings conditions = DEFAULT_CONDITIONS;

    /** Cave blocks and wall decorators applied to these caverns. */
    @Default DecoratorSettings decorators = DEFAULT_DECORATORS;

    /** The output of this generator produces a sort of tunnel in 3-dimensional space. */
    @Default NoiseMapSettings map = DEFAULT_MAP;

    /** The output of this generator offsets the map values up and down over time. */
    @Default NoiseMapSettings offset = DEFAULT_OFFSET;

    /** The exact radius of these tunnels in blocks. */
    @Default float radius = 4.5F;

    /** The target noise threshold for this generator to trace. */
    @Default float target = 0.1F;

    /** A vertical ratio that scales radius. */
    @Default float stretch = 1.0F;

    /** The shape of the curve used by this generator. */
    @Default float exponent = 4.0F;

    /** Shifts the output of the generator up and down, transforming tunnels into blobs. */
    @Default float shift = 0.0F;

    /** The minimum distance from biome borders allowed by this generator. */
    @Default float wallDistance = 18.0F;

    /** The shape of the curve on biome borders. */
    @Default float wallExponent = 2.0F;

    /** An optional set of regular tunnels to spawn branching from this feature. */
    @Default Optional<TunnelSettings> branches = empty();

    public static BurrowSettings from(final JsonObject json, final OverrideSettings overrides) {
        final ConditionSettings conditions = overrides.apply(DEFAULT_CONDITIONS.toBuilder()).build();
        final DecoratorSettings decorators = overrides.apply(DEFAULT_DECORATORS.toBuilder()).build();
        return from(json, builder().conditions(conditions).decorators(decorators));
    }

    public static BurrowSettings from(final JsonObject json, final BurrowSettingsBuilder builder) {
        final BurrowSettings original = builder.build();
        return new HjsonMapper<>(CavePreset.Fields.burrows, BurrowSettingsBuilder::build)
            .mapSelf((b, o) -> b.conditions(ConditionSettings.from(o, original.conditions)))
            .mapSelf((b, o) -> builder.decorators(DecoratorSettings.from(o, original.decorators)))
            .mapObject(Fields.map, (b, o) -> b.map(NoiseMapSettings.from(o, DEFAULT_MAP)))
            .mapObject(Fields.offset, (b, o) -> b.offset(NoiseMapSettings.from(o, DEFAULT_OFFSET)))
            .mapFloat(Fields.radius, BurrowSettingsBuilder::radius)
            .mapFloat(Fields.target, BurrowSettingsBuilder::target)
            .mapFloat(Fields.stretch, BurrowSettingsBuilder::stretch)
            .mapFloat(Fields.exponent, BurrowSettingsBuilder::exponent)
            .mapFloat(Fields.shift, BurrowSettingsBuilder::shift)
            .mapFloat(Fields.wallDistance, BurrowSettingsBuilder::wallDistance)
            .mapFloat(Fields.wallExponent, BurrowSettingsBuilder::wallExponent)
            .mapObject(Fields.branches, BurrowSettings::copyBranches)
            .create(builder, json);
    }

    private static void copyBranches(final BurrowSettingsBuilder builder, final JsonObject branches) {
        // Includes overrides and settings from the caverns object.
        final BurrowSettings updated = builder.build();
        builder.branches(full(TunnelSettings.from(branches, updated.conditions, updated.decorators)));
    }
}
