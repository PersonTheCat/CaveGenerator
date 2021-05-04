package com.personthecat.cavegenerator.data;

import com.personthecat.cavegenerator.model.Range;
import com.personthecat.cavegenerator.util.HjsonMapper;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import net.minecraft.block.state.IBlockState;
import org.hjson.JsonObject;

import java.util.List;
import java.util.Optional;

import static com.personthecat.cavegenerator.util.CommonMethods.empty;
import static com.personthecat.cavegenerator.util.CommonMethods.full;

/** Contains all of the data needed for spawning alternative blocks in caves. */
@Builder
@FieldNameConstants
@FieldDefaults(level = AccessLevel.PUBLIC, makeFinal = true)
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class CaveBlockSettings {

    /** The name of this feature to be used globally in serialization. */
    private static final String FEATURE_NAME = DecoratorSettings.Fields.caveBlocks;

    /** The block to place instead of air. */
    List<IBlockState> states;

    /** 0-1 spawn chance. */
    @Default double integrity = 1.0;

    /** Height bounds for this decorator. */
    @Default Range height = Range.of(0, 50);

    /** Noise Generator corresponding to this block. */
    @Default Optional<NoiseSettings> noise = empty();

    /** The default noise values for CaveBlocks with noise. */
    public static final NoiseSettings DEFAULT_NOISE = NoiseSettings.builder()
        .frequency(0.02f).threshold(Range.of(-0.8F)).stretch(1.0f).octaves(1).build();

    public static CaveBlockSettings from(JsonObject json) {
        final CaveBlockSettingsBuilder builder = builder();
        return new HjsonMapper(json)
            .mapRequiredStateList(Fields.states, FEATURE_NAME, builder::states)
            .mapObject(Fields.noise, o -> copyNoise(o, builder))
            .mapFloat(Fields.integrity, builder::integrity)
            .mapRange(Fields.height, builder::height)
            .release(builder::build);
    }

    private static void copyNoise(JsonObject json, CaveBlockSettingsBuilder builder) {
        builder.noise(full(NoiseSettings.from(json, DEFAULT_NOISE)));
    }
}