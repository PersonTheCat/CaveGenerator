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
import personthecat.catlib.util.Shorthand;
import personthecat.cavegenerator.model.Direction;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static java.util.Optional.empty;
import static personthecat.catlib.util.Shorthand.full;

@Builder
@FieldNameConstants
@FieldDefaults(level = AccessLevel.PUBLIC, makeFinal = true)
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class WallDecoratorSettings {

    /** The blocks used for decorating walls. */
    List<BlockState> states;

    /** The 0-1 chance that any block will be placed. */
    @Default double integrity = 1.0;

    /** Height bounds for this decorator. */
    @Default Range height = Range.of(10, 50);

    /** A list of directions to place blocks. */
    @Default List<Direction> directions = Collections.singletonList(Direction.ALL);

    /** A list of blocks to check for. */
    @Default List<BlockState> matchers = Collections.singletonList(Blocks.STONE.defaultBlockState());

    /** Whether to place <b>on</b> the wall or <b>in</b> the wall. */
    @Default Placement placement = Placement.EMBED;

    /** Optional noise generator used determine valid placements. */
    @Default Optional<NoiseSettings> noise = empty();

    /** The default noise values for WallDecorators with noise. */
    public static final NoiseSettings DEFAULT_NOISE = NoiseSettings.builder()
        .frequency(0.02f).threshold(Range.of(0.0F)).stretch(1.0f).build();

    public static WallDecoratorSettings from(JsonObject json) {
        return new HjsonMapper<>(DecoratorSettings.Fields.wallDecorators, WallDecoratorSettingsBuilder::build)
            .mapRequiredStateList(Fields.states,  WallDecoratorSettingsBuilder::states)
            .mapObject(Fields.noise, WallDecoratorSettings::copyNoise)
            .mapFloat(Fields.integrity, WallDecoratorSettingsBuilder::integrity)
            .mapRange(Fields.height, WallDecoratorSettingsBuilder::height)
            .mapGenericArray(Fields.directions, v -> Shorthand.assertEnumConstant(v.toString(), Direction.class),
                WallDecoratorSettingsBuilder::directions)
            .mapStateList(Fields.matchers, WallDecoratorSettingsBuilder::matchers)
            .mapEnum(Fields.placement, Placement.class, WallDecoratorSettingsBuilder::placement)
            .create(json, builder());
    }

    private static void copyNoise(final WallDecoratorSettingsBuilder builder, final JsonObject json) {
        builder.noise(full(NoiseSettings.from(json, DEFAULT_NOISE)));
    }

    /** Indicates whether to place blocks inside of or on top of a wall. */
    public enum Placement {
        OVERLAY,
        EMBED
    }
}