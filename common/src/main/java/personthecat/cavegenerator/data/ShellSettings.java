package personthecat.cavegenerator.data;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.RequiredArgsConstructor;
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
public class ShellSettings {

    /** The radius of blocks to decorate when a guaranteed number of blocks is possible. */
    @Default double radius = 0.0;

    /** The 1 / x chance that a sphere will have a shell. */
    @Default int sphereResolution = 2;

    /** The threshold-based distance used whenever a guaranteed number is not possible. */
    @Default Optional<Float> noiseThreshold = empty();

    /** A list of various blocks and conditions for placing those blocks. */
    @Default List<Decorator> decorators = Collections.emptyList();

    public static ShellSettings from(final JsonObject json) {
        return new HjsonMapper<>(DecoratorSettings.Fields.shell, ShellSettingsBuilder::build)
            .mapFloat(Fields.radius, ShellSettingsBuilder::radius)
            .mapInt(Fields.sphereResolution, ShellSettingsBuilder::sphereResolution)
            .mapFloat(Fields.noiseThreshold, (b, f) -> b.noiseThreshold(full(f)))
            .mapArray(Fields.decorators, Decorator::from, ShellSettingsBuilder::decorators)
            .create(json, builder());
    }

    @Builder
    @FieldNameConstants
    @RequiredArgsConstructor
    @FieldDefaults(level = AccessLevel.PUBLIC, makeFinal = true)
    public static class Decorator {

        /** A list of blocks to attempt spawning when these conditions are met. */
        List<BlockState> states;

        /** A list of blocks to search for before this feature will generate. */
        @Default List<BlockState> matchers = Collections.singletonList(Blocks.STONE.defaultBlockState());

        /** The height criterion for spawning these features. */
        @Default Range height = Range.of(0, 63);

        /** The 0-1 chance of any single block spawning successfully. */
        @Default double integrity = 1.0;

        /** 3-dimensional noise parameters for spawning this feature. */
        @Default Optional<NoiseSettings> noise = empty();

        /** The default noise values for shell decorators with noise. */
        public static final NoiseSettings DEFAULT_NOISE = NoiseSettings.builder()
            .frequency(0.02f).threshold(Range.of(-0.8F)).stretch(1.0f).octaves(1).build();

        private static Decorator from(final JsonObject json) {
            return new HjsonMapper<>(ShellSettings.Fields.decorators, DecoratorBuilder::build)
                .mapRequiredStateList(Fields.states, DecoratorBuilder::states)
                .mapStateList(Fields.matchers, DecoratorBuilder::matchers)
                .mapRange(Fields.height, DecoratorBuilder::height)
                .mapFloat(Fields.integrity, DecoratorBuilder::integrity)
                .mapObject(Fields.noise, Decorator::copyNoise)
                .create(json, builder());
        }

        private static void copyNoise(final DecoratorBuilder builder, final JsonObject json) {
            builder.noise(full(NoiseSettings.from(json, DEFAULT_NOISE)));
        }
    }
}
