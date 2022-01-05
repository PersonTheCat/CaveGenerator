package personthecat.cavegenerator.presets.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import lombok.Builder;
import lombok.experimental.FieldNameConstants;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import personthecat.catlib.data.InvertibleSet;
import personthecat.catlib.data.Range;
import personthecat.catlib.serialization.EasyStateCodec;
import personthecat.cavegenerator.presets.validator.LayerValidator;
import personthecat.cavegenerator.world.config.LayerConfig;

import java.util.Collections;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

import static personthecat.catlib.serialization.CodecUtils.dynamic;
import static personthecat.catlib.serialization.CodecUtils.easySet;
import static personthecat.catlib.serialization.DynamicField.extend;
import static personthecat.catlib.serialization.DynamicField.field;
import static personthecat.catlib.serialization.DynamicField.required;

@Builder(toBuilder = true)
@FieldNameConstants
public class LayerSettings implements ConfigProvider<LayerSettings, LayerConfig> {
    @Nullable public final ConditionSettings conditions;
    @Nullable public final BlockState state;
    @Nullable public final Set<BlockState> matchers;

    private static final NoiseSettings DEFAULT_NOISE =
        NoiseSettings.builder().frequency(0.015f).range(Range.of(-7, 7)).build();
    private static final ConditionSettings DEFAULT_CONDITIONS =
        ConditionSettings.builder().height(Range.of(0, 20)).ceiling(DEFAULT_NOISE).build();

    public static final Codec<LayerSettings> CODEC = dynamic(LayerSettings::builder, LayerSettingsBuilder::build).create(
        extend(ConditionSettings.CODEC, Fields.conditions, d -> d.conditions, (d, c) -> d.conditions = c),
        required(EasyStateCodec.INSTANCE, Fields.state, d -> d.state, (d, s) -> d.state = s),
        field(easySet(EasyStateCodec.INSTANCE), Fields.matchers, s -> s.matchers, (s, m) -> s.matchers = m)
    ).flatXmap(LayerValidator::apply, DataResult::success);

    public Codec<LayerSettings> codec() {
        return CODEC;
    }

    public LayerSettings withOverrides(final OverrideSettings o) {
        if (this.conditions == null) return this;
        return this.toBuilder().conditions(this.conditions.withOverrides(o)).build();
    }

    public LayerConfig compile(final Random rand, final long seed) {
        Objects.requireNonNull(this.state, "State not populated by codec");
        final ConditionSettings conditions = this.conditions != null ? this.conditions : ConditionSettings.EMPTY;
        final Set<BlockState> matchersCfg = this.matchers != null
            ? this.matchers : Collections.singleton(Blocks.STONE.defaultBlockState());
        final Set<BlockState> matchers = new InvertibleSet<>(matchersCfg, false).optimize(Collections.emptyList());

        return new LayerConfig(conditions.compile(rand, seed), this.state, matchers);
    }
}
