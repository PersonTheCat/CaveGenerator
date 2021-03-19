package com.personthecat.cavegenerator.data;

import com.personthecat.cavegenerator.model.Range;
import com.personthecat.cavegenerator.util.HjsonMapper;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import net.minecraft.world.biome.Biome;
import org.hjson.JsonObject;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.personthecat.cavegenerator.util.CommonMethods.empty;
import static com.personthecat.cavegenerator.util.CommonMethods.full;

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

    public static ConditionSettings from(JsonObject json, ConditionSettings defaults) {
        return copyInto(json, defaults.toBuilder());
    }

    public static ConditionSettings from(JsonObject json) {
        return copyInto(json, builder());
    }
    
    // Copies any values from the json into the builder, if present.
    private static ConditionSettings copyInto(JsonObject json, ConditionSettingsBuilder builder) {
        return new HjsonMapper(json)
            .mapBool(Fields.blacklistBiomes, builder::blacklistBiomes)
            .mapBiomes(Fields.biomes, builder::biomes)
            .mapBool(Fields.blacklistDimensions, builder::blacklistDimensions)
            .mapIntList(Fields.dimensions, builder::dimensions)
            .mapRange(Fields.height, builder::height)
            .mapObject(Fields.floor, o -> copyFloor(o, builder))
            .mapObject(Fields.ceiling, o -> copyCeiling(o, builder))
            .mapObject(Fields.region, o -> copyRegion(o, builder))
            .mapObject(Fields.noise, o -> copyNoise(o, builder))
            .release(builder::build);
    }

    private static void copyFloor(JsonObject json, ConditionSettingsBuilder builder) {
        final NoiseMapSettings floor = builder.floor$value
            .map(cfg -> NoiseMapSettings.from(json, cfg))
            .orElseGet(() -> NoiseMapSettings.from(json));
        builder.floor(full(floor));
    }

    private static void copyCeiling(JsonObject json, ConditionSettingsBuilder builder) {
        final NoiseMapSettings ceiling = builder.ceiling$value
            .map(cfg -> NoiseMapSettings.from(json, cfg))
            .orElseGet(() -> NoiseMapSettings.from(json));
        builder.ceiling(full(ceiling));
    }

    private static void copyRegion(JsonObject json, ConditionSettingsBuilder builder) {
        final NoiseRegionSettings region = builder.region$value
            .map(cfg -> NoiseRegionSettings.from(json, cfg))
            .orElseGet(() -> NoiseRegionSettings.from(json));
        builder.region(full(region));
    }

    private static void copyNoise(JsonObject json, ConditionSettingsBuilder builder) {
        final NoiseSettings noise = builder.noise$value
            .map(cfg -> NoiseSettings.from(json, cfg))
            .orElseGet(() -> NoiseSettings.from(json));
        builder.noise(full(noise));
    }
}
