package com.personthecat.cavegenerator.world.feature;

import com.personthecat.cavegenerator.data.ConditionSettings;
import com.personthecat.cavegenerator.model.BlockCheck;
import com.personthecat.cavegenerator.model.Conditions;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import static com.personthecat.cavegenerator.util.CommonMethods.numBetween;

public abstract class FeatureGenerator {

    /** The number of times to try locating vertical surfaces for structures. */
    protected static final int VERTICAL_RETRIES = 3;

    /** The number of times to try locating horizontal surfaces for structures. */
    protected static final int HORIZONTAL_RETRIES = 20;

    /** The value returned by any surface locator when no surface is found. */
    protected static final int NONE_FOUND = Integer.MIN_VALUE;

    /** Minimum distance below the surface for all late features. */
    protected static final int SURFACE_ROOM = 5;

    protected final Conditions conditions;
    protected final WeakReference<World> world;

    public FeatureGenerator(ConditionSettings conditions, World world) {
        Objects.requireNonNull(world, "Nullable world types are not yet supported.");
        this.conditions = Conditions.compile(conditions, world);
        this.world = new WeakReference<>(world);
    }

    protected final World getWorld() {
        return Objects.requireNonNull(world.get(), "World reference has been culled.");
    }

    public final void generate(WorldContext ctx) {
        final int dim = ctx.world.provider.getDimension();
        if (conditions.dimensions.test(dim)) {
            doGenerate(ctx);
        }
    }

    protected abstract void doGenerate(WorldContext ctx);

    /** Determines whether the IBlockState at the input coordinates is an opaque cube. */
    protected final boolean isSolid(World world, int x, int y, int z) {
        return isSolid(world, new BlockPos(x, y, z));
    }

    /** Determines whether the IBlockState at the input coordinates is an opaque cube. */
    protected final boolean isSolid(World world, BlockPos pos) {
        return world.getBlockState(pos).isOpaqueCube();
    }

    /**
     * Locates the first cave opening from above within the specified range.
     * Returns NONE_FOUND when no opening is found.
     */
    protected final int findFloor(World world, int x, int y, int z, int minY) {
        boolean previouslySolid = isSolid(world, x, y, z);
        for (int h = y - 1; h > minY; h--) {
            final boolean currentlySolid = isSolid(world, x, h, z);
            if (!previouslySolid && currentlySolid) {
                return h;
            }
            previouslySolid = currentlySolid;
        }
        return NONE_FOUND;
    }

    /**
     * Locates the first cave opening from below within the specified range.
     * Returns NONE_FOUND when no opening is found.
     */
    protected final int findCeiling(World world, int x, int y, int z, int maxY) {
        boolean previouslySolid = isSolid(world, x, y, z);
        for (int h = y + 1; h < maxY; h++) {
            final boolean currentlySolid = isSolid(world, x, h, z);
            if (!previouslySolid && currentlySolid) {
                return h;
            }
            previouslySolid = currentlySolid;
        }
        return NONE_FOUND;
    }

    /**
     * Randomly locates a cave opening from above within the specified range.
     * Starts at a random coordinate, then starts from the top, if nothing is found.
     * Returns NONE_FOUND when no opening is found.
     */
    protected final int randFindFloor(World world, Random rand, int x, int z, int minY, int maxY) {
        // Start at a random coordinate. Then try from the top, if nothing is found.
        final int startY = numBetween(rand, minY, maxY);
        int y = findFloor(world, x, startY, z, minY);
        if (y == NONE_FOUND) {
            y = findFloor(world, x, maxY, z, startY);
        }
        return y;
    }

    /**
     * Randomly locates a cave opening from below within the specified range.
     * Starts at a random coordinate, then starts from the bottom, if nothing is found.
     * Returns NONE_FOUND when no opening is found.
     */
    protected final int randFindCeiling(World world, Random rand, int x, int z, int minY, int maxY) {
        // Start at a random coordinate. Then try from the top, if nothing is found.
        final int startY = numBetween(rand, minY, maxY);
        int y = findCeiling(world, x, startY, z, maxY);
        if (y == NONE_FOUND) {
            y = findCeiling(world, x, minY, z, startY);
        }
        return y;
    }

    /**
     * Locates the first cave opening from a random coordinate, randomly searching up or down.
     * Returns NONE_FOUND when no opening is found.
     */
    protected final int findOpeningVertical(Random rand, World world, int x, int z, int minY, int maxY) {
        final int startY = numBetween(rand, minY, maxY);
        if (rand.nextBoolean()) {
            // First search from the center up.
            final int fromCenter = findOpeningFromBelow(world, x, startY, z, maxY);
            if (fromCenter != NONE_FOUND) {
                return fromCenter;
            }
            // Then try from the bottom to the center.
            return findOpeningFromBelow(world, x, minY, z, startY);
        } else {
            final int fromCenter = findOpeningFromAbove(world, x, startY, z, minY);
            if (fromCenter != NONE_FOUND) {
                return fromCenter;
            }
            return findOpeningFromAbove(world, x, maxY, z, startY);
        }
    }

    /**
     * Searches up until an opening is found, either ceiling or floor.
     * Returns NONE_FOUND when no opening is found.
     */
    protected final int findOpeningFromBelow(World world, int x, int y, int z, int maxY) {
        boolean previouslySolid = isSolid(world, x, y, z);
        for (int h = y; h < maxY; h++) {
            final boolean currentlySolid = isSolid(world, x, h, z);
            if (previouslySolid ^ currentlySolid) {
                return currentlySolid ? h : h - 1;
            }
            previouslySolid = currentlySolid;
        }
        return NONE_FOUND;
    }

    /**
     * Searches down until an opening is found, either ceiling or floor.
     * Returns NONE_FOUND when no opening is found.
     */
    protected final int findOpeningFromAbove(World world, int x, int y, int z, int minY) {
        boolean previouslySolid = isSolid(world, x, y, z);
        for (int h = y; h > minY; h--) {
            final boolean currentlySolid = isSolid(world, x, h, z);
            if (previouslySolid ^ currentlySolid) {
                return currentlySolid ? h : h + 1;
            }
            previouslySolid = currentlySolid;
        }
        return NONE_FOUND;
    }

    /**
     * Searches north with an offset of 8 until an opening is found.
     * Returns NONE_FOUND if no opening is found.
     */
    protected final int findOpeningNorth(World world, int x, int y, int offsetZ) {
        boolean previouslySolid = isSolid(world, x, y, offsetZ + 15);
        for (int z = offsetZ + 14; z >= offsetZ; z--) {
            final boolean currentlySolid = isSolid(world, x, y, z);
            if (previouslySolid ^ currentlySolid) {
                return z;
            }
            previouslySolid = currentlySolid;
        }
        return NONE_FOUND;
    }

    /**
     * Searches south with an offset of 8 until an opening is found.
     * Returns NONE_FOUND if no opening is found.
     */
    protected final int findOpeningSouth(World world, int x, int y, int offsetZ) {
        boolean previouslySolid = isSolid(world, x, y, offsetZ);
        for (int z = offsetZ + 1; z < offsetZ + 16; z++) {
            final boolean currentlySolid = isSolid(world, x, y, z);
            if (previouslySolid ^ currentlySolid) {
                return z;
            }
            previouslySolid = currentlySolid;
        }
        return NONE_FOUND;
    }

    /**
     * Searches east with an offset of 8 until an opening is found.
     * Returns NONE_FOUND if no opening is found.
     */
    protected final int findOpeningEast(World world, int y, int z, int offsetX) {
        boolean previouslySolid = isSolid(world, offsetX, y, z);
        for (int x = offsetX + 1; x < offsetX + 16; x++) {
            final boolean currentlySolid = isSolid(world, x, y, z);
            if (previouslySolid ^ currentlySolid) {
                return x;
            }
            previouslySolid = currentlySolid;
        }
        return NONE_FOUND;
    }

    /**
     * Searches west with an offset of 8 until an opening is found.
     * Returns NONE_FOUND if no opening is found.
     */
    protected final int findOpeningWest(World world, int y, int z, int offsetX) {
        boolean previouslySolid = isSolid(world, offsetX + 15, y, z);
        for (int x = offsetX + 14; x >= offsetX; x--) {
            final boolean currentlySolid = isSolid(world, x, y, z);
            if (previouslySolid ^ currentlySolid) {
                return x;
            }
            previouslySolid = currentlySolid;
        }
        return NONE_FOUND;
    }

    /**
     * Determines whether the block at the input location should spawn,
     * according to an array of matcher blocks.
     */
    protected final boolean checkSources(List<IBlockState> matchers, World world, BlockPos pos) {
        // No matchers -> always spawn.
        if (matchers.isEmpty()) {
            return true;
        }
        return matchers.contains(world.getBlockState(pos));
    }

    /** Determines whether non-solid blocks exist at each of the relative coordinates. */
    protected final boolean checkNonSolid(List<BlockPos> relative, World world, BlockPos origin) {
        for (BlockPos p : relative) {
            if (isSolid(world, origin.add(p.getX(), p.getY(), p.getZ()))) {
                return false;
            }
        }
        return true;
    }

    /** Determines whether solid blocks exist at each of the relative coordinates. */
    protected final boolean checkSolid(List<BlockPos> relative, World world, BlockPos origin) {
        for (BlockPos p : relative) {
            if (!isSolid(world, origin.add(p.getX(), p.getY(), p.getZ()))) {
                return false;
            }
        }
        return true;
    }

    /** Determines whether air blocks exist at each of the relative coordinates. */
    protected final boolean checkAir(List<BlockPos> relative, World world, BlockPos origin) {
        for (BlockPos p : relative) {
            if (!world.getBlockState(origin.add(p.getX(), p.getY(), p.getZ())).equals(Blocks.AIR.getDefaultState())) {
                return false;
            }
        }
        return true;
    }

    /** Determines whether air blocks exist at each of the relative coordinates. */
    protected final boolean checkWater(List<BlockPos> relative, World world, BlockPos origin) {
        for (BlockPos p : relative) {
            if (!world.getBlockState(origin.add(p.getX(), p.getY(), p.getZ())).equals(Blocks.WATER.getDefaultState())) {
                return false;
            }
        }
        return true;
    }

    /** Determines whether specific blocks exists at each of the relative coordinates. */
    protected final boolean checkBlocks(List<BlockCheck> checks, World world, BlockPos origin) {
        for (BlockCheck c : checks) {
            for (BlockPos p : c.positions) {
                final IBlockState state = world.getBlockState(origin.add(p.getX(), p.getY(), p.getZ()));
                if (!c.matchers.contains(state)) {
                    return false;
                }
            }
        }
        return true;
    }

}
