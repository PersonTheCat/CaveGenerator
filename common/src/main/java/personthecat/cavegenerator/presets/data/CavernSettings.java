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

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static java.util.Optional.empty;
import static personthecat.catlib.util.Shorthand.full;

@Builder
@FieldNameConstants
@FieldDefaults(level = AccessLevel.PUBLIC, makeFinal = true)
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class CavernSettings {

    /** The name of this feature to be used globally in serialization. */
    private static final String FEATURE_NAME = CavePreset.Fields.caverns;

    /** The default noise generator settings used by the caverns feature. */
    private static final NoiseSettings DEFAULT_GENERATOR =
        NoiseSettings.builder().frequency(0.0143F).threshold(Range.of(-0.6F)).stretch(0.5F).octaves(1).build();

    /** The default ceiling noise parameters used by caverns, if absent. */
    private static final NoiseMapSettings DEFAULT_CEIL_NOISE =
        NoiseMapSettings.builder().frequency(0.02F).range(Range.of(-17, -3)).build();

    /** The default floor noise parameters used by caverns, if absent. */
    private static final NoiseMapSettings DEFAULT_FLOOR_NOISE =
        NoiseMapSettings.builder().frequency(0.02F).range(Range.of(0, 8)).build();

    private static final NoiseMapSettings DEFAULT_HEIGHT_OFFSET =
        NoiseMapSettings.builder().frequency(0.005F).range(Range.of(0, 50)).build();

    /** The default wall noise used at biome borders. */
    private static final NoiseMapSettings DEFAULT_WALL_NOISE =
        NoiseMapSettings.builder().frequency(0.02F).range(Range.of(9, 15)).build();

    /** Transformations for biome walls for this generator. */
    private static final NoiseMapSettings DEFAULT_WALL_OFFSET =
        NoiseMapSettings.builder().frequency(0.05F).range(Range.of(0, 255)).build();

    /** Default spawn conditions for all cavern generators. */
    private static final ConditionSettings DEFAULT_CONDITIONS = ConditionSettings.builder()
        .height(Range.of(10, 50)).ceiling(full(DEFAULT_CEIL_NOISE)).floor(full(DEFAULT_FLOOR_NOISE)).build();

    /** Default decorator settings for all caverns generators. */
    private static final DecoratorSettings DEFAULT_DECORATORS = DecoratorSettings.DEFAULTS;

    /** Conditions for these caverns to spawn. */
    @Default ConditionSettings conditions = DEFAULT_CONDITIONS;

    /** Cave blocks and wall decorators applied to these caverns. */
    @Default DecoratorSettings decorators = DEFAULT_DECORATORS;

    /** The number of blocks to iterate and interpolate between when generating. */
    @Default int resolution = 1;

    /** How much to offset the y-value input to the generator based on (x, z). */
    @Default Optional<NoiseMapSettings> offset = empty();

    /** Settings for how to generate these walls, if applicable. */
    @Default Optional<NoiseMapSettings> walls = empty();

    /** Settings for translating wall noise up and down to obscure repetition. */
    @Default Optional<NoiseMapSettings> wallOffset = empty();

    /** Modifies the ceiling and floor curve around biome borders. */
    @Default float wallCurveRatio = 1.0f;

    /** Whether to interpolate biome borders for smoother walls. */
    @Default boolean wallInterpolation = false;

    /** A list of noise generators to produce the shape of these caverns. */
    @Default List<NoiseSettings> generators = Collections.singletonList(DEFAULT_GENERATOR);

    /** A list of tunnels that will spawn connected to these caverns. */
    @Default Optional<TunnelSettings> branches = empty();

    /** Whether to run this generator as a late feature. Will be removed. */
    @Default boolean deferred = false;

    public static CavernSettings from(final JsonObject json, final OverrideSettings overrides) {
        final ConditionSettings conditions = overrides.apply(DEFAULT_CONDITIONS.toBuilder()).build();
        final DecoratorSettings decorators = overrides.apply(DEFAULT_DECORATORS.toBuilder()).build();
        return copyInto(json, builder().conditions(conditions).decorators(decorators));
    }

    public static CavernSettings from(final JsonObject json) {
        return copyInto(json, builder());
    }

    private static CavernSettings copyInto(final JsonObject json, final CavernSettingsBuilder builder) {
        final CavernSettings original = builder.build();
        return new HjsonMapper<>(FEATURE_NAME, CavernSettingsBuilder::build)
            .mapSelf((b, o) -> b.conditions(ConditionSettings.from(o, original.conditions)))
            .mapSelf((b, o) -> b.decorators(DecoratorSettings.from(o, original.decorators)))
            .mapInt(Fields.resolution, CavernSettingsBuilder::resolution)
            .mapObject(Fields.offset, (b, o) -> b.offset(full(NoiseMapSettings.from(o, DEFAULT_HEIGHT_OFFSET))))
            .mapObject(Fields.walls, (b, o) -> b.walls(full(NoiseMapSettings.from(o, DEFAULT_WALL_NOISE))))
            .mapObject(Fields.wallOffset, (b, o) -> b.wallOffset(full(NoiseMapSettings.from(o, DEFAULT_WALL_OFFSET))))
            .mapFloat(Fields.wallCurveRatio, CavernSettingsBuilder::wallCurveRatio)
            .mapBool(Fields.wallInterpolation, CavernSettingsBuilder::wallInterpolation)
            .mapArray(Fields.generators, CavernSettings::createNoise, CavernSettingsBuilder::generators)
            .mapObject(Fields.branches, CavernSettings::copyBranches)
            .mapBool(Fields.deferred, CavernSettingsBuilder::deferred)
            .create(builder, json);
    }

    private static NoiseSettings createNoise(JsonObject json) {
        return NoiseSettings.from(json, DEFAULT_GENERATOR);
    }

    private static void copyBranches(CavernSettingsBuilder builder, JsonObject branches) {
        // Includes overrides and settings from the caverns object.
        final CavernSettings updated = builder.build();
        builder.branches(full(TunnelSettings.from(branches, updated.conditions, updated.decorators)));
    }
}
