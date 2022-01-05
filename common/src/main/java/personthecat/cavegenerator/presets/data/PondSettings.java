package personthecat.cavegenerator.presets.data;

import com.mojang.serialization.Codec;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldNameConstants;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import personthecat.catlib.data.Range;
import personthecat.catlib.serialization.EasyStateCodec;
import personthecat.cavegenerator.world.config.PondConfig;
import personthecat.fastnoise.FastNoise;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static personthecat.catlib.serialization.CodecUtils.codecOf;
import static personthecat.catlib.serialization.CodecUtils.easyList;
import static personthecat.catlib.serialization.CodecUtils.easySet;
import static personthecat.catlib.serialization.FieldDescriptor.field;
import static personthecat.catlib.serialization.FieldDescriptor.nullable;

@AllArgsConstructor
@FieldNameConstants
public class PondSettings {
    @NotNull public final List<BlockState> states;
    @Nullable public final Double integrity;
    @Nullable public final Range height;
    @Nullable public final Integer depth;
    @Nullable public final Set<BlockState> matchers;
    @Nullable public final NoiseSettings noise;

    public static final NoiseSettings DEFAULT_NOISE = NoiseSettings.builder()
        .frequency(0.02F).threshold(Range.of(0.0F)).frequencyY(0.04F).build();

    public static final Codec<NoiseSettings> DEFAULTED_NOISE = NoiseSettings.defaultedNoise(DEFAULT_NOISE);

    public static final Codec<PondSettings> CODEC = codecOf(
        field(easyList(EasyStateCodec.INSTANCE), Fields.states, s -> s.states),
        nullable(Codec.DOUBLE, Fields.integrity, s -> s.integrity),
        nullable(Range.CODEC, Fields.height, s -> s.height),
        nullable(Codec.INT, Fields.depth, s -> s.depth),
        nullable(easySet(EasyStateCodec.INSTANCE), Fields.matchers, s -> s.matchers),
        nullable(DEFAULTED_NOISE, Fields.noise, s -> s.noise),
        PondSettings::new
    );

    public PondConfig compile(final Random rand, final long seed) {
        final double integrity = this.integrity != null ? this.integrity : 1.0;
        final Range height = this.height != null ? this.height : Range.of(10, 50);
        final int depth = this.depth != null ? this.depth : 2;
        final Set<BlockState> matchers = this.matchers != null
            ? this.matchers : Collections.singleton(Blocks.STONE.defaultBlockState());
        final FastNoise noise = NoiseSettings.compile(this.noise, rand, seed);

        return new PondConfig(this.states, integrity, height, depth, matchers, noise);
    }
}
