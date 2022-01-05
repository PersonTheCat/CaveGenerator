package personthecat.cavegenerator.presets.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import lombok.Builder;
import lombok.experimental.FieldNameConstants;
import net.minecraft.core.Registry;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import personthecat.catlib.data.Range;
import personthecat.catlib.serialization.EasyStateCodec;
import personthecat.cavegenerator.presets.validator.PillarValidator;
import personthecat.cavegenerator.world.config.ConditionConfig;
import personthecat.cavegenerator.world.config.PillarConfig;

import javax.annotation.Nullable;

import java.util.Objects;
import java.util.Random;

import static personthecat.catlib.serialization.CodecUtils.dynamic;
import static personthecat.catlib.serialization.DynamicField.extend;
import static personthecat.catlib.serialization.DynamicField.field;
import static personthecat.catlib.serialization.DynamicField.required;

@Builder(toBuilder = true)
@FieldNameConstants
public class PillarSettings implements ConfigProvider<PillarSettings, PillarConfig> {
    @Nullable public final ConditionSettings conditions;
    @Nullable public final BlockState state;
    @Nullable public final Integer count;
    @Nullable public final Range length;
    @Nullable public final StairBlock stairBlock;

    private static final ConditionSettings DEFAULT_CONDITIONS =
        ConditionSettings.builder().height(Range.of(10, 50)).build();

    private static final Codec<StairBlock> STAIR_CODEC = Registry.BLOCK.flatXmap(
        b -> b instanceof StairBlock ? DataResult.success((StairBlock) b) : DataResult.error("Not a stair block"),
        DataResult::success
    );

    public static final Codec<PillarSettings> CODEC = dynamic(PillarSettings::builder, PillarSettingsBuilder::build).create(
        required(EasyStateCodec.INSTANCE, Fields.state, s -> s.state, (s, b) -> s.state = b),
        extend(ConditionSettings.CODEC, Fields.conditions, s -> s.conditions, (s, c) -> s.conditions = c),
        field(Codec.INT, Fields.count, s -> s.count, (s, c) -> s.count = c),
        field(Range.CODEC, Fields.length, s -> s.length, (s, l) -> s.length = l),
        field(STAIR_CODEC, Fields.stairBlock, s -> s.stairBlock, (s, b) -> s.stairBlock = b)
    ).flatXmap(PillarValidator::apply, DataResult::success);

    @Override
    public Codec<PillarSettings> codec() {
        return CODEC;
    }

    @Override
    public PillarSettings withOverrides(final OverrideSettings o) {
        if (this.conditions == null) return this;
        return this.toBuilder().conditions(this.conditions.withOverrides(o)).build();
    }

    @Override
    public PillarConfig compile(final Random rand, final long seed) {
        Objects.requireNonNull(this.state, "State not populated by codec");
        final ConditionSettings conditionsCfg = this.conditions != null ? this.conditions : ConditionSettings.EMPTY;
        final int count = this.count != null ? this.count : 15;
        final Range length = this.length != null ? this.length : Range.of(5, 12);
        final ConditionConfig conditions = conditionsCfg.withDefaults(DEFAULT_CONDITIONS).compile(rand, seed);

        return new PillarConfig(conditions, this.state, count, length, this.stairBlock);
    }
}
