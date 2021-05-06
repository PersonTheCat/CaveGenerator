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

    public static ShellSettings from(JsonObject json) {
        final ShellSettingsBuilder builder = builder();
        return new HjsonMapper(json)
            .mapFloat(Fields.radius, builder::radius)
            .mapInt(Fields.sphereResolution, builder::sphereResolution)
            .mapFloat(Fields.noiseThreshold, f -> builder.noiseThreshold(full(f)))
            .mapArray(Fields.decorators, Decorator::from, builder::decorators)
            .release(builder::build);
    }

    @Builder
    @FieldNameConstants
    @RequiredArgsConstructor
    @FieldDefaults(level = AccessLevel.PUBLIC, makeFinal = true)
    public static class Decorator {

        /** A list of blocks to attempt spawning when these conditions are met. */
        List<IBlockState> states;

        /** A list of blocks to search for before this feature will generate. */
        @Default List<IBlockState> matchers = Collections.singletonList(Blocks.STONE.getDefaultState());

        /** The height criterion for spawning these features. */
        @Default Range height = Range.of(0, 63);

        /** The 0-1 chance of any single block spawning successfully. */
        @Default double integrity = 1.0;

        /** 3-dimensional noise parameters for spawning this feature. */
        @Default Optional<NoiseSettings> noise = empty();

        /** The default noise values for shell decorators with noise. */
        public static final NoiseSettings DEFAULT_NOISE = NoiseSettings.builder()
            .frequency(0.02f).threshold(Range.of(-0.8F)).stretch(1.0f).octaves(1).build();

        private static Decorator from(JsonObject json) {
            final DecoratorBuilder builder = builder();
            return new HjsonMapper(json)
                .mapRequiredStateList(Fields.states, ShellSettings.Fields.decorators, builder::states)
                .mapStateList(Fields.matchers, builder::matchers)
                .mapRange(Fields.height, builder::height)
                .mapFloat(Fields.integrity, builder::integrity)
                .mapObject(Fields.noise, o -> copyNoise(o, builder))
                .release(builder::build);
        }

        private static void copyNoise(JsonObject json, DecoratorBuilder builder) {
            builder.noise(full(NoiseSettings.from(json, DEFAULT_NOISE)));
        }
    }
}
