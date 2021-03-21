package com.personthecat.cavegenerator.data;

import com.personthecat.cavegenerator.config.CavePreset;
import com.personthecat.cavegenerator.config.ConfigFile;
import com.personthecat.cavegenerator.model.Range;
import com.personthecat.cavegenerator.util.HjsonMapper;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.hjson.JsonObject;

import java.util.Collections;
import java.util.List;

import static com.personthecat.cavegenerator.util.CommonMethods.full;

@FieldNameConstants
@Builder(toBuilder = true)
@FieldDefaults(level = AccessLevel.PUBLIC, makeFinal = true)
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

    /** Default spawn conditions for all cavern generators. */
    private static final ConditionSettings DEFAULT_CONDITIONS = ConditionSettings.builder()
        .height(Range.of(10, 50)).ceiling(full(DEFAULT_CEIL_NOISE)).floor(full(DEFAULT_FLOOR_NOISE)).build();

    /** Default decorator settings for all caverns generators. */
    private static final DecoratorSettings DEFAULT_DECORATORS = DecoratorSettings.DEFAULTS;

    /** Conditions for these caverns to spawn. */
    @Default ConditionSettings conditions = DEFAULT_CONDITIONS;

    /** Cave blocks and wall decorators applied to these caverns. */
    @Default DecoratorSettings decorators = DEFAULT_DECORATORS;

    /** Whether to spawn caverns based on default settings. */
    @Default boolean enabled = false;

    /** The number of blocks to iterate and interpolate between when generating. */
    @Default int resolution = 1;

    /** A list of noise generators to produce the shape of these caverns. */
    @Default List<NoiseSettings> generators = Collections.singletonList(DEFAULT_GENERATOR);

    public static CavernSettings from(JsonObject json, OverrideSettings overrides) {
        final ConditionSettings conditions = overrides.apply(DEFAULT_CONDITIONS.toBuilder()).build();
        final DecoratorSettings decorators = overrides.apply(DEFAULT_DECORATORS.toBuilder()).build();
        return copyInto(json, builder().conditions(conditions).decorators(decorators));
    }

    public static CavernSettings from(JsonObject json) {
        return copyInto(json, builder());
    }

    private static CavernSettings copyInto(JsonObject json, CavernSettingsBuilder builder) {
        final CavernSettings original = builder.build();
        new HjsonMapper(json)
            .mapSelf(o -> builder.conditions(ConditionSettings.from(o, original.conditions)))
            .mapSelf(o -> builder.decorators(DecoratorSettings.from(o, original.decorators)))
            .mapBool(Fields.enabled, builder::enabled)
            .mapInt(Fields.resolution, builder::resolution)
            .mapArray(Fields.generators, CavernSettings::createNoise, builder::generators);

        // Forcibly disable biome restrictions for caverns until they look better.
        if (!ConfigFile.forceEnableCavernBiomes) {
            final ConditionSettings conditions = builder.build().conditions;
            builder.conditions(conditions.toBuilder().biomes(Collections.emptyList()).build());
        }
        return builder.build();
    }

    private static NoiseSettings createNoise(JsonObject json) {
        return NoiseSettings.from(json, DEFAULT_GENERATOR);
    }

}
