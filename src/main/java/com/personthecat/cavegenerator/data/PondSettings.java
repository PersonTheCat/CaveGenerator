package com.personthecat.cavegenerator.data;

import com.personthecat.cavegenerator.model.Range;
import com.personthecat.cavegenerator.util.HjsonMapper;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Builder.Default;
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

@Builder
@FieldNameConstants
@FieldDefaults(level = AccessLevel.PUBLIC, makeFinal = true)
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class PondSettings {

    /** The name of this feature to be used globally in serialization. */
    private static final String FEATURE_NAME = DecoratorSettings.Fields.wallDecorators;

    /** The blocks used for decorating walls. */
    List<IBlockState> states;

    /** The 0-1 chance that any block will be placed. */
    @Default double integrity = 1.0;

    /** Height bounds for this decorator. */
    @Default Range height = Range.of(10, 50);

    /** How many layers deep to spawn. */
    @Default int depth = 2;

    /** A list of blocks to check for. */
    @Default List<IBlockState> matchers = Collections.singletonList(Blocks.STONE.getDefaultState());

    /** Optional noise generator used determine valid placements. */
    @Default Optional<NoiseSettings> noise = empty();

    /** The default noise values for WallDecorators with noise. */
    public static final NoiseSettings DEFAULT_NOISE = NoiseSettings.builder()
        .frequency(0.02f).threshold(Range.of(0.0F)).stretch(1.0f).build();

    public static PondSettings from(JsonObject json) {
        final PondSettingsBuilder builder = builder();
        return new HjsonMapper(json)
            .mapRequiredStateList(Fields.states, FEATURE_NAME, builder::states)
            .mapObject(Fields.noise, o -> copyNoise(o, builder))
            .mapFloat(Fields.integrity, builder::integrity)
            .mapRange(Fields.height, builder::height)
            .mapInt(Fields.depth, builder::depth)
            .mapStateList(Fields.matchers, builder::matchers)
            .release(builder::build);
    }

    private static void copyNoise(JsonObject json, PondSettingsBuilder builder) {
        builder.noise(full(NoiseSettings.from(json, DEFAULT_NOISE)));
    }

}
