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
    private final boolean speleothem;
    private final boolean stalactite;
    private final boolean stalagmite;

    public StalactiteGenerator(StalactiteSettings cfg, World world) {
        super(cfg.conditions, world);
        this.cfg = cfg;
        this.resolution = calculateResolution(cfg.chance);
        this.speleothem = cfg.type.equals(StalactiteSettings.Type.SPELEOTHEM);
        this.stalactite = this.speleothem || cfg.type.equals(StalactiteSettings.Type.STALACTITE);
        this.stalagmite = this.speleothem || cfg.type.equals(StalactiteSettings.Type.STALAGMITE);
    }

    /**
     * Determines the number of blocks between noise calculations.
     * Higher frequency -> higher resolution -> lower distance.
     */
    private static int calculateResolution(double chance) {
        if (chance < 0.25) return 16; // 00 to 25 -> 1x / chunk
        if (chance < 0.35) return 8;  // 25 to 35 -> 4x / chunk
        if (chance < 0.55) return 4;  // 35 to 55 -> 16x / chunk
        if (chance < 0.75) return 2;  // 55 to 75 -> 64x / chunk
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
                    this.generateRegion(ctx, localRand, x, z);
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
                    final int maxY = Math.min(info.heightmap[dx & 15][dz & 15] - SURFACE_ROOM, height.max);
                    if (maxY <= height.min) continue;

                    final int y = this.stalactite ? this.findCeiling(info.world, dx, height.min, dz, maxY)
                        : this.findFloor(info.world, dx, maxY, dz, height.min);

                    if (y != NONE_FOUND && conditions.noise.GetBoolean(dx, y, dz)) {
                        final BlockPos pos = new BlockPos(dx, y, dz);
                        if (checkSources(cfg.matchers, info.world, pos)) {
                            this.checkSpace(info.world, rand, pos);
                        }
                    }
                }
            }
        }
    }

    private void checkSpace(World world, Random rand, BlockPos pos) {
        final int length = this.cfg.length.rand(rand);
        final int needed = this.cfg.space + length;
        final int space = this.getSpace(world, pos, needed);
        if (this.speleothem && space > 2 * needed) {
            return;
        }
        if (space >= needed) {
            this.generateSingle(world, rand, pos, length, !this.stalactite);
            if (this.speleothem) {
                this.generateSingle(world, rand, pos.down(space), length, true);
            }
        }
    }

    private void generateSingle(World world, Random rand, BlockPos pos, int length, boolean up) {
        this.place(world, pos, length, up);
        if (length > 2 && cfg.size != StalactiteSettings.Size.SMALL) {
            this.placeAll(world, rand, length * 2 / 3, up, sidePositions(pos));
            this.placeAll(world, rand, length / 4, up, cornerPositions(pos));
            if (length > 5 && cfg.size.ordinal() > StalactiteSettings.Size.MEDIUM.ordinal()) {
                this.placeAll(world, rand, length / 4, up, middleSidePositions(pos));
                this.placeAll(world, rand, length / 6, up, middleCornerPositions(pos));
                if (length > 9 && cfg.size.ordinal() > StalactiteSettings.Size.LARGE.ordinal()) {
                    this.placeAll(world, rand, length / 8, up, outerSidePositions(pos));
                    this.placeAll(world, rand, length / 11, up, outerCornerPositions(pos));
                    this.placeAll(world, rand, length / 8, up, outerBetweenPositions(pos));
                }
            }
        }
    }

    private int getSpace(World world, BlockPos pos, int max) {
        for (int i = 0; i < max; i++) {
            // We will be at the top and need to look down.
            pos = this.stalactite ? pos.down() : pos.up();
            if (world.getBlockState(pos).isOpaqueCube()) {
                return i;
            }
        }
        return max;
    }

    private void place(World world, BlockPos pos, int length, boolean up) {
        for (int i = 0; i < length; i++) {
            pos = up ? pos.up() : pos.down();
            world.setBlockState(pos, cfg.state, 16);
        }
    }

    private void placeAll(World world, Random rand, int length, boolean up, BlockPos[] positions) {
        for (BlockPos pos : positions) {
            if (!cfg.symmetrical) {
                final int min = length * 9 / 11;
                length = rand.nextInt(length - min + 1) + min;
            }
            this.findPlace(world, pos, length, up);
        }
    }

    private void findPlace(World world, BlockPos pos, int length, boolean up) {
        for (int i = 0; i < 3; i++) {
            if (world.getBlockState(pos).isOpaqueCube()) {
                this.place(world, pos, length, up);
                return;
            } // Go in the opposite direction and find a surface.
            pos = up ? pos.down() : pos.up();
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

    private static BlockPos[] middleSidePositions(BlockPos pos) {
        final int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        return new BlockPos[] {
            new BlockPos(x, y, z - 2), // North
            new BlockPos(x, y, z + 2), // South
            new BlockPos(x + 2, y, z), // East
            new BlockPos(x - 2, y, z)  // West
        };
    }

    private static BlockPos[] middleCornerPositions(BlockPos pos) {
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

    private static BlockPos[] outerSidePositions(BlockPos pos) {
        final int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        return new BlockPos[] {
            new BlockPos(x, y, z - 3), // North
            new BlockPos(x, y, z + 3), // South
            new BlockPos(x + 3, y, z), // East
            new BlockPos(x - 3, y, z)  // West
        };
    }

    private static BlockPos[] outerBetweenPositions(BlockPos pos) {
        final int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        return new BlockPos[] {
            new BlockPos(x + 2, y, z - 2), // Northeast
            new BlockPos(x + 2, y, z + 2), // Southeast
            new BlockPos(x - 2, y, z + 2), // Southwest
            new BlockPos(x - 2, y, z - 2)  // Northwest
        };
    }

    private static BlockPos[] outerCornerPositions(BlockPos pos) {
        final int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        return new BlockPos[] {
            new BlockPos(x + 1, y, z + 3),
            new BlockPos(x + 3, y, z + 1),
            new BlockPos(x + 3, y, z - 1),
            new BlockPos(x + 1, y, z - 3),
            new BlockPos(x - 1, y, z - 3),
            new BlockPos(x - 3, y, z - 1),
            new BlockPos(x - 3, y, z + 1),
            new BlockPos(x - 1, y, z + 3)
        };
    }
}