package com.personthecat.cavegenerator.data;

import com.personthecat.cavegenerator.model.Direction;
import com.personthecat.cavegenerator.model.Range;
import com.personthecat.cavegenerator.util.HjsonMapper;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import org.hjson.JsonObject;

import java.util.*;

import static com.personthecat.cavegenerator.util.CommonMethods.empty;
import static com.personthecat.cavegenerator.util.CommonMethods.find;
import static com.personthecat.cavegenerator.util.CommonMethods.full;
import static com.personthecat.cavegenerator.util.CommonMethods.runExF;

@FieldNameConstants
@AllArgsConstructor
@Builder(toBuilder = true)
@FieldDefaults(level = AccessLevel.PUBLIC, makeFinal = true)
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class WallDecoratorSettings {

    /** The name of this feature to be used globally in serialization. */
    private static final String FEATURE_NAME = DecoratorSettings.Fields.wallDecorators;

    /** The blocks used for decorating walls. */
    List<IBlockState> states;

    /** The 0-1 chance that any block will be placed. */
    @Default double chance = 1.0;

    /** Height bounds for this decorator. */
    @Default Range height = Range.of(10, 50);

    /** A list of directions to place blocks. */
    @Default List<Direction> directions = Collections.singletonList(Direction.ALL);

    /** A list of blocks to check for. */
    @Default List<IBlockState> matchers = Collections.singletonList(Blocks.STONE.getDefaultState());

    /** Whether to place <b>on</b> the wall or <b>in</b> the wall. */
    @Default Preference preference = Preference.REPLACE_MATCH;

    /** Optional noise generator used determine valid placements. */
    @Default Optional<NoiseSettings> noise = empty();

    /** The default noise values for WallDecorators with noise. */
    public static final NoiseSettings DEFAULT_NOISE = NoiseSettings.builder()
        .frequency(0.02f).scale(0.5f).scaleY(1.0f).build();

    public static WallDecoratorSettings from(JsonObject json) {
        final WallDecoratorSettingsBuilder builder = builder();
        return new HjsonMapper(json)
            .mapRequiredStateList(Fields.states, FEATURE_NAME, builder::states)
            .mapObject(Fields.noise, o -> copyNoise(o, builder))
            .mapFloat(Fields.chance, builder::chance)
            .mapRange(Fields.height, builder::height)
            .mapDirectionList(Fields.directions, builder::directions)
            .mapStateList(Fields.matchers, builder::matchers)
            .mapPreference(Fields.preference, builder::preference)
            .release(builder::build);
    }

    private static void copyNoise(JsonObject json, WallDecoratorSettingsBuilder builder) {
        builder.noise(full(NoiseSettings.from(json, DEFAULT_NOISE)));
    }

    /**
     * Indicates whether to place blocks inside of or on top of a wall. As much as I
     * would love to rename these, I do think it's a little too late.
     */
    public enum Preference {
        REPLACE_ORIGINAL,
        REPLACE_MATCH;

        public static Preference from(final String s) {
            final Optional<Preference> pref = find(values(), (v) -> v.toString().equalsIgnoreCase(s));
            return pref.orElseThrow(() -> {
                final String o = Arrays.toString(values());
                return runExF("Error: Preference \"{}\" does not exist. The following are valid options:\n\n{}", s, o);
            });
        }
    }
}