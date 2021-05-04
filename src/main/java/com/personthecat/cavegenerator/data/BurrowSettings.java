package com.personthecat.cavegenerator.data;

import com.personthecat.cavegenerator.model.Range;
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

    public static BurrowSettings from(JsonObject json, OverrideSettings overrides) {
        final ConditionSettings conditions = overrides.apply(DEFAULT_CONDITIONS.toBuilder()).build();
        final DecoratorSettings decorators = overrides.apply(DEFAULT_DECORATORS.toBuilder()).build();
        return from(json, builder().conditions(conditions).decorators(decorators));
    }

    public static BurrowSettings from(JsonObject json, BurrowSettingsBuilder builder) {
        final BurrowSettings original = builder.build();
        return new HjsonMapper(json)
            .mapSelf(o -> builder.conditions(ConditionSettings.from(o, original.conditions)))
            .mapSelf(o -> builder.decorators(DecoratorSettings.from(o, original.decorators)))
            .mapObject(Fields.map, o -> builder.map(NoiseMapSettings.from(o, DEFAULT_MAP)))
            .mapObject(Fields.offset, o -> builder.offset(NoiseMapSettings.from(o, DEFAULT_OFFSET)))
            .mapFloat(Fields.radius, builder::radius)
            .mapFloat(Fields.target, builder::target)
            .mapFloat(Fields.stretch, builder::stretch)
            .mapFloat(Fields.exponent, builder::exponent)
            .mapFloat(Fields.shift, builder::shift)
            .mapFloat(Fields.wallDistance, builder::wallDistance)
            .mapFloat(Fields.wallExponent, builder::wallExponent)
            .mapObject(Fields.branches, o -> copyBranches(builder, o))
            .release(builder::build);
    }

    private static void copyBranches(BurrowSettingsBuilder builder, JsonObject branches) {
        // Includes overrides and settings from the caverns object.
        final BurrowSettings updated = builder.build();
        builder.branches(full(TunnelSettings.from(branches, updated.conditions, updated.decorators)));
    }
}
