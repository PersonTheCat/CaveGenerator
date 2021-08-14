package personthecat.cavegenerator.data;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.hjson.JsonObject;
import personthecat.catlib.data.Range;
import personthecat.catlib.util.HjsonMapper;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static java.util.Optional.empty;
import static personthecat.catlib.util.Shorthand.full;

@Builder
@FieldNameConstants
@FieldDefaults(level = AccessLevel.PUBLIC, makeFinal = true)
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class PondSettings {

    /** The name of this feature to be used globally in serialization. */
    private static final String FEATURE_NAME = DecoratorSettings.Fields.wallDecorators;

    /** The blocks used for decorating walls. */
    List<BlockState> states;

    /** The 0-1 chance that any block will be placed. */
    @Default double integrity = 1.0;

    /** Height bounds for this decorator. */
    @Default Range height = Range.of(10, 50);

    /** How many layers deep to spawn. */
    @Default int depth = 2;

    /** A list of blocks to check for. */
    @Default List<BlockState> matchers = Collections.singletonList(Blocks.STONE.defaultBlockState());

    /** Optional noise generator used determine valid placements. */
    @Default Optional<NoiseSettings> noise = empty();

    /** The default noise values for WallDecorators with noise. */
    public static final NoiseSettings DEFAULT_NOISE = NoiseSettings.builder()
        .frequency(0.02f).threshold(Range.of(0.0F)).stretch(1.0f).build();

    public static PondSettings from(final JsonObject json) {
        final PondSettingsBuilder builder = builder();
        return new HjsonMapper<>(DecoratorSettings.Fields.ponds, PondSettingsBuilder::build)
            .mapRequiredStateList(Fields.states, PondSettingsBuilder::states)
            .mapObject(Fields.noise, PondSettings::copyNoise)
            .mapFloat(Fields.integrity, PondSettingsBuilder::integrity)
            .mapRange(Fields.height, PondSettingsBuilder::height)
            .mapInt(Fields.depth, PondSettingsBuilder::depth)
            .mapStateList(Fields.matchers, PondSettingsBuilder::matchers)
            .create(builder, json);
    }

    private static void copyNoise(final PondSettingsBuilder builder, final JsonObject json) {
        builder.noise(full(NoiseSettings.from(json, DEFAULT_NOISE)));
    }

}
