package com.personthecat.cavegenerator.world.generator;

import com.personthecat.cavegenerator.data.ConditionSettings;
import com.personthecat.cavegenerator.data.DecoratorSettings;
import com.personthecat.cavegenerator.model.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkPrimer;

import java.util.*;

/** An early world generator supporting cave block replacements and surface decorations. */
public abstract class WorldCarver extends BasicGenerator {

    protected final Decorators decorators;

    public WorldCarver(ConditionSettings conditions, DecoratorSettings decorators, World world) {
        super(conditions, world);
        this.decorators = Decorators.compile(decorators, Collections.emptyList(), world);
    }

    /** Returns whether the generator has any surface decorators. */
    protected boolean hasLocalDecorators() {
        return decorators.wallMap.containsAny;
    }

    /** Spawns whichever cave block is valid at this location, or else air. */
    protected void replaceBlock(Random rand, ChunkPrimer primer, int x, int y, int z, int chunkX, int chunkZ) {
        if (decorators.canReplace.test(primer.getBlockState(x, y, z))) {
            for (ConfiguredCaveBlock block : decorators.caveBlocks) {
                if (block.canGenerate(x, y, z, chunkX, chunkZ)) {
                    for (IBlockState state : block.cfg.states) {
                        if (rand.nextFloat() <= block.cfg.chance) {
                            primer.setBlockState(x, y, z, state);
                            return;
                        }
                    }
                }
            }
            primer.setBlockState(x, y, z, BLK_AIR);
        }
    }

    /** Spawns blocks from the shell decorator settings for a single coordinate. */
    protected void generateShell(Random rand, ChunkPrimer primer, int x, int y, int z, int cY, int chunkX, int chunkZ) {
        for (ConfiguredShell.Decorator shell : decorators.shell.decorators) {
            if (shell.cfg.height.contains(cY)) {
                final IBlockState candidate = primer.getBlockState(x, y, z);
                if (shell.matches(candidate) && shell.testNoise(x, y, z, chunkX, chunkZ)) {
                    for (IBlockState state : shell.cfg.states) {
                        if (rand.nextFloat() <= shell.cfg.chance) {
                            primer.setBlockState(x, y, z, state);
                            return;
                        }
                    }
                }
            }
        }
    }

    /** Conditionally replaces the current block with blocks from this generator's WallDecorators. */
    protected void decorateBlock(Random rand, ChunkPrimer primer, int x, int y, int z, int cX, int cZ) {
        if (!this.decorators.wallMap.containsAny) {
            return;
        }
        if (decorateAll(this.decorators.wallMap.all, primer, rand, x, y, z, cX, cZ)) {
            return;
        }
        if (decorateSide(this.decorators.wallMap.side, primer, rand, x, y, z, cX, cZ)) {
            return;
        }
        if (y > 0 && decorate(this.decorators.wallMap.down, primer, rand, x, y, z, x, y - 1, z, cX, cZ)) {
            return;
        }
        if (y < 15 && decorate(this.decorators.wallMap.up, primer, rand, x, y, z, x, y + 1, z, cX, cZ)) {
            return;
        }
        if (x > 0 && decorate(this.decorators.wallMap.west, primer, rand, x, y, z, x - 1, y, z, cX, cZ)) {
            return;
        }
        if (x < 15 && decorate(this.decorators.wallMap.east, primer, rand, x, y, z, x + 1, y, z, cX, cZ)) {
            return;
        }
        if (z > 0 && decorate(this.decorators.wallMap.north, primer, rand, x, y, z, x, y, z - 1, cX, cZ)) {
            return;
        }
        if (z < 15) {
            decorate(this.decorators.wallMap.south, primer, rand, x, y, z, x, y, z + 1, cX, cZ);
        }
    }

    // Avoids redundant noise calculations, if applicable.
    private static boolean decorateAll(List<ConfiguredWallDecorator> all, ChunkPrimer primer, Random rand, int x, int y, int z, int cX, int cZ) {
        for (ConfiguredWallDecorator decorator : all) {
            if (decorator.canGenerate(rand, x, y, z, cX, cZ)) {
                if (y > 0 && checkPlaceWall(decorator, primer, rand, x, y, z, x, y - 1, z)) {
                    return true;
                }
                if (y < 255 && checkPlaceWall(decorator, primer, rand, x, y, z, x, y + 1, z)) {
                    return true;
                }
                if (x > 0 && checkPlaceWall(decorator, primer, rand, x, y, z, x - 1, y, z)) {
                    return true;
                }
                if (x < 15 && checkPlaceWall(decorator, primer, rand, x, y, z, x + 1, y, z)) {
                    return true;
                }
                if (z > 0 && checkPlaceWall(decorator, primer, rand, x, y, z, x, y, z - 1)) {
                    return true;
                }
                if (z < 15 && checkPlaceWall(decorator, primer, rand, x, y, z, x, y, z + 1)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean decorateSide(List<ConfiguredWallDecorator> side, ChunkPrimer primer, Random rand, int x, int y, int z, int cX, int cZ) {
        for (ConfiguredWallDecorator decorator : side) {
            if (decorator.canGenerate(rand, x, y, z, cX, cZ)) {
                if (x > 0 && checkPlaceWall(decorator, primer, rand, x, y, z, x - 1, y, z)) {
                    return true;
                }
                if (x < 15 && checkPlaceWall(decorator, primer, rand, x, y, z, x + 1, y, z)) {
                    return true;
                }
                if (z > 0 && checkPlaceWall(decorator, primer, rand, x, y, z, x, y, z - 1)) {
                    return true;
                }
                if (z < 15 && checkPlaceWall(decorator, primer, rand, x, y, z, x, y, z + 1)) {
                    return true;
                }
            }
        }
        return false;
    }


    private static boolean checkPlaceWall(ConfiguredWallDecorator decorator, ChunkPrimer primer, Random rand, int x, int y, int z, int xO, int yO, int zO) {
        if (decorator.matchesBlock(primer.getBlockState(xO, yO, zO))) {
            return placeWall(decorator, primer, rand, x, y, z, xO, yO, zO);
        }
        return false;
    }

    private static boolean decorate(List<ConfiguredWallDecorator> decorators, ChunkPrimer primer, Random rand, int x, int y, int z, int xO, int yO, int zO, int cX, int cZ) {
        for (ConfiguredWallDecorator decorator : decorators) {
            final IBlockState candidate = primer.getBlockState(xO, yO, zO);
            if (decorator.canGenerate(rand, candidate, x, y, z, cX, cZ)) {
                if (placeWall(decorator, primer, rand, x, y, z, xO, yO, zO)) {
                    return true;
                }
            }
        }
        return false;
    }

    // Returns true if the current block is replaced, not the wall.
    private static boolean placeWall(ConfiguredWallDecorator decorator, ChunkPrimer primer, Random rand, int x, int y, int z, int xO, int yO, int zO) {
        for (IBlockState state : decorator.cfg.states) {
            if (rand.nextFloat() <= decorator.cfg.chance) {
                if (decorator.decidePlace(state, primer, x, y, z, xO, yO, zO)) {
                    return true;
                }
            }
        }
        return false;
    }
}
