package personthecat.cavegenerator.presets.data;

import com.mojang.serialization.Codec;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.experimental.FieldNameConstants;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import personthecat.catlib.data.InvertibleSet;
import personthecat.catlib.serialization.EasyStateCodec;
import personthecat.cavegenerator.model.WallDecoratorMap;
import personthecat.cavegenerator.world.config.*;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Predicate;

import static personthecat.catlib.serialization.CodecUtils.autoFlatten;
import static personthecat.catlib.serialization.CodecUtils.codecOf;
import static personthecat.catlib.serialization.CodecUtils.easyList;
import static personthecat.catlib.serialization.FieldDescriptor.nullable;
import static personthecat.catlib.util.Shorthand.map;

@Builder
@AllArgsConstructor
@FieldNameConstants
public class DecoratorSettings {
    @Nullable public final List<BlockState> replaceableBlocks;
    @Nullable public final Boolean replaceDecorators;
    @Nullable public final Boolean replaceSolidBlocks;
    @Nullable public final List<CaveBlockSettings> caveBlocks;
    @Nullable public final List<WallDecoratorSettings> wallDecorators;
    @Nullable public final List<PondSettings> ponds;
    @Nullable public final ShellSettings shell;
    @Nullable public final List<BlockState> globalDecorators;

    public static final DecoratorSettings EMPTY =
        new DecoratorSettings(null, null, null, null, null, null, null, null);

    public static final Codec<DecoratorSettings> CODEC = codecOf(
        nullable(easyList(EasyStateCodec.INSTANCE), Fields.replaceableBlocks, s -> s.replaceableBlocks),
        nullable(Codec.BOOL, Fields.replaceDecorators, s -> s.replaceDecorators),
        nullable(Codec.BOOL, Fields.replaceSolidBlocks, s -> s.replaceSolidBlocks),
        nullable(autoFlatten(CaveBlockSettings.CODEC), Fields.caveBlocks, s -> s.caveBlocks),
        nullable(autoFlatten(WallDecoratorSettings.CODEC), Fields.wallDecorators, s -> s.wallDecorators),
        nullable(autoFlatten(PondSettings.CODEC), Fields.ponds, s -> s.ponds),
        nullable(ShellSettings.CODEC, Fields.shell, s -> s.shell),
        nullable(easyList(EasyStateCodec.INSTANCE), "<globals>", s -> s.globalDecorators),
        DecoratorSettings::new
    );

    public DecoratorSettings withOverrides(final OverrideSettings o) {
        return builder()
            .replaceableBlocks(this.replaceableBlocks != null ? this.replaceableBlocks : o.replaceableBlocks)
            .replaceDecorators(this.replaceDecorators != null ? this.replaceDecorators : o.replaceDecorators)
            .replaceSolidBlocks(this.replaceSolidBlocks != null ? this.replaceSolidBlocks : o.replaceSolidBlocks)
            .caveBlocks(this.caveBlocks != null ? this.caveBlocks : o.caveBlocks)
            .wallDecorators(this.wallDecorators != null ? this.wallDecorators : o.wallDecorators)
            .ponds(this.ponds != null ? this.ponds : o.ponds)
            .shell(this.shell != null ? this.shell : o.shell)
            .globalDecorators(o.globalDecorators)
            .build();
    }

    public DecoratorSettings withDefaults(final DecoratorSettings defaults) {
        return builder()
            .replaceableBlocks(this.replaceableBlocks != null ? this.replaceableBlocks : defaults.replaceableBlocks)
            .replaceDecorators(this.replaceDecorators != null ? this.replaceDecorators : defaults.replaceDecorators)
            .replaceSolidBlocks(this.replaceSolidBlocks != null ? this.replaceSolidBlocks : defaults.replaceSolidBlocks)
            .caveBlocks(this.caveBlocks != null ? this.caveBlocks : defaults.caveBlocks)
            .wallDecorators(this.wallDecorators != null ? this.wallDecorators : defaults.wallDecorators)
            .ponds(this.ponds != null ? this.ponds : defaults.ponds)
            .shell(this.shell != null ? this.shell : defaults.shell)
            .globalDecorators(this.globalDecorators)
            .build();
    }

    public DecoratorConfig compile(final Random rand, final long seed) {
        final List<BlockState> replaceableBlocks = this.replaceableBlocks != null
            ? this.replaceableBlocks : Arrays.asList(Blocks.STONE.defaultBlockState(),
            Blocks.DIRT.defaultBlockState(), Blocks.GRASS_BLOCK.defaultBlockState());
        final boolean replaceDecorators = this.replaceDecorators != null
            ? this.replaceDecorators : true;
        final boolean replaceSolidBlocks = this.replaceSolidBlocks != null
            ? this.replaceSolidBlocks : false;
        final List<CaveBlockSettings> caveBlocksCfg = this.caveBlocks != null
            ? this.caveBlocks : Collections.emptyList();
        final List<WallDecoratorSettings> wallDecoratorsCfg = this.wallDecorators != null
            ? this.wallDecorators : Collections.emptyList();
        final List<PondSettings> pondsCfg = this.ponds != null
            ? this.ponds : Collections.emptyList();
        final ShellSettings shellCfg = this.shell != null
            ? this.shell : ShellSettings.EMPTY;
        final List<BlockState> globalDecorators = this.globalDecorators != null
            ? this.globalDecorators : Collections.emptyList();

        final List<CaveBlockConfig> caveBlocks = map(caveBlocksCfg, c -> c.compile(rand, seed));
        final List<WallDecoratorConfig> wallDecorators = map(wallDecoratorsCfg, d -> d.compile(rand, seed));
        final WallDecoratorMap wallMap = WallDecoratorMap.sort(wallDecorators);
        final List<PondConfig> ponds = map(pondsCfg, p -> p.compile(rand, seed));
        final ShellConfig shell = shellCfg.compile(rand, seed);

        final Predicate<BlockState> canReplace =
            compileCanReplace(replaceableBlocks, replaceDecorators, replaceSolidBlocks, globalDecorators);

        return new DecoratorConfig(canReplace, caveBlocks, wallDecorators, wallMap, ponds, shell, this.globalDecorators);
    }

    private static Predicate<BlockState> compileCanReplace(
        final List<BlockState> replaceableBlocks, final boolean replaceDecorators,
        final boolean replaceSolidBlocks, final List<BlockState> globalDecorators) {

        final Set<BlockState> replaceable = new HashSet<>(replaceableBlocks);
        if (replaceable.isEmpty()) {
            return s -> !s.getBlock().equals(Blocks.BEDROCK);
        }
        if (replaceDecorators) {
            replaceable.addAll(globalDecorators);
        }
        final Set<BlockState> optimized = new InvertibleSet<>(replaceable, false).optimize(Collections.emptyList());
        if (replaceSolidBlocks) {
            return s -> optimized.contains(s) || (s.getMaterial().isSolid() && !s.getBlock().equals(Blocks.BEDROCK));
        }
        return optimized::contains;
    }
}
