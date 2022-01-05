package personthecat.cavegenerator.world.generator;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import personthecat.cavegenerator.model.*;
import personthecat.cavegenerator.world.config.*;

import java.util.List;
import java.util.Random;

import static personthecat.cavegenerator.util.CommonBlocks.BLK_CAVE;

public abstract class CaveCarver extends EarlyGenerator {

    protected final DecoratorConfig decorators;
    protected final int maxPondDepth;

    public CaveCarver(final ConditionConfig conditions, final DecoratorConfig decorators, final Random rand, final long seed) {
        super(conditions, rand, seed);
        this.decorators = decorators;

        int max = 0;
        for (final PondConfig pond : decorators.ponds) {
            max = Math.max(pond.depth, max);
        }
        this.maxPondDepth = max;
    }

    /**
     * @return Whether this generator is configured to place any wall decorators.
     */
    protected boolean hasWallDecorators() {
        return this.decorators.wallMap.containsAny;
    }

    /**
     * @return Whether this generator is configured to place any pond decorations.
     */
    protected boolean hasPonds() {
        return !this.decorators.ponds.isEmpty();
    }

    /**
     * @return Whether this generator is configured to generate inside of a shell decorators.
     */
    protected boolean hasShell() {
        return !this.decorators.shell.decorators.isEmpty();
    }

    /**
     * Updates the block at the given coordinates using the first valid cave block, or else
     * sets it to {@link Blocks#CAVE_AIR}.
     *
     * @param ctx  The current early generation context.
     * @param rand A RNG used for <b>decoration purposes only</b>.
     * @param x    The relative x-coordinate of the block being placed.
     * @param y    The relative y-coordinate of the block being placed.
     * @param z    The relative z-coordinate of the block being placed.
     */
    protected void replaceBlock(PrimerContext ctx, Random rand, int x, int y, int z) {
        if (this.decorators.canReplace.test(ctx.get(x, y, z))) {
            for (final CaveBlockConfig block : this.decorators.caveBlocks) {
                if (block.canGenerate(x, y, z, ctx.chunkX, ctx.chunkZ)) {
                    for (final BlockState state : block.states) {
                        if (rand.nextFloat() <= block.integrity) {
                            ctx.set(x, y, z, state);
                            return;
                        }
                    }
                }
            }
            ctx.set(x, y, z, BLK_CAVE);
        }
    }

    /**
     * Runs all of the applicable decorators for the presently-generating cavern.
     *
     * @param ctx       The current early generation context.
     * @param positions A set of block positions indicating which blocks were updated.
     * @param rand      A RNG used for <b>decoration purposes only</b>.
     */
    protected void decorateAll(PrimerContext ctx, PositionFlags positions, Random rand) {
        if (this.hasPonds()) {
            this.generatePond(ctx, positions, rand);
        }
        if (this.hasWallDecorators()) {
            this.generateWall(ctx, positions, rand);
        }
    }

    /**
     * Generates a pond feature which is local to the caverns being generated.
     *
     * @param ctx       The current early generation context.
     * @param positions A set of relative block positions indicating which blocks were updated.
     * @param rand      A RNG used for <b>decoration purposes only</b>.
     */
    protected void generatePond(PrimerContext ctx, PositionFlags positions, Random rand) {
        positions.forEach((x, y, z) -> evaluatePond(ctx, rand, x, y, z));
    }

    /**
     * Determines whether a pond block should spawn at the given coordinates. If so, it will
     * generated at and below this location.
     *
     * @param ctx  The current early generation context.
     * @param rand A RNG used for <b>decoration purposes only</b>.
     * @param x    The relative x-coordinate being tested.
     * @param y    The relative y-coordinate being tested.
     * @param z    The relative z-coordinate being tested.
     */
    private void evaluatePond(PrimerContext ctx, Random rand, int x, int y, int z) {
        final BlockState candidate = ctx.get(x, y - 1, z);
        if (candidate.isAir()) {
            return;
        }
        if (anySolid(getSurrounding(ctx, x, y, z))) {
            return;
        }
        if (anyAir(getSurrounding(ctx, x, y - 1, z))) {
            return;
        }
        int d = 1;
        for (final PondConfig pond : this.decorators.ponds) {
            if (pond.depth >= d && pond.canGenerate(rand, candidate, x, y, z, ctx.chunkX, ctx.chunkZ)) {
                d = placeCountPond(ctx, pond, rand, x, y, z);
            }
        }
    }

    /**
     * Returns an array of the four blocks surrounding the current position horizontally.
     *
     * @param ctx  The current early generation context.
     * @param x    The relative x-coordinate being tested.
     * @param y    The relative y-coordinate being tested.
     * @param z    The relative z-coordinate being tested.
     * @return An array of 4 surrounding blocks, or else {@link Blocks#CAVE_AIR}.
     */
    private static BlockState[] getSurrounding(PrimerContext ctx, int x, int y, int z) {
        // Todo: we used to check surrounding chunks, if loaded. Need to expose the current level.
        return new BlockState[] {
            x == 0 ? BLK_CAVE : ctx.get(x - 1, y, z),
            x == 15 ? BLK_CAVE : ctx.get(x + 1, y, z),
            z == 0 ? BLK_CAVE : ctx.get(x, y, z - 1),
            z == 15 ? BLK_CAVE : ctx.get(x, y, z + 1)
        };
    }

    /**
     * Determines whether any of the blocks in this array are solid and thus viable candidates
     * for pond placement.
     *
     * @param states An array of the 4 blocks immediately surrounding the current position.
     * @return <code>true</code> if any one of these blocks is solid.
     */
    private static boolean anySolid(final BlockState[] states) {
        for (final BlockState state : states) {
            if (state != null && state.getMaterial().isSolid()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determines whether any of the blocks in this array are air blocks and thus <b>not</b>
     * viable candidates for pond placement.
     *
     * @param states An array of the 4 blocks immediately surrounding the current position.
     * @return <code>true</code> if any one of these blocks is air.
     */
    private static boolean anyAir(final BlockState[] states) {
        for (final BlockState state : states) {
            if (state != null && state.isAir()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Places a single column of a pond decorator to the current location in the world.
     *
     * @param ctx  The current early generation context.
     * @param pond A RNG used for <b>decoration purposes only</b>.
     * @param rand The single pond decorator which has been selected for placement.
     * @param x    The relative x-coordinate being tested.
     * @param y    The relative y-coordinate being tested.
     * @param z    The relative z-coordinate being tested.
     * @return The number of blocks placed.
     */
    private static int placeCountPond(PrimerContext ctx, PondConfig pond, Random rand, int x, int y, int z) {
        if (pond.integrity == 1.0 && pond.states.size() > 0) {
            final BlockState state = pond.states.get(0);
            for (int i = y - 1; i > y - pond.depth - 1; i--) {
                ctx.set(x, i, z, state);
            }
            return y - pond.depth;
        }
        int yO = y;
        for (int i = 1; i < pond.depth + 1; i++) {
            yO = y - i;
            for (BlockState state : pond.states) {
                if (rand.nextFloat() <= pond.integrity) {
                    ctx.set(x, y, z, state);
                } else {
                    return yO + 1;
                }
            }
        }
        return yO;
    }

    /**
     * Spawns blocks from the shell decorator settings for a single coordinate.
     *
     * @param ctx  The current early generation context.
     * @param rand A RNG used for <b>decoration purposes only</b>.
     * @param x    The relative x-coordinate being tested.
     * @param y    The relative y-coordinate being tested.
     * @param z    The relative z-coordinate being tested.
     * @param cY   The center height for this shell to generate around.
     */
    protected void generateShell(PrimerContext ctx, Random rand, int x, int y, int z, int cY) {
        for (final ShellConfig.Decorator shell : this.decorators.shell.decorators) {
            if (shell.height.contains(cY)) {
                final BlockState candidate = ctx.get(x, y, z);
                if (shell.matches(candidate) && shell.testNoise(x, y, z, ctx.chunkX, ctx.chunkZ)) {
                    for (final BlockState state : shell.states) {
                        if (rand.nextFloat() <= shell.integrity) {
                            ctx.set(x, y, z, state);
                            return;
                        }
                    }
                }
            }
        }
    }

    /**
     * Generates wall decorations for an entire set of block positions.
     *
     * @param ctx       The current early generation context.
     * @param positions A set of block positions which have been updated by the feature.
     * @param rand      A RNG used for <b>decoration purposes only</b>.
     */
    protected void generateWall(PrimerContext ctx, PositionFlags positions, Random rand) {
        positions.forEach((x, y, z) -> this.decorateBlock(ctx, rand, x, y, z));
    }

    /**
     * Conditionally replaces the current block with blocks from this generator's WallDecorators.
     *
     * @param ctx  The current early generation context.
     * @param rand A RNG used for <b>decoration purposes only</b>.
     * @param x    The relative x-coordinate being tested.
     * @param y    The relative y-coordinate being tested.
     * @param z    The relative z-coordinate being tested.
     */
    protected void decorateBlock(PrimerContext ctx, Random rand, int x, int y, int z) {
        if (!this.decorators.wallMap.containsAny) {
            return;
        }
        if (decorateAll(ctx, this.decorators.wallMap.all, rand, x, y, z)) {
            return;
        }
        if (decorateSide(ctx, this.decorators.wallMap.side, rand, x, y, z)) {
            return;
        }
        if (y > 0 && decorate(ctx, this.decorators.wallMap.down, rand, x, y, z, x, y - 1, z)) {
            return;
        }
        if (y < 255 && decorate(ctx, this.decorators.wallMap.up, rand, x, y, z, x, y + 1, z)) {
            return;
        }
        if (x > 0 && decorate(ctx, this.decorators.wallMap.west, rand, x, y, z, x - 1, y, z)) {
            return;
        }
        if (x < 15 && decorate(ctx, this.decorators.wallMap.east, rand, x, y, z, x + 1, y, z)) {
            return;
        }
        if (z > 0 && decorate(ctx, this.decorators.wallMap.north, rand, x, y, z, x, y, z - 1)) {
            return;
        }
        if (z < 15) {
            decorate(ctx, this.decorators.wallMap.south, rand, x, y, z, x, y, z + 1);
        }
    }

    /**
     * Variant of {@link #decorate} which was originally thought to be an optimization for any
     * feature which generates wall decorations on every side.
     *
     * @param ctx  The current early generation context.
     * @param all  <b>all</b> of the wall decorators being generated at this position.
     * @param rand A RNG used for <b>decoration purposes only</b>.
     * @param x    The relative x-coordinate being tested.
     * @param y    The relative y-coordinate being tested.
     * @param z    The relative z-coordinate being tested.
     * @return     Whether the block at the current position hsa been updated.
     */
    protected boolean decorateAll(PrimerContext ctx, List<WallDecoratorConfig> all, Random rand, int x, int y, int z) {
        for (final WallDecoratorConfig decorator : all) {
            if (decorator.canGenerate(rand, x, y, z, ctx.chunkX, ctx.chunkZ)) {
                if (y > 0 && checkPlaceWall(ctx, decorator, rand, x, y, z, x, y - 1, z)) {
                    return true;
                }
                if (y < 255 && checkPlaceWall(ctx, decorator, rand, x, y, z, x, y + 1, z)) {
                    return true;
                }
                if (x > 0 && checkPlaceWall(ctx, decorator, rand, x, y, z, x - 1, y, z)) {
                    return true;
                }
                if (x < 15 && checkPlaceWall(ctx, decorator, rand, x, y, z, x + 1, y, z)) {
                    return true;
                }
                if (z > 0 && checkPlaceWall(ctx, decorator, rand, x, y, z, x, y, z - 1)) {
                    return true;
                }
                if (z < 15 && checkPlaceWall(ctx, decorator, rand, x, y, z, x, y, z + 1)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Variant of {@link #decorate} which was originally thought to be an optimization for any
     * feature which generates wall decorations on every horizontal side.
     *
     * @param ctx  The current early generation context.
     * @param side All of the horizontal wall decorators being generated at this position.
     * @param rand A RNG used for <b>decoration purposes only</b>.
     * @param x    The relative x-coordinate being tested.
     * @param y    The relative y-coordinate being tested.
     * @param z    The relative z-coordinate being tested.
     * @return     Whether the block at the current position hsa been updated.
     */
    private boolean decorateSide(PrimerContext ctx, List<WallDecoratorConfig> side, Random rand, int x, int y, int z) {
        for (final WallDecoratorConfig decorator : side) {
            if (decorator.canGenerate(rand, x, y, z, ctx.chunkX, ctx.chunkZ)) {
                if (x > 0 && checkPlaceWall(ctx, decorator, rand, x, y, z, x - 1, y, z)) {
                    return true;
                }
                if (x < 15 && checkPlaceWall(ctx, decorator, rand, x, y, z, x + 1, y, z)) {
                    return true;
                }
                if (z > 0 && checkPlaceWall(ctx, decorator, rand, x, y, z, x, y, z - 1)) {
                    return true;
                }
                if (z < 15 && checkPlaceWall(ctx, decorator, rand, x, y, z, x, y, z + 1)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Determines whether a wall decorator can spawn at the current coordinates. If so, spawns it.
     *
     * @param ctx       The current early generation context.
     * @param decorator The wall decorator being generated.
     * @param rand      A RNG used for <b>decoration purposes only</b>.
     * @param x0        The x-coordinate of the current cave block.
     * @param y0        The y-coordinate of the current cave block.
     * @param z0        The z-coordinate of the current cave block.
     * @param xD        The x-coordinate of the current surface block.
     * @param yD        The y-coordinate of the current surface block.
     * @param zD        The z-coordinate of the current surface block.
     * @return Whether a block was placed at the current position.
     */
    private boolean checkPlaceWall(PrimerContext ctx, WallDecoratorConfig decorator, Random rand, int x0, int y0, int z0, int xD, int yD, int zD) {
        if (decorator.matchesBlock(ctx.get(xD, yD, zD))) {
            return placeWall(ctx, decorator, rand, x0, y0, z0, xD, yD, zD);
        }
        return false;
    }

    /**
     * Places a single wall decoration when given a list of applicable wall decorators.
     *
     * @param ctx        The current early generation context.
     * @param decorators All of the wall decorator which may generate.
     * @param rand       A RNG used for <b>decoration purposes only</b>.
     * @param x0         The x-coordinate of the current cave block.
     * @param y0         The y-coordinate of the current cave block.
     * @param z0         The z-coordinate of the current cave block.
     * @param xD         The x-coordinate of the current surface block.
     * @param yD         The y-coordinate of the current surface block.
     * @param zD         The z-coordinate of the current surface block.
     * @return Whether a block was placed at the current position.
     */
    private boolean decorate(PrimerContext ctx, List<WallDecoratorConfig> decorators, Random rand, int x0, int y0, int z0, int xD, int yD, int zD) {
        for (final WallDecoratorConfig decorator : decorators) {
            final BlockState candidate = ctx.get(xD, yD, zD);
            if (decorator.canGenerate(rand, candidate, x0, y0, z0, ctx.chunkX, ctx.chunkZ)) {
                if (placeWall(ctx, decorator, rand, x0, y0, z0, xD, yD, zD)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Places a single wall decoration at the current coordinates, if the integrity and noise
     * checks all pass.
     *
     * @param ctx       The current early generation context.
     * @param decorator The wall decorator being generated.
     * @param rand      A RNG used for <b>decoration purposes only</b>.
     * @param x0        The x-coordinate of the current cave block.
     * @param y0        The y-coordinate of the current cave block.
     * @param z0        The z-coordinate of the current cave block.
     * @param xD        The x-coordinate of the current surface block.
     * @param yD        The y-coordinate of the current surface block.
     * @param zD        The z-coordinate of the current surface block.
     * @return Whether a block was placed at the current position.
     */
    private boolean placeWall(PrimerContext ctx, WallDecoratorConfig decorator, Random rand, int x0, int y0, int z0, int xD, int yD, int zD) {
        for (final BlockState state : decorator.states) {
            if (rand.nextFloat() <= decorator.integrity) {
                if (decorator.decidePlace(ctx, state, x0, y0, z0, xD, yD, zD)) {
                    return true;
                }
            }
        }
        return false;
    }
}
