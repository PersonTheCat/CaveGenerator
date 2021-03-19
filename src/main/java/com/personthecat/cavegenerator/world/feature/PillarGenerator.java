package com.personthecat.cavegenerator.world.feature;

import com.personthecat.cavegenerator.data.PillarSettings;
import com.personthecat.cavegenerator.model.Range;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Random;

import static net.minecraft.block.BlockStairs.EnumHalf;
import static net.minecraft.block.BlockStairs.EnumShape;

@ParametersAreNonnullByDefault
public class PillarGenerator extends FeatureGenerator {

    private static final IBlockState BLK_AIR = Blocks.AIR.getDefaultState();

    private final PillarSettings cfg;

    public PillarGenerator(PillarSettings cfg, World world) {
        super(cfg.conditions, world);
        this.cfg = cfg;
    }

    protected void doGenerate(FeatureInfo info) {
        final Random rand = info.rand;
        for (int i = 0; i < rand.nextInt(cfg.frequency + 1); i++) {
            // Avoid pillars spawning right next to each other.
            final int x = ((rand.nextInt(6) * 2) + 2) + (info.chunkX * 16); // 2 to 14
            final int z = ((rand.nextInt(6) * 2) + 1) + (info.chunkZ * 16); // 1 to 13
            final Biome biome = info.world.getBiome(new BlockPos(x, 0, z));

            if (conditions.biomes.test(biome)) {
                final Range height = conditions.getColumn(x, z);

                if (height.diff() != 0) {
                    final int y = height.rand(rand);
                    final int opening = findCeiling(info.world, x, y, z, height.max);
                    if (opening != NONE_FOUND) {
                        generateSingle(info.world, info.rand, new BlockPos(x, opening, z));
                    }
                }
            }
        }
    }

    /** @param pos is the top block in the pillar. */
    private void generateSingle(World world, Random rand, BlockPos pos) {
        final int actualMax = pos.getY();
        final int actualMin = getLowestBlock(world, pos);
        // Verify that the position is possible.
        if (actualMin < 0) return;

        int length = actualMax - actualMin;
        // Ensure that the difference is within the specified bounds.
        if (length < cfg.length.min || length > cfg.length.max) return;

        for (int y = actualMax; y >= actualMin; y--) {
            BlockPos current = new BlockPos(pos.getX(), y, pos.getZ());
            // Start by placing the initial block.
            world.setBlockState(current, cfg.state, 2);

            // Handle stair blocks, if applicable.
            if (cfg.stairBlock.isPresent()) {
                if (y == actualMax) { // We're at the top. Place stairs upward.
                    testPlaceStairs(world, cfg.stairBlock.get(), rand, pos, EnumHalf.TOP);
                } else if (y == actualMin) { // We're at the bottom. Place stairs downward.
                    testPlaceStairs(world, cfg.stairBlock.get(), rand, current, EnumHalf.BOTTOM);
                }
            }
        }
    }

    /** Determines the lowest block at the given X, Z coordinates. */
    private int getLowestBlock(World world, BlockPos pos) {
        for (pos = pos.down(); pos.getY() > cfg.conditions.height.min; pos = pos.down()) {
            if (world.getBlockState(pos).isOpaqueCube()) {
                // We're going down. There was air above us, but not at this position.
                // This is the bottom.
                return pos.getY();
            }
        }
        // Nothing was found. Just return -1 instead of boxing it in some container.
        return -1;
    }

    /** Tries to randomly place stair blocks around the pillar in all 4 directions. */
    private void testPlaceStairs(World world, BlockStairs stairs, Random rand, BlockPos pos, EnumHalf topOrBottom) {
        if (topOrBottom.equals(EnumHalf.TOP)) {
            testPlaceUp(stairs, pos.north(), EnumFacing.SOUTH, rand, world, topOrBottom);
            testPlaceUp(stairs, pos.south(), EnumFacing.NORTH, rand, world, topOrBottom);
            testPlaceUp(stairs, pos.east(), EnumFacing.WEST, rand, world, topOrBottom);
            testPlaceUp(stairs, pos.west(), EnumFacing.EAST, rand, world, topOrBottom);
        } else {
            testPlaceDown(stairs, pos.north(), EnumFacing.SOUTH, rand, world, topOrBottom);
            testPlaceDown(stairs, pos.south(), EnumFacing.NORTH, rand, world, topOrBottom);
            testPlaceDown(stairs, pos.east(), EnumFacing.WEST, rand, world, topOrBottom);
            testPlaceDown(stairs, pos.west(), EnumFacing.EAST, rand, world, topOrBottom);
        }
    }

    /**
     * Randomly looks +-3 blocks vertically surrounding the given X, Z
     * coordinates. Places stairs given a ~1/3 chance.
     */
    private void testPlaceDown(BlockStairs stairs, BlockPos pos, EnumFacing facing, Random rand, World world, EnumHalf topOrBottom) {
        BlockPos previous = pos.down(4);
        // Iterate 3 blocks down, 3 blocks up.
        for (int i = - 3; i <= 3; i++) {
            final BlockPos current = pos.up(i);
            // Verify that we're within the height bounds. Stop randomly.
            if (current.getY() >= cfg.conditions.height.min && rand.nextInt(2) == 0) {
                // Find a boundary between solid and air.
                if (world.getBlockState(previous).isOpaqueCube() && world.getBlockState(current).equals(BLK_AIR)) {
                    // Replace air.
                    world.setBlockState(current, getStairRotation(stairs, facing, topOrBottom), 16);
                    return;
                }
            }
            previous = current;
        }
    }

    private void testPlaceUp(BlockStairs stairs, BlockPos pos, EnumFacing facing, Random rand, World world, EnumHalf topOrBottom) {
        BlockPos previous = pos.up(4);
        for (int i =  3; i >= - 3; i--) {
            final BlockPos current = pos.up(i);
            if (current.getY() >= cfg.conditions.height.min && rand.nextInt(2) == 0) {
                // pos.up will add or subtract and is thus still valid.
                if (world.getBlockState(previous).isOpaqueCube() && world.getBlockState(current).equals(BLK_AIR)) {
                    world.setBlockState(current, getStairRotation(stairs, facing, topOrBottom), 16);
                    return;
                }
            }
            previous = current;
        }
    }

    /**
     * Determines the correct stair rotation for the given properties.
     * It may make sense to pre-calculate this.
     */
    private IBlockState getStairRotation(BlockStairs stairs, EnumFacing facing, EnumHalf topOrBottom) {
        // The null check is handled above.
        return stairs.getDefaultState()
            .withProperty(BlockStairs.FACING, facing)
            .withProperty(BlockStairs.HALF, topOrBottom)
            .withProperty(BlockStairs.SHAPE, EnumShape.STRAIGHT);
    }
}