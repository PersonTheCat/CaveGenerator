package com.personthecat.cavegenerator.world.feature;

import com.personthecat.cavegenerator.data.VineSettings;
import com.personthecat.cavegenerator.model.Range;
import net.minecraft.block.BlockVine;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Optional;
import java.util.Random;

import static com.personthecat.cavegenerator.util.CommonMethods.empty;
import static com.personthecat.cavegenerator.util.CommonMethods.full;

@ParametersAreNonnullByDefault
public class VineGenerator extends FeatureGenerator {

    private final VineSettings cfg;

    public VineGenerator(VineSettings cfg, World world) {
        super(cfg.conditions, world);
        this.cfg = cfg;
    }

    @Override
    protected void doGenerate(WorldContext ctx) {
        final Random rand = ctx.rand;
        for (int i = 0; i < rand.nextInt(cfg.count + 1); i++) {
            final int x = ctx.offsetX + rand.nextInt(16);
            final int z = ctx.offsetZ + rand.nextInt(16);
            final Biome biome = ctx.world.getBiome(new BlockPos(x, 0, z));

            if (conditions.biomes.test(biome)) {
                final Range height = conditions.getColumn(x, z);

                if (!height.isEmpty() && conditions.region.GetBoolean(x, z)) {
                    final int y = height.rand(rand);
                    final int length = cfg.length.rand(rand);
                    final Optional<BlockPos> found = this.findSpawnPos(ctx.world, rand, length, x, y, z, height.max);
                    if (!found.isPresent()) {
                        break;
                    }
                    final BlockPos pos = found.get();
                    if (conditions.noise.GetBoolean(pos.getX(), pos.getY(), pos.getZ())) {
                        if (this.hasSpaceDown(ctx.world, pos, length)) {
                            this.generateSingle(ctx.world, length, pos);
                        }
                    }
                }
            }
        }
    }

    private Optional<BlockPos> findSpawnPos(World world, Random rand, int length, int x, int y, int z, int max) {
        final BlockPos pos = new BlockPos(x, y, z);
        if (!world.isAirBlock(pos)) {
            y = this.findTop(world, pos, length, max);
            if (y == NONE_FOUND) return empty();
        }
        final int dir = rand.nextInt(4);
        if (dir == 0) {
            z = this.findOpeningNorth(world, x, y, z - 8);
            if (z == NONE_FOUND) return empty();
        } else if (dir == 1) {
            z = this.findOpeningSouth(world, x, y, z - 8);
            if (z == NONE_FOUND) return empty();
        } else if (dir == 2) {
            x = this.findOpeningEast(world, y, z, x - 8);
            if (x == NONE_FOUND) return empty();
        } else {
            x = this.findOpeningWest(world, y, z, x - 8);
            if (x == NONE_FOUND) return empty();
        }
        return full(new BlockPos(x, y, z));
    }

    private int findTop(World world, BlockPos pos, int length, int max) {
        final int floor = this.findFloorUp(world, pos, max);
        if (floor != NONE_FOUND && this.hasSpaceUp(world, pos, length)) {
            return floor + length;
        }
        return NONE_FOUND;
    }

    private int findFloorUp(World world, BlockPos pos, int max) {
        for (; pos.getY() < max; pos = pos.up()) {
            if (world.isAirBlock(pos)) {
                return pos.getY();
            }
        }
        return NONE_FOUND;
    }

    private boolean hasSpaceUp(World world, BlockPos pos, int length) {
        final int max = pos.getY() + length;
        for (; pos.getY() < max; pos = pos.up()) {
            if (!world.isAirBlock(pos)) {
                return false;
            }
        }
        return true;
    }

    private boolean hasSpaceDown(World world, BlockPos pos, int length) {
        final int min = pos.getY() - length;
        for (; pos.getY() > min; pos = pos.down()) {
            if (!world.isAirBlock(pos)) {
                return false;
            }
        }
        return true;
    }

    /** @param pos is the top block in the vine. */
    private void generateSingle(World world, int length, BlockPos pos) {
        final int min = pos.getY() - length;
        IBlockState vine = Blocks.VINE.getDefaultState();
        for (; pos.getY() >= min; pos = pos.down()) {
            for (EnumFacing dir : EnumFacing.Plane.HORIZONTAL.facings()) {
                if (Blocks.VINE.canPlaceBlockOnSide(world, pos, dir)) {
                    vine = Blocks.VINE.getDefaultState()
                        .withProperty(BlockVine.NORTH, dir == EnumFacing.SOUTH)
                        .withProperty(BlockVine.EAST, dir == EnumFacing.WEST)
                        .withProperty(BlockVine.SOUTH, dir == EnumFacing.NORTH)
                        .withProperty(BlockVine.WEST, dir == EnumFacing.EAST);
                    break;
                }
            }
            world.setBlockState(pos, vine, 2);
        }
    }
}
