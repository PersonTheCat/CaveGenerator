package personthecat.cavegenerator.presets.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import lombok.Builder;
import lombok.experimental.FieldNameConstants;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import personthecat.catlib.data.InvertibleSet;
import personthecat.catlib.data.Range;
import personthecat.catlib.serialization.EasyStateCodec;
import personthecat.cavegenerator.presets.validator.StalactiteValidator;
import personthecat.cavegenerator.world.config.ConditionConfig;
import personthecat.cavegenerator.world.config.StalactiteConfig;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Random;
import java.util.Set;

import static personthecat.catlib.serialization.CodecUtils.dynamic;
import static personthecat.catlib.serialization.CodecUtils.easySet;
import static personthecat.catlib.serialization.CodecUtils.ofEnum;
import static personthecat.catlib.serialization.DynamicField.extend;
import static personthecat.catlib.serialization.DynamicField.field;

@Builder(toBuilder = true)
@FieldNameConstants
public class StalactiteSettings implements ConfigProvider<StalactiteSettings, StalactiteConfig> {
    @Nullable public final ConditionSettings conditions;
    @Nullable public final BlockState state;
    @Nullable public final Type type;
    @Nullable public final Size size;
    @Nullable public final Double chance;
    @Nullable public final Range length;
    @Nullable public final Integer space;
    @Nullable public final Boolean symmetrical;
    @Nullable public final Set<BlockState> matchers;

    public static final NoiseSettings DEFAULT_REGION = NoiseSettings.builder()
        .frequency(0.025f).threshold(Range.of(-0.425F)).build();

    private static final ConditionSettings DEFAULT_CONDITIONS =
        ConditionSettings.builder().height(Range.of(11, 55)).build();

    public static final Codec<StalactiteSettings> CODEC = dynamic(StalactiteSettings::builder, StalactiteSettingsBuilder::build).create(
        extend(ConditionSettings.CODEC, Fields.conditions, s -> s.conditions, (s, c) -> s.conditions = c),
        field(EasyStateCodec.INSTANCE, Fields.state, s -> s.state, (s, b) -> s.state = b),
        field(Type.CODEC, Fields.type, s -> s.type, (s, t) -> s.type = t),
        field(Size.CODEC, Fields.size, s -> s.size, (s, z) -> s.size = z),
        field(Codec.DOUBLE, Fields.chance, s -> s.chance, (s, c) -> s.chance = c),
        field(Range.CODEC, Fields.length, s -> s.length, (s, l) -> s.length = l),
        field(Codec.INT, Fields.space, s -> s.space, (s, p) -> s.space = p),
        field(Codec.BOOL, Fields.symmetrical, s -> s.symmetrical, (s, m) -> s.symmetrical = m),
        field(easySet(EasyStateCodec.INSTANCE), Fields.matchers, s -> s.matchers, (s, m) -> s.matchers = m)
    ).flatXmap(StalactiteValidator::apply, DataResult::success);

    @Override
    public Codec<StalactiteSettings> codec() {
        return CODEC;
    }

    @Override
    public StalactiteSettings withOverrides(final OverrideSettings o) {
        if (this.conditions == null) return this;
        return this.toBuilder().conditions(this.conditions.withOverrides(o)).build();
    }

    @Override
    public StalactiteConfig compile(final Random rand, final long seed) {
        final ConditionSettings conditionsCfg = this.conditions != null ? this.conditions : ConditionSettings.EMPTY;
        final BlockState state = this.state != null ? this.state : Blocks.STONE.defaultBlockState();
        final Type type = this.type != null ? this.type : Type.STALACTITE;
        final Size size = this.size != null ? this.size : Size.MEDIUM;
        final double chance = this.chance != null ? this.chance : 0.167;
        final Range length = this.length != null ? this.length : Range.of(3, 5);
        final int space = this.space != null ? this.space : 3;
        final boolean symmetrical = this.symmetrical != null ? this.symmetrical : true;
        final Set<BlockState> matchersCfg = this.matchers != null ? this.matchers : Collections.emptySet();

        final ConditionConfig conditions = conditionsCfg
            .withDefaults(DEFAULT_CONDITIONS).withDefaultRegion(DEFAULT_REGION).compile(rand, seed);
        final Set<BlockState> matchers =
            new InvertibleSet<>(matchersCfg, false).optimize(Collections.emptyList());

        return new StalactiteConfig(conditions, state, type, size, chance, length, space, symmetrical, matchers);
    }

    public enum Type {
        STALAGMITE,
        STALACTITE,
        SPELEOTHEM;

        public static final Codec<Type> CODEC = ofEnum(Type.class);
    }

    public enum Size {
        SMALL,
        MEDIUM,
        LARGE,
        GIANT;

        public static final Codec<Size> CODEC = ofEnum(Size.class);
    }
}