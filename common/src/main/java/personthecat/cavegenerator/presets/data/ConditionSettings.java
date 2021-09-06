package personthecat.cavegenerator.presets.data;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import net.minecraft.world.level.biome.Biome;
import org.hjson.JsonObject;
import personthecat.catlib.data.Range;
import personthecat.catlib.util.HjsonMapper;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static java.util.Optional.empty;
import static personthecat.catlib.util.Shorthand.full;

@FieldNameConstants
@Builder(toBuilder = true)
@FieldDefaults(level = AccessLevel.PUBLIC, makeFinal = true)
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class ConditionSettings {

    /** Whether the biomes list should be treated as a blacklist. */
    @Default boolean blacklistBiomes = false;

    /** A list of biomes in which this feature can spawn. */
    @Default List<Biome> biomes = Collections.emptyList();

    /** Whether the dimension list should be treated as a blacklist. */
    @Default boolean blacklistDimensions = false;

    /** A list of dimensions in which this feature can spawn.. */
    @Default List<Integer> dimensions = Collections.emptyList();

    /** Height restrictions for the current feature. */
    @Default Range height = Range.of(0, 255);

    /** Settings used for augmenting the maximum height level. */
    @Default Optional<NoiseMapSettings> floor = empty();

    /** Settings used for augmenting the minimum height level. */
    @Default Optional<NoiseMapSettings> ceiling = empty();

    /** Settings used to determine whether this feature can spawn when given 2 coordinates. */
    @Default Optional<NoiseRegionSettings> region = empty();

    /** Settings used to control 3-dimensional placement of this feature. */
    @Default Optional<NoiseSettings> noise = empty();

    public static ConditionSettings from(final JsonObject json, final ConditionSettings defaults) {
        return copyInto(json, defaults.toBuilder());
    }

    public static ConditionSettings from(final JsonObject json) {
        return copyInto(json, builder());
    }
    
    // Copies any values from the json into the builder, if present.
    private static ConditionSettings copyInto(final JsonObject json, final ConditionSettingsBuilder builder) {
        return new HjsonMapper<>("", ConditionSettingsBuilder::build)
            .mapBool(Fields.blacklistBiomes, ConditionSettingsBuilder::blacklistBiomes)
            .mapBiomes(Fields.biomes, ConditionSettingsBuilder::biomes)
            .mapBool(Fields.blacklistDimensions, ConditionSettingsBuilder::blacklistDimensions)
            .mapIntList(Fields.dimensions, ConditionSettingsBuilder::dimensions)
            .mapRange(Fields.height, ConditionSettingsBuilder::height)
            .mapObject(Fields.floor, ConditionSettings::copyFloor)
            .mapObject(Fields.ceiling, ConditionSettings::copyCeiling)
            .mapObject(Fields.region, ConditionSettings::copyRegion)
            .mapObject(Fields.noise, ConditionSettings::copyNoise)
            .create(builder, json);
    }

    private static void copyFloor(final ConditionSettingsBuilder builder, final JsonObject json) {
        final NoiseMapSettings floor = builder.floor$value
            .map(cfg -> NoiseMapSettings.from(json, cfg))
            .orElseGet(() -> NoiseMapSettings.from(json));
        builder.floor(full(floor));
    }

    private static void copyCeiling(final ConditionSettingsBuilder builder, final JsonObject json) {
        final NoiseMapSettings ceiling = builder.ceiling$value
            .map(cfg -> NoiseMapSettings.from(json, cfg))
            .orElseGet(() -> NoiseMapSettings.from(json));
        builder.ceiling(full(ceiling));
    }

    private static void copyRegion(final ConditionSettingsBuilder builder, final JsonObject json) {
        final NoiseRegionSettings region = builder.region$value
            .map(cfg -> NoiseRegionSettings.from(json, cfg))
            .orElseGet(() -> NoiseRegionSettings.from(json));
        builder.region(full(region));
    }

    private static void copyNoise(final ConditionSettingsBuilder builder, final JsonObject json) {
        final NoiseSettings noise = builder.noise$value
            .map(cfg -> NoiseSettings.from(json, cfg))
            .orElseGet(() -> NoiseSettings.from(json));
        builder.noise(full(noise));
    }
}
