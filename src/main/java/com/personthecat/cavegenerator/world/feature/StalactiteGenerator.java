package com.personthecat.cavegenerator.world.feature;

import com.personthecat.cavegenerator.data.StalactiteSettings;
import com.personthecat.cavegenerator.model.Range;
import com.personthecat.cavegenerator.util.XoRoShiRo;
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
    protected void doGenerate(WorldContext ctx) {
        final Random localRand = new XoRoShiRo(ctx.rand.nextInt());
        // Each iteration increments by `distance`. This changes the frequency
        // with which `noise` is calculated, theoretically impacting performance.
        // Lower frequencies do not require as high a resolution, as this
        // difference would typically not be visible.
        for (int x = ctx.offsetX; x < ctx.offsetX + 16; x = x + resolution) {
            for (int z = ctx.offsetZ; z < ctx.offsetZ + 16; z = z + resolution) {
                final Biome biome = ctx.world.getBiome(new BlockPos(x, 0, z));
                if (conditions.biomes.test(biome) && conditions.region.GetBoolean(x, z)) {
                    generateRegion(ctx, localRand, x, z);
                }
            }
        }
    }

    /** Attempts to spawn a stalactite at every coordinate pair in this region. */
    private void generateRegion(WorldContext info, Random rand, int x, int z) {
        for (int dx = x; dx < x + resolution; dx++) {
            for (int dz = z; dz < z + resolution; dz++) {
                // Check this earlier -> do less when it fails.
                if (rand.nextDouble() >= cfg.chance) {
                    continue;
                }
                final Range height = conditions.getColumn(dx, dz);
                if (height.diff() != 0 && conditions.region.GetBoolean(dz, dz)) {
                    final int y = StalactiteSettings.Type.STALACTITE.equals(cfg.type) ?
                        findCeiling(info.world, dx, height.min, dz, height.max):
                        findFloor(info.world, dx, height.max, dz, height.min);

                    if (y != NONE_FOUND && conditions.noise.GetBoolean(dx, y, dz)) {
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
        int length = cfg.length.rand(rand);
        if (hasEnoughSpace(world, pos, length + cfg.space)) {
            this.place(world, pos, length);
            if (length > 2 && cfg.wide) {
                this.placeAll(world, rand, length * 2 / 3, sidePositions(pos));
                this.placeAll(world, rand, length / 4, cornerPositions(pos));
                if (length > 5 && cfg.giant) {
                     this.placeAll(world, rand, length / 4, outerSidePositions(pos));
                     this.placeAll(world, rand, length / 6, outerCornerPositions(pos));
                }
            }
        }
    }

    private boolean hasEnoughSpace(World world, BlockPos pos, int space) {
        for (int i = 0; i < space; i++) {
            pos = StalactiteSettings.Type.STALACTITE.equals(cfg.type) ? pos.down() : pos.up();
            if (world.getBlockState(pos).isOpaqueCube()) {
                return false;
            }
        }
        return true;
    }

    private void place(World world, BlockPos pos, int length) {
        for (int i = 0; i < length; i++) {
            pos = StalactiteSettings.Type.STALACTITE.equals(cfg.type) ? pos.down() : pos.up();
            world.setBlockState(pos, cfg.state, 16);
        }
    }

    private void placeAll(World world, Random rand, int length, BlockPos[] positions) {
        for (BlockPos pos : positions) {
            if (!cfg.symmetrical) {
                final int min = length / 2;
                length = rand.nextInt(length - min + 1) + min;
            }
            this.findPlace(world, pos, length);
        }
    }

    private void findPlace(World world, BlockPos pos, int length) {
        for (int i = 0; i < 3; i++) {
            if (world.getBlockState(pos).isOpaqueCube()) {
                this.place(world, pos, length);
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

    private static BlockPos[] outerSidePositions(BlockPos pos) {
        final int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        return new BlockPos[] {
            new BlockPos(x, y, z - 2), // North
            new BlockPos(x, y, z + 2), // South
            new BlockPos(x + 2, y, z), // East
            new BlockPos(x - 2, y, z)  // West
        };
    }

    private static BlockPos[] outerCornerPositions(BlockPos pos) {
        final int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        return new BlockPos[] {
            new BlockPos(x + 2, y, z + 1),
            new BlockPos(x + 2, y, z - 1),
            new BlockPos(x - 2, y, z + 1),
            new BlockPos(x - 2, y, z - 1),
            new BlockPos(x + 1, y, z + 2),
            new BlockPos(x - 1, y, z + 2),
            new BlockPos(x + 1, y, z - 2),
            new BlockPos(x - 1, y, z - 2)
        };
    }
}