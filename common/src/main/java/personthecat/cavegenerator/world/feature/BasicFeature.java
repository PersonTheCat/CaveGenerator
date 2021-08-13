package personthecat.cavegenerator.world.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import personthecat.catlib.util.Shorthand;
import personthecat.cavegenerator.data.ConditionSettings;
import personthecat.cavegenerator.model.BlockCheck;
import personthecat.cavegenerator.model.Conditions;

import java.util.List;
import java.util.Random;

public abstract class BasicFeature {

    /** The number of times to try locating vertical surfaces for structures. */
    protected static final int VERTICAL_RETRIES = 3;

    /** The number of times to try locating horizontal surfaces for structures. */
    protected static final int HORIZONTAL_RETRIES = 20;

    /** The value returned by any surface locator when no surface is found. */
    protected static final int NONE_FOUND = Integer.MIN_VALUE;

    /** Minimum distance below the surface for all late features. */
    protected static final int SURFACE_ROOM = 5;

    protected final Conditions conditions;
    protected final Random globalRand;
    protected final long seed;

    public BasicFeature(final ConditionSettings conditions, final Random rand, final long seed) {
        this.conditions = Conditions.compile(conditions, rand, seed);
        this.globalRand = rand;
        this.seed = seed;
    }

    /**
     * Generates this feature <em>after</em> checking to ensure that it can spawn in the
     * current dimension.
     *
     * Todo: This is completely unnecessary. Generators should be mapped to dimensions,
     *   thus leaving no reason to check whether they can spawn here.
     *
     * @param ctx A context containing world information and coordinates.
     */
    public final void generate(final WorldContext ctx) {
        // Todo: dimensions (note that the controller can be mapped to a dimension)
        this.doGenerate(ctx);
    }

    /**
     * The primary function which much be implemented in order to make changes to the
     * world.
     *
     * @param ctx A context containing world information and coordinates.
     */
    protected abstract void doGenerate(final WorldContext ctx);

    /**
     * Determines whether the block at the input coordinates is a solid cube.
     *
     * @param level The current world being operated on.
     * @param x     The x-coordinate of the block in question.
     * @param y     The y-coordinate of the block in question.
     * @param z     The z-coordinate of the block in question.
     */
    protected final boolean isSolid(final Level level, final int x, final int y, final int z) {
        return isSolid(level, new BlockPos(x, y, z));
    }

    /**
     * Determines whether the block at the input coordinates is a solid cube.
     *
     * @param level The current world being operated on.
     * @param pos   The coordinates of the block in question.
     * @return <code>true</code> If the block at this position is solid.
     */
    protected final boolean isSolid(final Level level, final BlockPos pos) {
        return level.getBlockState(pos).getMaterial().isSolid();
    }

    /**
     * Locates the first cave surface from above within the specified range.
     *
     * @param level The current world being operated on.
     * @param x     The x-coordinate of the starting position.
     * @param y     The y-coordinate of the starting position.
     * @param z     The z-coordinate of the starting position.
     * @param minY  The minimum y-bound, exclusive.
     * @return The position of the floor, or else <code>NONE_FOUND</code>.
     */
    protected final int findFloor(final Level level, final int x, final int y, final int z, final int minY) {
        final MutableBlockPos pos = new MutableBlockPos(x, y, z);
        // Skip until not solid
        while (pos.getY() > minY && isSolid(level, pos)) {
            pos.setY(pos.getY() - 1);
        }
        // Get first solid
        while (pos.getY() > minY) {
            pos.setY(pos.getY() - 1);
            if (isSolid(level, pos)) {
                return pos.getY();
            }
        }
        return NONE_FOUND;
    }

    /**
     * Locates the first cave surface from below within the specified range.
     *
     * @param level The current world being operated on.
     * @param x     The x-coordinate of the starting position.
     * @param y     The y-coordinate of the starting position.
     * @param z     The z-coordinate of the starting position.
     * @param maxY  The maximum y-bound, exclusive.
     * @return The position of the ceiling, or else <code>NONE_FOUND</code>.
     */
    protected final int findCeiling(final Level level, final int x, final int y, final int z, final int maxY) {
        final MutableBlockPos pos = new MutableBlockPos(x, y, z);
        while (pos.getY() < maxY && isSolid(level, pos)) {
            pos.setY(pos.getY() + 1);
        }
        while (pos.getY() < maxY) {
            pos.setY(pos.getY() + 1);
            if (isSolid(level, pos)) {
                return pos.getY();
            }
        }
        return NONE_FOUND;
    }

    /**
     * Randomly locates a cave surface from above within the specified range. Starts at
     * a random coordinate, then starts from the top, if nothing is found.
     *
     * @param level The current world being operated on.
     * @param rand  The ongoing RNG in use by the current generator.
     * @param x     The x-coordinate of the starting position.
     * @param z     The z-coordinate of the starting position.
     * @param minY  The maximum y-bound, exclusive.
     * @param maxY  The maximum y-bound, exclusive.
     * @return The position of the floor, or else <code>NONE_FOUND</code>.
     */
    protected final int randFindFloor(final Level level, final Random rand, final int x, final int z, final int minY, final int maxY) {
        // Start at a random coordinate. Then try from the top, if nothing is found.
        final int startY = Shorthand.numBetween(rand, minY, maxY);
        final int y = findFloor(level, x, startY, z, minY);
        if (y != NONE_FOUND) {
            return y;
        }
        return findFloor(level, x, maxY, z, startY);
    }

    /**
     * Randomly locates a cave surface from below within the specified range. Starts at
     * a random coordinate, then starts from the top, if nothing is found.
     *
     * @param level The current world being operated on.
     * @param rand  The ongoing RNG in use by the current generator.
     * @param x     The x-coordinate of the starting position.
     * @param z     The z-coordinate of the starting position.
     * @param minY  The maximum y-bound, exclusive.
     * @param maxY  The maximum y-bound, exclusive.
     * @return The position of the ceiling, or else <code>NONE_FOUND</code>.
     */
    protected final int randFindCeiling(final Level level, final Random rand, final int x, final int z, final int minY, final int maxY) {
        // Start at a random coordinate. Then try from the top, if nothing is found.
        final int startY = Shorthand.numBetween(rand, minY, maxY);
        int y = findCeiling(level, x, startY, z, maxY);
        if (y == NONE_FOUND) {
            y = findCeiling(level, x, minY, z, startY);
        }
        return y;
    }

    /**
     * Locates the first cave opening from a random coordinate, randomly searching up or down.
     *
     * @param level The current world being operated on.
     * @param rand  The ongoing RNG in use by the current generator.
     * @param x     The x-coordinate of the starting position.
     * @param z     The z-coordinate of the starting position.
     * @param minY  The maximum y-bound, exclusive.
     * @param maxY  The maximum y-bound, exclusive.
     * @return The position of the surface, or else <code>NONE_FOUND</code>.
     */
    protected final int findOpeningVertical(final Random rand, final Level level, final int x, final int z, final int minY, final int maxY) {
        final int startY = Shorthand.numBetween(rand, minY, maxY);
        if (rand.nextBoolean()) {
            // First search from the center up.
            final int fromCenter = findOpeningFromBelow(level, x, startY, z, maxY);
            if (fromCenter != NONE_FOUND) {
                return fromCenter;
            }
            // Then try from the bottom to the center.
            return findOpeningFromBelow(level, x, minY, z, startY);
        } else {
            final int fromCenter = findOpeningFromAbove(level, x, startY, z, minY);
            if (fromCenter != NONE_FOUND) {
                return fromCenter;
            }
            return findOpeningFromAbove(level, x, maxY, z, startY);
        }
    }

    /**
     * Locates the first cave opening <b>or surface</b> from below within the specified range.
     *
     * @param level The current world being operated on.
     * @param x     The x-coordinate of the starting position.
     * @param y     The y-coordinate of the starting position.
     * @param z     The z-coordinate of the starting position.
     * @param maxY  The maximum y-bound, exclusive.
     * @return The position of the ceiling, or else <code>NONE_FOUND</code>.
     */
    protected final int findOpeningFromBelow(final Level level, final int x, final int y, final int z, final int maxY) {
        final MutableBlockPos pos = new MutableBlockPos(x, y, z);
        final boolean solid = isSolid(level, pos);
        pos.setY(y + 1);

        while (pos.getY() < maxY) {
            if ((solid != isSolid(level, pos))) {
                return pos.getY();
            }
            pos.setY(pos.getY() + 1);
        }
        return NONE_FOUND;
    }

    /**
     * Locates the first cave opening <b>or surface</b> from above within the specified range.
     *
     * @param level The current world being operated on.
     * @param x     The x-coordinate of the starting position.
     * @param y     The y-coordinate of the starting position.
     * @param z     The z-coordinate of the starting position.
     * @param minY  The minimum y-bound, exclusive.
     * @return The position of the ceiling, or else <code>NONE_FOUND</code>.
     */
    protected final int findOpeningFromAbove(final Level level, final int x, final int y, final int z, final int minY) {
        final MutableBlockPos pos = new MutableBlockPos(x, y, z);
        final boolean solid = isSolid(level, pos);
        pos.setY(y - 1);

        while (pos.getY() > minY) {
            if ((solid != isSolid(level, pos))) {
                return pos.getY();
            }
            pos.setY(pos.getY() - 1);
        }
        return NONE_FOUND;
    }

    /**
     * Locates the first cave opening <b>or surface</b> from south within the specified range.
     *
     * @param level   The current world being operated on.
     * @param x       The x-coordinate of the starting position.
     * @param y       The y-coordinate of the starting position.
     * @param offsetZ The z-coordinate, starting from an offset of 8.
     * @return The position of the opening, or else <code>NONE_FOUND</code>.
     */
    protected final int findOpeningNorth(final Level level, final int x, final int y, final int offsetZ) {
        final MutableBlockPos pos = new MutableBlockPos(x, y, offsetZ + 15);
        final boolean solid = isSolid(level, pos);
        pos.setZ(offsetZ + 14);

        while (pos.getZ() >= offsetZ) {
            if (solid != isSolid(level, pos)) {
                return pos.getZ();
            }
            pos.setZ(pos.getZ() - 1);
        }
        return NONE_FOUND;
    }

    /**
     * Locates the first cave opening <b>or surface</b> from north within the specified range.
     *
     * @param level   The current world being operated on.
     * @param x       The x-coordinate of the starting position.
     * @param y       The y-coordinate of the starting position.
     * @param offsetZ The z-coordinate, starting from an offset of 8.
     * @return The position of the opening, or else <code>NONE_FOUND</code>.
     */
    protected final int findOpeningSouth(final Level level, final int x, final int y, final int offsetZ) {
        final MutableBlockPos pos = new MutableBlockPos(x, y, offsetZ);
        final boolean solid = isSolid(level, pos);
        pos.setZ(offsetZ + 1);

        while (pos.getZ() < offsetZ + 16) {
            if (solid != isSolid(level, pos)) {
                return pos.getZ();
            }
            pos.setZ(pos.getZ() + 1);
        }
        return NONE_FOUND;
    }

    /**
     * Locates the first cave opening <b>or surface</b> from west within the specified range.
     *
     * @param level   The current world being operated on.
     * @param offsetX The x-coordinate, starting from an offset of 8.
     * @param y       The y-coordinate of the starting position.
     * @param z       The z-coordinate of the starting position.
     * @return The position of the opening, or else <code>NONE_FOUND</code>.
     */
    protected final int findOpeningEast(final Level level, final int y, final int z, final int offsetX) {
        final MutableBlockPos pos = new MutableBlockPos(offsetX, y, z);
        final boolean solid = isSolid(level, pos);
        pos.setX(offsetX + 1);

        while (pos.getX() < offsetX + 16) {
            if (solid != isSolid(level, pos)) {
                return pos.getX();
            }
            pos.setX(pos.getX() + 1);
        }
        return NONE_FOUND;
    }

    /**
     * Locates the first cave opening <b>or surface</b> from east within the specified range.
     *
     * @param level   The current world being operated on.
     * @param offsetX The x-coordinate, starting from an offset of 8.
     * @param y       The y-coordinate of the starting position.
     * @param z       The z-coordinate of the starting position.
     * @return The position of the opening, or else <code>NONE_FOUND</code>.
     */
    protected final int findOpeningWest(final Level level, final int y, final int z, final int offsetX) {
        final MutableBlockPos pos = new MutableBlockPos(offsetX + 15, y, z);
        final boolean solid = isSolid(level, pos);
        pos.setX(offsetX + 14);

        while (pos.getX() >= offsetX) {
            if (solid != isSolid(level, pos)) {
                return pos.getX();
            }
            pos.setX(pos.getX() - 1);
        }
        return NONE_FOUND;
    }

    /**
     * Determines whether the block at the input location should spawn, according to an
     * array of matcher blocks.
     *
     * @param matchers A list of valid block states.
     * @param level    The current world being operated on.
     * @param pos      The position of the block in question.
     * @return Whether Whether the array contains the block at the given coordinates.
     */
    protected final boolean checkSources(final List<BlockState> matchers, final Level level, final BlockPos pos) {
        // No matchers -> always spawn.
        if (matchers.isEmpty()) {
            return true;
        }
        return matchers.contains(level.getBlockState(pos));
    }

    /**
     * Determines whether non-solid blocks exist at <b>all</b> of the relative coordinates.
     *
     * @param relative A list of relative block coordinates.
     * @param level    The current world being operated on.
     * @param origin   The position of the block in question.
     * @return Whether Whether <b>all</b> of the blocks are non-solid.
     */
    protected final boolean checkNonSolid(final List<BlockPos> relative, final Level level, final BlockPos origin) {
        for (final BlockPos p : relative) {
            if (isSolid(level, origin.offset(p.getX(), p.getY(), p.getZ()))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Determines whether solid blocks exist at <b>all</b> of the relative coordinates.
     *
     * @param relative A list of relative block coordinates.
     * @param level    The current world being operated on.
     * @param origin   The position of the block in question.
     * @return Whether <b>all</b> of the blocks are solid.
     */
    protected final boolean checkSolid(final List<BlockPos> relative, final Level level, final BlockPos origin) {
        for (final BlockPos p : relative) {
            if (!isSolid(level, origin.offset(p.getX(), p.getY(), p.getZ()))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Determines whether air blocks exist at each of the relative coordinates.
     *
     * @param relative A list of relative coordinates.
     * @param level    The current world being operated on.
     * @param origin   The position of the block in question.
     * @return Whether <b>all</b> of the blocks are air blocks.
     */
    protected final boolean checkAir(final List<BlockPos> relative, final Level level, final BlockPos origin) {
        for (final BlockPos p : relative) {
            if (!level.getBlockState(origin.offset(p.getX(), p.getY(), p.getZ())).isAir()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Determines whether water blocks exist at each of the relative coordinates.
     *
     * Todo: Consider replacing this with "check fluid."
     *
     * @param relative A list of relative coordinates.
     * @param level    The current world being operated on.
     * @param origin   The position of the block in question.
     * @return Whether <b>all</b> of the blocks are water blocks.
     */
    protected final boolean checkWater(final List<BlockPos> relative, final Level level, final BlockPos origin) {
        for (final BlockPos p : relative) {
            if (!level.getBlockState(origin.offset(p.getX(), p.getY(), p.getZ())).equals(Blocks.WATER.defaultBlockState())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Determines whether any block from a specific set of blocks exists at each of
     * the given relative coordinates.
     *
     * @param checks A list expected blocks and their respective relative coordinates.
     * @param level  The current world being operated on.
     * @param origin The position of the block in question.
     * @return Whether <b>all</b> of the blocks are water blocks.
     */
    protected final boolean checkBlocks(final List<BlockCheck> checks, final Level level, final BlockPos origin) {
        for (final BlockCheck c : checks) {
            for (final BlockPos p : c.positions) {
                final BlockState state = level.getBlockState(origin.offset(p.getX(), p.getY(), p.getZ()));
                if (!c.matchers.contains(state)) {
                    return false;
                }
            }
        }
        return true;
    }
}
