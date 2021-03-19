package com.personthecat.cavegenerator.model;

import com.personthecat.cavegenerator.data.DecoratorSettings;
import com.personthecat.cavegenerator.data.CaveBlockSettings;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.experimental.FieldDefaults;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Builder(toBuilder = true)
@FieldDefaults(level = AccessLevel.PUBLIC, makeFinal = true)
public class Decorators {

    /** Determines whether this carver can replace any given block. */
    @Default Predicate<IBlockState> canReplace = s -> !Blocks.WATER.getDefaultState().equals(s);

    /** Determines which block to place at the given coordinates. */
    @Default List<ConfiguredCaveBlock> caveBlocks = Collections.emptyList();

    /** Determines which blocks to place to the side of the given coordinates. */
    @Default WallDecoratorMap wallMap = WallDecoratorMap.builder().build();

    public static Decorators compile(DecoratorSettings settings, List<IBlockState> featureBlocks, World world) {
        return builder()
            .canReplace(compileCanReplace(settings, featureBlocks))
            .caveBlocks(map(settings.caveBlocks, b -> new ConfiguredCaveBlock(b, world)))
            .wallMap(WallDecoratorMap.sort(settings.wallDecorators, world))
            .build();
    }

    private static <T, U> List<U> map(List<T> list, Function<T, U> mapper) {
        return list.stream().map(mapper).collect(Collectors.toList());
    }

    // This would ideally be adapted to check for other *kinds* of block features.
    private static Predicate<IBlockState> compileCanReplace(DecoratorSettings settings, List<IBlockState> featureBlocks) {
        final List<IBlockState> replaceable = settings.replaceableBlocks;
        if (replaceable.isEmpty()) {
            return s -> true;
        }
        if (settings.replaceDecorators) {
            replaceable.addAll(featureBlocks);
            settings.caveBlocks.forEach(c -> replaceable.addAll(c.states));
            settings.wallDecorators.forEach(w -> replaceable.addAll(w.states));
        }
        if (replaceable.size() == 1) {
            final IBlockState state = replaceable.get(0);
            return state::equals;
        }
        return replaceable::contains;
    }
}
