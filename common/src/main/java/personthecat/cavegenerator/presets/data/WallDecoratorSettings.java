package personthecat.cavegenerator.presets.data;

import com.mojang.serialization.Codec;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldNameConstants;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import personthecat.catlib.data.InvertibleSet;
import personthecat.catlib.data.Range;
import personthecat.catlib.serialization.EasyStateCodec;
import personthecat.cavegenerator.model.Direction;
import personthecat.cavegenerator.world.config.WallDecoratorConfig;
import personthecat.fastnoise.FastNoise;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static personthecat.catlib.serialization.CodecUtils.codecOf;
import static personthecat.catlib.serialization.CodecUtils.easyList;
import static personthecat.catlib.serialization.CodecUtils.easySet;
import static personthecat.catlib.serialization.CodecUtils.ofEnum;
import static personthecat.catlib.serialization.FieldDescriptor.field;
import static personthecat.catlib.serialization.FieldDescriptor.nullable;

@AllArgsConstructor
@FieldNameConstants
public class WallDecoratorSettings {
    @Nullable public final List<BlockState> states;
    @Nullable public final Double integrity;
    @Nullable public final Range height;
    @Nullable public final List<Direction> directions;
    @Nullable public final Set<BlockState> matchers;
    @Nullable public final Placement placement;
    @Nullable public final NoiseSettings noise;

    public static final NoiseSettings DEFAULT_NOISE = NoiseSettings.builder()
        .frequency(0.02F).threshold(Range.of(0.0F)).frequencyY(0.04F).build();

    public static final Codec<NoiseSettings> DEFAULTED_NOISE = NoiseSettings.defaultedNoise(DEFAULT_NOISE);

    public static final Codec<WallDecoratorSettings> CODEC = codecOf(
        field(easyList(EasyStateCodec.INSTANCE), Fields.states, s -> s.states),
        nullable(Codec.DOUBLE, Fields.integrity, s -> s.integrity),
        nullable(Range.CODEC, Fields.height, s -> s.height),
        nullable(easyList(Direction.CODEC), Fields.directions, s -> s.directions),
        nullable(easySet(EasyStateCodec.INSTANCE), Fields.matchers, s -> s.matchers),
        nullable(Placement.CODEC, Fields.placement, s -> s.placement),
        nullable(DEFAULTED_NOISE, Fields.noise, s -> s.noise),
        WallDecoratorSettings::new
    );

    public WallDecoratorConfig compile(final Random rand, final long seed) {
        final double integrity = this.integrity != null ? this.integrity : 1.0;
        final Range height = this.height != null ? this.height : Range.of(10, 50);
        final List<Direction> directions = this.directions != null
            ? this.directions : Collections.singletonList(Direction.ALL);
        final Set<BlockState> matchersCfg = this.matchers != null
            ? this.matchers : Collections.singleton(Blocks.STONE.defaultBlockState());
        final Set<BlockState> matchers =
            new InvertibleSet<>(matchersCfg, true).optimize(Collections.emptyList());
        final Placement placement = this.placement != null ? this.placement : Placement.EMBED;
        final NoiseSettings noiseCfg = this.noise != null ? this.noise : DEFAULT_NOISE;
        final FastNoise noise = NoiseSettings.compile(noiseCfg, rand, seed);

        return new WallDecoratorConfig(this.states, integrity, height, directions, matchers, placement, noise);
    }

    /** Indicates whether to place blocks inside or on top of a wall. */
    public enum Placement {
        OVERLAY,
        EMBED;

        public static Codec<Placement> CODEC = ofEnum(Placement.class);
    }
}