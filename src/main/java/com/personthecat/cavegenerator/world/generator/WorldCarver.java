package com.personthecat.cavegenerator.world.generator;

import com.personthecat.cavegenerator.data.ConditionSettings;
import com.personthecat.cavegenerator.data.DecoratorSettings;
import com.personthecat.cavegenerator.data.PondSettings;
import com.personthecat.cavegenerator.model.*;
import com.personthecat.cavegenerator.util.PositionFlags;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkPrimer;

import java.util.*;

/** An early world generator supporting cave block replacements and surface decorations. */
public abstract class WorldCarver extends BasicGenerator {

    protected final Decorators decorators;
    protected final int maxPondDepth;

    public WorldCarver(ConditionSettings conditions, DecoratorSettings decorators, World world) {
        super(conditions, world);
        this.decorators = Decorators.compile(decorators, world);
        this.maxPondDepth = getMaxPondDepth(decorators.ponds);
    }

    private static int getMaxPondDepth(List<PondSettings> ponds) {
        int max = 0;
        for (PondSettings pond : ponds) {
            max = Math.max(pond.depth, max);
        }
        return max;
    }

    /** Returns whether the generator has any surface decorators. */
    protected boolean hasWallDecorators() {
        return this.decorators.wallMap.containsAny;
    }

    /** Returns whether the generator has any pond decorators. */
    protected boolean hasPonds() {
        return !this.decorators.ponds.isEmpty();
    }

    /** Returns whether the generator has any shell decorators. */
    protected boolean hasShell() {
        return !this.decorators.shell.decorators.isEmpty();
    }

    /** Spawns whichever cave block is valid at this location, or else air. */
    protected void replaceBlock(Random rand, ChunkPrimer primer, int x, int y, int z, int chunkX, int chunkZ) {
        if (this.decorators.canReplace.test(primer.getBlockState(x, y, z))) {
            for (ConfiguredCaveBlock block : this.decorators.caveBlocks) {
                if (block.canGenerate(x, y, z, chunkX, chunkZ)) {
                    for (IBlockState state : block.cfg.states) {
                        if (rand.nextFloat() <= block.cfg.integrity) {
                            primer.setBlockState(x, y, z, state);
                            return;
                        }
                    }
                }
            }
            primer.setBlockState(x, y, z, BLK_AIR);
        }
    }

    protected void generatePond(PositionFlags positions, Random rand, World world, ChunkPrimer primer, int cX, int cZ) {
        for (int i = 1; i < this.maxPondDepth + 1; i++) {
            final int d = i;
            positions.filter((x, y, z) -> evaluatePond(world, primer, rand, x, y, z, cX, cZ, d));
        }
    }

    private boolean evaluatePond(World world, ChunkPrimer primer, Random rand, int x, int y, int z, int cX, int cZ, int d) {
        final int aX = cX * 16 + x;
        final int aZ = cZ * 16 + x;
        final IBlockState candidate = primer.getBlockState(x, y - d, z);
        if (Blocks.AIR.equals(candidate.getBlock())) return false;
        final IBlockState[] surrounding = getSurrounding(world, primer, x, y - d + 1, z, aX, aZ);
        final int solids = numSolid(surrounding);
        if (d == 1 && solids > 0 || solids > 2) {
            return false;
        }
        final IBlockState[] lower = getSurrounding(world, primer, x, y - d, z, aX, aZ);
        if (anyAir(lower)) {
            return false;
        }
        for (ConfiguredPond pond : this.decorators.ponds) {
            if (d > pond.cfg.depth) continue;
            if (pond.canGenerate(rand, candidate, x, y, z, cX, cZ)) {
                if (placePond(pond, primer, rand, x, y - d, z)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static IBlockState[] getSurrounding(World world, ChunkPrimer primer, int x, int y, int z, int aX, int aZ) {
        return new IBlockState[] {
            x == 0 ? getBlock(world, aX - 1, y, aZ) : primer.getBlockState(x - 1, y, z),
            x == 15 ? getBlock(world, aX + 1, y, aZ) : primer.getBlockState(x + 1, y, z),
            z == 0 ? getBlock(world, aX, y, aZ - 1) : primer.getBlockState(x, y, z - 1),
            z == 15 ? getBlock(world, aX, y, aZ + 1) : primer.getBlockState(x, y, z + 1)
        };
    }

    private static IBlockState getBlock(World world, int aX, int y, int aZ) {
        final BlockPos pos = new BlockPos(aX, y, aZ);
        if (world.isBlockLoaded(pos)) {
            return world.getBlockState(pos);
        }
        return null;
    }

    private static int numSolid(IBlockState[] states) {
        int count = 0;
        for (IBlockState state : states) {
            if (state != null && state.isOpaqueCube()) {
                count++;
            }
        }
        return count;
    }

    private static boolean anyAir(IBlockState[] states) {
        for (IBlockState state : states) {
            if (state != null && state.getBlock().equals(Blocks.AIR)) {
                return true;
            }
        }
        return false;
    }

    private static boolean placePond(ConfiguredPond pond, ChunkPrimer primer, Random rand, int x, int y, int z) {
        for (IBlockState state : pond.cfg.states) {
            if (pond.cfg.integrity == 0 || rand.nextFloat() <= pond.cfg.integrity) {
                primer.setBlockState(x, y, z, state);
                return true;
            }
        }
        return false;
    }

    /** Spawns blocks from the shell decorator settings for a single coordinate. */
    protected void generateShell(Random rand, ChunkPrimer primer, int x, int y, int z, int cY, int chunkX, int chunkZ) {
        for (ConfiguredShell.Decorator shell : this.decorators.shell.decorators) {
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
        if (y < 255 && decorate(this.decorators.wallMap.up, primer, rand, x, y, z, x, y + 1, z, cX, cZ)) {
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
            if (rand.nextFloat() <= decorator.cfg.integrity) {
                if (decorator.decidePlace(state, primer, x, y, z, xO, yO, zO)) {
                    return true;
                }
            }
        }
        return false;
    }
}
