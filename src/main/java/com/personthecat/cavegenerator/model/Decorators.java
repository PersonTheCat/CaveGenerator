package com.personthecat.cavegenerator.model;

import com.personthecat.cavegenerator.data.DecoratorSettings;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.experimental.FieldDefaults;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

import java.util.*;
import java.util.function.Predicate;

import static com.personthecat.cavegenerator.util.CommonMethods.map;

@Builder(toBuilder = true)
@FieldDefaults(level = AccessLevel.PUBLIC, makeFinal = true)
public class Decorators {

    /** Determines whether this carver can replace any given block. */
    @Default Predicate<IBlockState> canReplace = s -> !Blocks.WATER.getDefaultState().equals(s);

    /** Determines which block to place at the given coordinates. */
    @Default List<ConfiguredCaveBlock> caveBlocks = Collections.emptyList();

    /** Determines which blocks to place to the side of the given coordinates. */
    @Default WallDecoratorMap wallMap = WallDecoratorMap.builder().build();

    /** Determines which blocks to place in the shape of a pond below this feature. */
    @Default List<ConfiguredPond> ponds = Collections.emptyList();

    /** Determines which blocks to place surrounding the current feature. */
    @Default ConfiguredShell shell = ConfiguredShell.EMPTY_SHELL;

    public static Decorators compile(DecoratorSettings settings, World world) {
        return builder()
            .canReplace(compileCanReplace(settings))
            .caveBlocks(map(settings.caveBlocks, b -> new ConfiguredCaveBlock(b, world)))
            .wallMap(WallDecoratorMap.sort(settings.wallDecorators, world))
            .ponds(map(settings.ponds, p -> new ConfiguredPond(p, world)))
            .shell(new ConfiguredShell(settings.shell, world))
            .build();
    }

    // This would ideally be adapted to check for other *kinds* of block features.
    private static Predicate<IBlockState> compileCanReplace(DecoratorSettings settings) {
        final Set<IBlockState> replaceable = new HashSet<>(settings.replaceableBlocks);
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
            final IBlockState state = replaceable.iterator().next();
            return state::equals;
        }
        return replaceable::contains;
    }
}
