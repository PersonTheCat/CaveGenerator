package personthecat.cavegenerator.data;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import net.minecraft.world.level.block.state.BlockState;
import org.hjson.JsonObject;
import personthecat.catlib.data.Range;
import personthecat.catlib.util.HjsonMapper;

import java.util.List;
import java.util.Optional;

import static java.util.Optional.empty;
import static personthecat.catlib.util.Shorthand.full;

/** Contains all of the data needed for spawning alternative blocks in caves. */
@Builder
@FieldNameConstants
@FieldDefaults(level = AccessLevel.PUBLIC, makeFinal = true)
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class CaveBlockSettings {

    /** The name of this feature to be used globally in serialization. */
    private static final String FEATURE_NAME = DecoratorSettings.Fields.caveBlocks;

    /** The block to place instead of air. */
    List<BlockState> states;

    /** 0-1 spawn chance. */
    @Default double integrity = 1.0;

    /** Height bounds for this decorator. */
    @Default Range height = Range.of(0, 50);

    /** Noise Generator corresponding to this block. */
    @Default Optional<NoiseSettings> noise = empty();

    /** The default noise values for CaveBlocks with noise. */
    public static final NoiseSettings DEFAULT_NOISE = NoiseSettings.builder()
        .frequency(0.02f).threshold(Range.of(-0.8F)).stretch(1.0f).octaves(1).build();

    private static final HjsonMapper<CaveBlockSettingsBuilder, CaveBlockSettings> MAPPER =
        new HjsonMapper<>(FEATURE_NAME, CaveBlockSettingsBuilder::build)
            .mapRequiredStateList(Fields.states, CaveBlockSettingsBuilder::states)
            .mapObject(Fields.noise, CaveBlockSettings::copyNoise)
            .mapFloat(Fields.integrity, CaveBlockSettingsBuilder::integrity)
            .mapRange(Fields.height, CaveBlockSettingsBuilder::height);

    public static CaveBlockSettings from(final JsonObject json) {
        return MAPPER.create(builder(), json);
    }

    private static void copyNoise(final CaveBlockSettingsBuilder builder, final JsonObject json) {
        builder.noise(full(NoiseSettings.from(json, DEFAULT_NOISE)));
    }
}