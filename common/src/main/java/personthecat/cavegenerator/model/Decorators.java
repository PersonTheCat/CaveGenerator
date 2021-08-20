package personthecat.cavegenerator.model;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.experimental.FieldDefaults;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import personthecat.cavegenerator.presets.data.DecoratorSettings;

import java.util.*;
import java.util.function.Predicate;

import static personthecat.catlib.util.Shorthand.map;

@Builder(toBuilder = true)
@FieldDefaults(level = AccessLevel.PUBLIC, makeFinal = true)
public class Decorators {

    /** Determines whether this carver can replace any given block. */
    @Default Predicate<BlockState> canReplace = s -> !Blocks.WATER.defaultBlockState().equals(s);

    /** Determines which block to place at the given coordinates. */
    @Default List<ConfiguredCaveBlock> caveBlocks = Collections.emptyList();

    /** Determines which blocks to place to the side of the given coordinates. */
    @Default WallDecoratorMap wallMap = WallDecoratorMap.builder().build();

    /** Determines which blocks to place in the shape of a pond below this feature. */
    @Default List<ConfiguredPond> ponds = Collections.emptyList();

    /** Determines which blocks to place surrounding the current feature. */
    @Default ConfiguredShell shell = ConfiguredShell.EMPTY_SHELL;

    public static Decorators compile(final DecoratorSettings settings, final Random rand, final long seed) {
        return builder()
            .canReplace(compileCanReplace(settings))
            .caveBlocks(map(settings.caveBlocks, b -> new ConfiguredCaveBlock(b, rand, seed)))
            .wallMap(WallDecoratorMap.sort(settings.wallDecorators, rand, seed))
            .ponds(map(settings.ponds, p -> new ConfiguredPond(p, rand, seed)))
            .shell(new ConfiguredShell(settings.shell, rand, seed))
            .build();
    }

    // This would ideally be adapted to check for other *kinds* of block features.
    private static Predicate<BlockState> compileCanReplace(final DecoratorSettings settings) {
        final Set<BlockState> replaceable = new HashSet<>(settings.replaceableBlocks);
        if (replaceable.isEmpty()) {
            return s -> !s.getBlock().equals(Blocks.BEDROCK);
        } else if (settings.replaceSolidBlocks) {
            return s -> s.getMaterial().isSolid() && !s.getBlock().equals(Blocks.BEDROCK) ;
        } else if (settings.replaceDecorators) {
            replaceable.addAll(settings.globalDecorators);
            settings.caveBlocks.forEach(c -> replaceable.addAll(c.states));
            settings.wallDecorators.forEach(w -> replaceable.addAll(w.states));
            settings.ponds.forEach(p -> replaceable.addAll(p.states));
            settings.shell.decorators.forEach(s -> replaceable.addAll(s.states));
        }
        if (replaceable.size() == 1) {
            final BlockState state = replaceable.iterator().next();
            return state::equals;
        }
        return replaceable::contains;
    }
}
