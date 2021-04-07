package com.personthecat.cavegenerator.world.generator;

import com.personthecat.cavegenerator.data.ConditionSettings;
import com.personthecat.cavegenerator.data.DecoratorSettings;
import com.personthecat.cavegenerator.model.*;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
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
    protected void decorateBlock(Random rand, ChunkPrimer primer, int x, int y, int z, int chunkX, int chunkZ) {
        if (this.decorateVertical(rand, primer, x, y, z, chunkX, chunkZ, true)) {
            return;
        } else if (this.decorateVertical(rand, primer, x, y, z, chunkX, chunkZ, false)) {
            return;
        }
        this.decorateHorizontal(rand, primer, x, y, z, chunkX, chunkZ);
    }

    // Todo: this is heinous.
    private boolean decorateVertical(Random rand, ChunkPrimer primer, int x, int y, int z, int chunkX, int chunkZ, boolean up) {
        final int offset = up ? y + 1 : y - 1;
        final List<ConfiguredWallDecorator> decorators = up ? this.decorators.wallMap.up : this.decorators.wallMap.down;
        for (ConfiguredWallDecorator decorator : decorators) {
            final IBlockState candidate = primer.getBlockState(x, offset, z);
            // Ignore air blocks.
            if (candidate.getMaterial().equals(Material.AIR)) {
                return false;
            }
            // Filter for valid generators at this position and for this block state.
            if (decorator.canGenerate(rand, candidate, x, y, z, chunkX, chunkZ)) {
                if (decorator.matchesBlock(candidate)) {
                    for (IBlockState state : decorator.cfg.states) {
                        if (rand.nextFloat() <= decorator.cfg.chance) {
                            // Place block -> return success if original was replaced.
                            if (decorator.decidePlace(state, primer, x, y, z, x, offset, z)) {
                                return true;
                            } // else continue iterating through decorators.
                        }
                    }
                }
            }
        }
        // Everything failed.
        return false;
    }

    // Todo: refactor this to be easier to read and support multiple directions.
    private void decorateHorizontal(Random rand, ChunkPrimer primer, int x, int y, int z, int chunkX, int chunkZ ) {
        // Avoid repeated calculations.
        final List<ConfiguredWallDecorator> testedDecorators = pretestDecorators(rand, x, y, z, chunkX, chunkZ);
        // We'll need to reiterate through those decorators below.
        // Todo: This is probably where you'd add in specific direction support.
        for (BlockPos pos : nsew(x, y, z)) {
            if (!areCoordsInChunk(pos.getX(), pos.getZ())) {
                continue;
            }
            final IBlockState candidate = primer.getBlockState(pos.getX(), pos.getY(), pos.getZ());
            // Ignore air blocks.
            if (candidate.getMaterial().equals(Material.AIR)) {
                continue;
            }
            for (ConfiguredWallDecorator decorator : testedDecorators) {
                if (decorator.matchesBlock(candidate)) {
                    for (IBlockState state : decorator.cfg.states) {
                        if (rand.nextFloat() <= decorator.cfg.chance) {
                            // Place block -> return success if original was replaced.
                            if (decorator.decidePlace(state, primer, x, y, z, pos.getX(), pos.getY(), pos.getZ())) {
                                return;
                            } // else continue iterating through decorators.
                        }
                    }
                }
            }
        }
    }

    private List<ConfiguredWallDecorator> pretestDecorators(Random rand, int x, int y, int z, int chunkX, int chunkZ) {
        final List<ConfiguredWallDecorator> testedDecorators = new ArrayList<>();
        for (ConfiguredWallDecorator decorator : decorators.wallMap.north) { // Todo: this used to be all horizontal decorators
            // Filter for valid generators at this position only.
            if (decorator.canGenerate(rand, x, y, z, chunkX, chunkZ)) {
                testedDecorators.add(decorator);
            }
        }
        return testedDecorators;
    }

    private BlockPos[] nsew(int x, int y, int z) {
        return new BlockPos[] {
            new BlockPos(x, y, z - 1), // North
            new BlockPos(x, y, z + 1), // South
            new BlockPos(x + 1, y, z), // East
            new BlockPos(x - 1, y, z)  // West
        };
    }

    protected boolean areCoordsInChunk(int x, int z) {
        return x > -1 && x < 16 && z > -1 && z < 16;
    }
}
