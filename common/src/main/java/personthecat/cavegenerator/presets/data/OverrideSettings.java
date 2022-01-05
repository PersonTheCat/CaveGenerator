package personthecat.cavegenerator.presets.data;

import com.mojang.serialization.Codec;
import lombok.Builder;
import lombok.With;
import lombok.experimental.FieldNameConstants;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import personthecat.catlib.data.BiomePredicate;
import personthecat.catlib.data.DimensionPredicate;
import personthecat.catlib.data.Range;
import personthecat.catlib.serialization.EasyStateCodec;

import java.util.Collections;
import java.util.List;

import static personthecat.catlib.serialization.CodecUtils.dynamic;
import static personthecat.catlib.serialization.CodecUtils.easyList;
import static personthecat.catlib.serialization.DynamicField.field;

/**
 * Any settings that can be written at the top level to serve as default values.
 *
 * Todo: should this be handled directly in the JSON. (?)
 */
@Builder
@FieldNameConstants
public class OverrideSettings {
    @Nullable public final BiomePredicate biomes;
    @Nullable public final DimensionPredicate dimensions;
    @Nullable public final Range height;
    @Nullable public final NoiseSettings floor;
    @Nullable public final NoiseSettings ceiling;
    @Nullable public final NoiseSettings region;
    @Nullable public final NoiseSettings noise;
    @Nullable public final List<BlockState> replaceableBlocks;
    @Nullable public final Boolean replaceDecorators;
    @Nullable public final Boolean replaceSolidBlocks;
    @Nullable public final List<CaveBlockSettings> caveBlocks;
    @Nullable public final List<WallDecoratorSettings> wallDecorators;
    @Nullable public final List<PondSettings> ponds;
    @Nullable public final ShellSettings shell;
    @Nullable public final TunnelSettings branches;
    @Nullable public final RoomSettings rooms;

    @With @NotNull public final List<BlockState> globalDecorators;

    public static final Codec<OverrideSettings> CODEC =
        dynamic(OverrideSettings::builder, OverrideSettingsBuilder::build).create(
            field(BiomePredicate.CODEC, Fields.biomes, s -> s.biomes, (s, b) -> s.biomes = b),
            field(DimensionPredicate.CODEC, Fields.dimensions, s -> s.dimensions, (s, d) -> s.dimensions = d),
            field(Range.CODEC, Fields.height, s -> s.height, (s, h) -> s.height = h),
            field(NoiseSettings.MAP, Fields.floor, s -> s.floor, (s, f) -> s.floor = f),
            field(NoiseSettings.REGION, Fields.region, s -> s.region, (s, r) -> s.region = r),
            field(NoiseSettings.NOISE, Fields.noise, s -> s.noise, (s, n) -> s.noise = n),
            field(easyList(EasyStateCodec.INSTANCE), Fields.replaceableBlocks, s -> s.replaceableBlocks, (s, b) -> s.replaceableBlocks = b),
            field(Codec.BOOL, Fields.replaceDecorators, s -> s.replaceDecorators, (s, r) -> s.replaceDecorators = r),
            field(Codec.BOOL, Fields.replaceSolidBlocks, s -> s.replaceSolidBlocks, (s, r) -> s.replaceSolidBlocks = r),
            field(easyList(CaveBlockSettings.CODEC), Fields.caveBlocks, s -> s.caveBlocks, (s, c) -> s.caveBlocks = c),
            field(easyList(WallDecoratorSettings.CODEC), Fields.wallDecorators, s -> s.wallDecorators, (s, w) -> s.wallDecorators = w),
            field(easyList(PondSettings.CODEC), Fields.ponds, s -> s.ponds, (s, p) -> s.ponds = p),
            field(ShellSettings.CODEC, Fields.shell, s -> s.shell, (s, h) -> s.shell = h),
            field(TunnelSettings.CODEC, Fields.branches, s -> s.branches, (s, b) -> s.branches = b),
            field(RoomSettings.CODEC, Fields.rooms, s -> s.rooms, (s, r) -> s.rooms = r)
        );

    public static class OverrideSettingsBuilder {
        OverrideSettingsBuilder() {
            this.globalDecorators = Collections.emptyList();
        }
    }
}
