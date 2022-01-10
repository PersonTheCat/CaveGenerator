package personthecat.cavegenerator.presets.data;

import com.mojang.serialization.Codec;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.experimental.FieldNameConstants;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import personthecat.catlib.data.Range;
import personthecat.catlib.serialization.EasyStateCodec;
import personthecat.cavegenerator.world.config.ShellConfig;
import personthecat.fastnoise.FastNoise;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static personthecat.catlib.serialization.CodecUtils.autoFlatten;
import static personthecat.catlib.serialization.CodecUtils.codecOf;
import static personthecat.catlib.serialization.CodecUtils.easyList;
import static personthecat.catlib.serialization.CodecUtils.easySet;
import static personthecat.catlib.serialization.FieldDescriptor.field;
import static personthecat.catlib.serialization.FieldDescriptor.nullable;
import static personthecat.catlib.util.Shorthand.map;

@AllArgsConstructor
@FieldNameConstants
public class ShellSettings implements ConfigProvider<ShellSettings, ShellConfig> {
    @Nullable public final Float radius;
    @Nullable public final Integer sphereResolution;
    @Nullable public final Float noiseThreshold;
    @Nullable public final List<Decorator> decorators;

    public static final ShellSettings EMPTY = new ShellSettings(null, null, null, null);

    public static final Codec<ShellSettings> CODEC = codecOf(
        nullable(Codec.FLOAT, Fields.radius, s -> s.radius),
        nullable(Codec.INT, Fields.sphereResolution, s -> s.sphereResolution),
        nullable(Codec.FLOAT, Fields.noiseThreshold, s -> s.noiseThreshold),
        nullable(autoFlatten(Decorator.CODEC), Fields.decorators, s -> s.decorators),
        ShellSettings::new
    );

    @Override
    public Codec<ShellSettings> codec() {
        return CODEC;
    }

    @Override
    public ShellConfig compile(final Random rand, final long seed) {
        final float radius = this.radius != null ? this.radius : 0.0F;
        final int sphereResolution = this.sphereResolution != null ? this.sphereResolution : 2;
        final float noiseThreshold = this.noiseThreshold != null ? this.noiseThreshold : (radius + 0.00001F) / 10.0F;
        final List<Decorator> decorators = this.decorators != null ? this.decorators : Collections.emptyList();
        final List<ShellConfig.Decorator> decoratorConfigs = map(decorators, d -> d.compile(rand, seed));

        return new ShellConfig(radius, sphereResolution, noiseThreshold, decoratorConfigs);
    }

    @AllArgsConstructor
    @FieldNameConstants
    public static class Decorator implements ConfigProvider<Decorator, ShellConfig.Decorator> {
        @NonNull public final List<BlockState> states;
        @Nullable public final Set<BlockState> matchers;
        @Nullable public final Range height;
        @Nullable public final Double integrity;
        @Nullable public final NoiseSettings noise;

        public static final NoiseSettings DEFAULT_NOISE = NoiseSettings.builder()
            .frequency(0.02F).threshold(Range.of(-0.8F)).frequencyY(0.04F).octaves(1).build();

        public static final Codec<NoiseSettings> DEFAULTED_NOISE = NoiseSettings.defaultedNoise(DEFAULT_NOISE);

        public static final Codec<Decorator> CODEC = codecOf(
            field(easyList(EasyStateCodec.INSTANCE), Fields.states, s -> s.states),
            nullable(easySet(EasyStateCodec.INSTANCE), Fields.matchers, s -> s.matchers),
            nullable(Range.CODEC, Fields.height, s -> s.height),
            nullable(Codec.DOUBLE, Fields.integrity, s -> s.integrity),
            nullable(DEFAULTED_NOISE, Fields.noise, s -> s.noise),
            Decorator::new
        );

        @Override
        public Codec<Decorator> codec() {
            return CODEC;
        }

        @Override
        public ShellConfig.Decorator compile(final Random rand, final long seed) {
            final Set<BlockState> matchers = this.matchers != null
                ? this.matchers : Collections.singleton(Blocks.STONE.defaultBlockState());
            final Range height = this.height != null ? this.height : Range.of(0, 63);
            final double integrity = this.integrity != null ? this.integrity : 1.0;
            final FastNoise noise = NoiseSettings.compile(this.noise, rand, seed);

            return new ShellConfig.Decorator(this.states, matchers, height, integrity, noise);
        }
    }
}
