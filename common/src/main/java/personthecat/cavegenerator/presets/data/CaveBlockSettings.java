package personthecat.cavegenerator.presets.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldNameConstants;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import personthecat.catlib.data.Range;
import personthecat.catlib.serialization.EasyStateCodec;
import personthecat.cavegenerator.presets.validator.CaveBlockValidator;
import personthecat.cavegenerator.world.config.CaveBlockConfig;

import java.util.List;
import java.util.Objects;
import java.util.Random;

import static personthecat.catlib.serialization.CodecUtils.codecOf;
import static personthecat.catlib.serialization.CodecUtils.easyList;
import static personthecat.catlib.serialization.FieldDescriptor.field;
import static personthecat.catlib.serialization.FieldDescriptor.nullable;

@AllArgsConstructor
@FieldNameConstants
public class CaveBlockSettings implements ConfigProvider<CaveBlockSettings, CaveBlockConfig> {
    @Nullable public final List<BlockState> states;
    @Nullable public final Double integrity;
    @Nullable public final Range height;
    @Nullable public final NoiseSettings noise;

    public static final NoiseSettings DEFAULT_NOISE =
        NoiseSettings.builder().frequency(0.02F).threshold(Range.of(-0.8F)).frequencyY(0.04F).octaves(1).build();

    public static final Codec<NoiseSettings> DEFAULTED_NOISE = NoiseSettings.defaultedNoise(DEFAULT_NOISE);

    public static final Codec<CaveBlockSettings> CODEC = codecOf(
        field(easyList(EasyStateCodec.INSTANCE), Fields.states, s -> s.states),
        nullable(Codec.DOUBLE, Fields.integrity, s -> s.integrity),
        nullable(Range.CODEC, Fields.height, s -> s.height),
        nullable(DEFAULTED_NOISE, Fields.noise, s -> s.noise),
        CaveBlockSettings::new
    ).flatXmap(CaveBlockValidator::apply, DataResult::success);

    @Override
    public Codec<CaveBlockSettings> codec() {
        return CODEC;
    }

    @Override
    public CaveBlockConfig compile(final Random rand, final long seed) {
        return new CaveBlockConfig(
            Objects.requireNonNull(this.states, "States not populated in codec"),
            this.integrity != null ? this.integrity : 1.0,
            this.height != null ? this.height : Range.of(0, 50),
            NoiseSettings.compile(this.noise != null ? this.noise : DEFAULT_NOISE, rand, seed)
        );
    }
}