package com.personthecat.cavegenerator.world.feature;

import com.personthecat.cavegenerator.data.StalactiteSettings;
import com.personthecat.cavegenerator.model.Range;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Random;

@ParametersAreNonnullByDefault
public class StalactiteGenerator extends FeatureGenerator {

    private final StalactiteSettings cfg;
    private final int resolution;

    public StalactiteGenerator(StalactiteSettings cfg, World world) {
        super(cfg.conditions, world);
        this.cfg = cfg;
        this.resolution = calculateResolution(cfg.chance);
    }

    /**
     * Determines the number of blocks between noise calculations.
     * Higher frequency -> higher resolution -> lower distance.
     */
    private static int calculateResolution(double chance) {
        if (chance < 25.0) return 16; // 00 to 25 -> 1x / chunk
        if (chance < 35.0) return 8;  // 25 to 35 -> 4x / chunk
        if (chance < 55.0) return 4;  // 35 to 55 -> 16x / chunk
        if (chance < 75.0) return 2;  // 55 to 75 -> 64x / chunk
        return 1;                     // 75 to 100 -> 256x / chunk
    }

    @Override
    protected void doGenerate(FeatureInfo info) {
        final Random localRand = new Random(info.rand.nextInt());
        // Each iteration increments by `distance`. This changes the frequency
        // with which `noise` is calculated, theoretically impacting performance.
        // Lower frequencies do not require as high a resolution, as this
        // difference would typically not be visible.
        for (int x = info.offsetX; x < info.offsetX + 16; x = x + resolution) {
            for (int z = info.offsetZ; z < info.offsetZ + 16; z = z + resolution) {
                final Biome biome = info.world.getBiome(new BlockPos(x, 0, z));
                if (conditions.biomes.test(biome) && conditions.region.GetBoolean(x, z)) {
                    generateRegion(info, localRand, x, z);
                }
            }
        }
    }

    /** Attempts to spawn a stalactite at every coordinate pair in this region. */
    private void generateRegion(FeatureInfo info, Random rand, int x, int z) {
        for (int dx = x; dx < x + resolution; dx++) {
            for (int dz = z; dz < z + resolution; dz++) {
                // Check this earlier -> do less when it fails.
                if (rand.nextDouble() >= cfg.chance) {
                    continue;
                }
                final Range height = conditions.getColumn(dx, dz);
                if (height.diff() != 0) {
                    final int y = StalactiteSettings.Type.STALACTITE.equals(cfg.type) ?
                        findCeiling(info.world, dx, height.min, dz, height.max):
                        findFloor(info.world, dx, height.max, dz, height.min);

                    if (y != NONE_FOUND) {
                        final BlockPos pos = new BlockPos(dx, y, dz);
                        if (checkSources(cfg.matchers, info.world, pos)) {
                            generateSingle(info.world, rand, pos);
                        }
                    }
                }
            }
        }
    }

    private void generateSingle(World world, Random rand, BlockPos pos) {
        if (cfg.wide) {
            int length = place(world, rand, pos, cfg.maxLength, 4);
            placeSides(world, rand, pos, length * 2 / 3, length + 1);
            placeCorners(world, rand, pos, length / 4, 3);
        } else { // Just place the single column and stop.
            place(world, rand, pos, cfg.maxLength, 3);
        }
    }

    private int place(World world, Random rand, BlockPos start, int length, int stopChance) {
        BlockPos pos = start;
        int i = 1; // Skip the initial position. It's the surface.
        for (; i < length; i++) {
            // Determine whether to go up or down.
            pos = StalactiteSettings.Type.STALACTITE.equals(cfg.type) ? pos.down() : pos.up();
            // Stop randomly / when the current block is solid.
            if (world.getBlockState(pos).isOpaqueCube() || rand.nextInt(stopChance) == 0) {
                break;
            } // Set the new state.
            world.setBlockState(pos, cfg.state, 16);
        }
        return i; // Return the actual length.
    }

    private void placeSides(World world, Random rand, BlockPos pos, int length, int stopChance) {
        for (BlockPos cardinal : sidePositions(pos)) {
            findPlace(world, rand, cardinal, length, stopChance);
        }
    }

    private void placeCorners(World world, Random rand, BlockPos pos, int length, int stopChance) {
        for (BlockPos ordinal : cornerPositions(pos)) {
            findPlace(world, rand, ordinal, length, stopChance);
        }
    }

    private void findPlace(World world, Random rand, BlockPos pos, int length, int stopChance) {
        for (int i = 0; i < 3; i++) {
            if (world.getBlockState(pos).isOpaqueCube()) {
                place(world, rand, pos, length, stopChance);
                return;
            } // Go in the opposite direction and find a surface.
            pos = StalactiteSettings.Type.STALACTITE.equals(cfg.type) ? pos.up() : pos.down();
        }
    }

    private static BlockPos[] sidePositions(BlockPos pos) {
        final int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        return new BlockPos[] {
            new BlockPos(x, y, z - 1), // North
            new BlockPos(x, y, z + 1), // South
            new BlockPos(x + 1, y, z), // East
            new BlockPos(x - 1, y, z)  // West
        };
    }

    private static BlockPos[] cornerPositions(BlockPos pos) {
        final int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        return new BlockPos[] {
            new BlockPos(x + 1, y, z - 1), // Northeast
            new BlockPos(x + 1, y, z + 1), // Southeast
            new BlockPos(x - 1, y, z + 1), // Southwest
            new BlockPos(x - 1, y, z - 1)  // Northwest
        };
    }
}