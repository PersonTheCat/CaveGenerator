package com.personthecat.cavegenerator.data;

import com.personthecat.cavegenerator.model.Range;
import com.personthecat.cavegenerator.util.HjsonMapper;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import org.hjson.JsonObject;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.personthecat.cavegenerator.util.CommonMethods.empty;
import static com.personthecat.cavegenerator.util.CommonMethods.full;

/** Contains all of the data needed for spawning alternative blocks in caves. */
@FieldNameConstants
@RequiredArgsConstructor
@Builder(toBuilder = true)
@FieldDefaults(level = AccessLevel.PUBLIC, makeFinal = true)
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class CaveBlockSettings {

    /** The name of this feature to be used globally in serialization. */
    private static final String FEATURE_NAME = DecoratorSettings.Fields.caveBlocks;

    /** The block to place instead of air. */
    List<IBlockState> states;

    /** 0-1 spawn chance. */
    @Default double chance = 1.0;

    /** Height bounds for this decorator. */
    @Default Range height = Range.of(0, 50);

    /** Minimum height bound. */
    @Default int minHeight = 0;

    /** Maximum height bound. */
    @Default int maxHeight = 50;

    /** Noise Generator corresponding to this block. */
    @Default Optional<NoiseSettings> noise = empty();

    /** The default noise values for CaveBlocks with noise. */
    public static final NoiseSettings DEFAULT_NOISE = NoiseSettings.builder()
        .frequency(0.02f).scale(0.1f).scaleY(1.0f).octaves(1).build();

    /** An instance of the vanilla lava CaveBlocks that exists by default in all presets. */
    public static final CaveBlockSettings VANILLA_LAVA = CaveBlockSettings.builder()
        .states(Collections.singletonList(Blocks.LAVA.getDefaultState())).height(Range.of(0, 10)).build();

    public static CaveBlockSettings from(JsonObject json) {
        final CaveBlockSettingsBuilder builder = builder();
        return new HjsonMapper(json)
            .mapRequiredStateList(Fields.states, FEATURE_NAME, builder::states)
            .mapObject(Fields.noise, o -> copyNoise(o, builder))
            .mapFloat(Fields.chance, builder::chance)
            .mapRange(Fields.height, builder::height)
            .release(builder::build);
    }

    private static void copyNoise(JsonObject json, CaveBlockSettingsBuilder builder) {
        builder.noise(full(NoiseSettings.from(json, DEFAULT_NOISE)));
    }
}