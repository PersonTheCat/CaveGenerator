package personthecat.cavegenerator.presets.data;

import com.mojang.serialization.Codec;
import lombok.Builder;
import lombok.experimental.FieldNameConstants;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import personthecat.catlib.data.InvertibleSet;
import personthecat.catlib.data.Range;
import personthecat.catlib.serialization.EasyStateCodec;
import personthecat.cavegenerator.world.config.ClusterConfig;

import java.util.*;

import static personthecat.catlib.util.Shorthand.coalesce;
import static personthecat.catlib.util.Shorthand.map;
import static personthecat.catlib.serialization.CodecUtils.dynamic;
import static personthecat.catlib.serialization.CodecUtils.easyList;
import static personthecat.catlib.serialization.CodecUtils.easySet;
import static personthecat.catlib.serialization.DynamicField.extend;
import static personthecat.catlib.serialization.DynamicField.field;
import static personthecat.catlib.serialization.DynamicField.required;

@Builder(toBuilder = true)
@FieldNameConstants
public class ClusterSettings implements ConfigProvider<ClusterSettings, ClusterConfig> {
    @Nullable public final ConditionSettings conditions;
    @Nullable public final List<BlockState> states;
    @Nullable public final Double chance;
    @Nullable public final Double integrity;
    @Nullable public final Range radiusX;
    @Nullable public final Range radiusY;
    @Nullable public final Range radiusZ;
    @Nullable public final Range radius;
    @Nullable public final Range centerHeight;
    @Nullable public final Set<BlockState> matchers;
    @Nullable public final Boolean spawnInAir;

    public static final Codec<ClusterSettings> CODEC = dynamic(ClusterSettings::builder, ClusterSettingsBuilder::build).create(
        extend(ConditionSettings.CODEC, Fields.conditions, s -> s.conditions, (s, c) -> s.conditions = c),
        required(easyList(EasyStateCodec.INSTANCE), Fields.states, s -> s.states, (s, b) -> s.states = b),
        field(Codec.DOUBLE, Fields.chance, s -> s.chance, (s, c) -> s.chance = c),
        field(Codec.DOUBLE, Fields.integrity, s -> s.integrity, (s, i) -> s.integrity = i),
        field(Range.CODEC, Fields.radiusX, s -> s.radiusX, (s, r) -> s.radiusX = r),
        field(Range.CODEC, Fields.radiusY, s -> s.radiusY, (s, r) -> s.radiusY = r),
        field(Range.CODEC, Fields.radiusZ, s -> s.radiusZ, (s, r) -> s.radiusZ = r),
        field(Range.CODEC, Fields.radius, s -> s.radius, (s, r) -> s.radius = r),
        field(Range.CODEC, Fields.centerHeight, s -> s.centerHeight, (s, h) -> s.centerHeight = h),
        field(easySet(EasyStateCodec.INSTANCE), Fields.matchers, s -> s.matchers, (s, m) -> s.matchers = m),
        field(Codec.BOOL, Fields.spawnInAir, s -> s.spawnInAir, (s, a) -> s.spawnInAir = a)
    );

    @Override
    public Codec<ClusterSettings> codec() {
        return CODEC;
    }

    @Override
    public ClusterSettings withOverrides(final OverrideSettings o) {
        if (this.conditions == null) return this;
        return this.toBuilder().conditions(this.conditions.withOverrides(o)).build();
    }

    @Override
    public ClusterConfig compile(final Random rand, final long seed) {
        Objects.requireNonNull(this.states, "States not populated in codec.");
        return new ClusterConfig(
            coalesce(this.conditions, ConditionSettings.EMPTY).compile(rand, seed),
            map(this.states, s -> Pair.of(s, Block.getId(s))),
            (1.0 - coalesce(this.chance, 0.15)) * 92.0,
            coalesce(this.chance, 0.15),
            coalesce(this.integrity, 1.0),
            coalesce(this.radiusX, this.radius, Range.of(13, 19)),
            coalesce(this.radiusY, this.radius, Range.of(9, 15)),
            coalesce(this.radiusZ, this.radius, Range.of(13, 19)),
            coalesce(this.centerHeight, Range.of(24, 40)),
            new InvertibleSet<>(coalesce(this.matchers, Collections.emptySet()), false).optimize(Collections.emptyList()),
            this.spawnInAir != null ? this.spawnInAir : false
        );
    }
}