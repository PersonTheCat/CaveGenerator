package personthecat.cavegenerator.world.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import personthecat.catlib.data.Range;
import personthecat.cavegenerator.presets.data.StalactiteSettings;
import personthecat.cavegenerator.util.XoRoShiRo;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Random;

@ParametersAreNonnullByDefault
public class StalactiteGenerator extends BasicFeature {

    private final StalactiteSettings cfg;
    private final int resolution;
    private final boolean speleothem;
    private final boolean stalactite;

    public StalactiteGenerator(final StalactiteSettings cfg, final Random rand, final long seed) {
        super(cfg.conditions, rand, seed);
        this.cfg = cfg;
        this.resolution = calculateResolution(cfg.chance);
        this.speleothem = cfg.type.equals(StalactiteSettings.Type.SPELEOTHEM);
        this.stalactite = this.speleothem || cfg.type.equals(StalactiteSettings.Type.STALACTITE);
    }

    /**
     * Determines the number of blocks between noise calculations.
     * Higher frequency -> higher resolution -> lower distance.
     */
    private static int calculateResolution(final double chance) {
        if (chance < 0.25) return 16; // 00 to 25 -> 1x / chunk
        if (chance < 0.35) return 8;  // 25 to 35 -> 4x / chunk
        if (chance < 0.55) return 4;  // 35 to 55 -> 16x / chunk
        if (chance < 0.75) return 2;  // 55 to 75 -> 64x / chunk
        return 1;                     // 75 to 100 -> 256x / chunk
    }

    @Override
    protected void doGenerate(final WorldContext ctx) {
        final Random localRand = new XoRoShiRo(ctx.rand.nextInt());
        // Each iteration increments by `distance`. This changes the frequency
        // with which `noise` is calculated, theoretically impacting performance.
        // Lower frequencies do not require as high a resolution, as this
        // difference would typically not be visible.
        for (int x = ctx.centerX; x < ctx.centerX + 16; x = x + resolution) {
            for (int z = ctx.centerZ; z < ctx.centerZ + 16; z = z + resolution) {
                final Biome biome = ctx.region.getBiome(new BlockPos(x, 0, z));
                if (conditions.biomes.test(biome) && conditions.region.getBoolean(x, z)) {
                    this.generateRegion(ctx, localRand, x, z);
                }
            }
        }
    }

    /** Attempts to spawn a stalactite at every coordinate pair in this region. */
    private void generateRegion(final WorldContext ctx, final Random rand, final int x, final int z) {
        for (int dx = x; dx < x + resolution; dx++) {
            for (int dz = z; dz < z + resolution; dz++) {
                // Check this earlier -> do less when it fails.
                if (rand.nextDouble() >= cfg.chance) {
                    continue;
                }
                final Range height = conditions.getColumn(dx, dz);
                if (height.diff() != 0 && conditions.region.getBoolean(dz, dz)) {
                    final int surface = ctx.region.getHeight(Heightmap.Types.OCEAN_FLOOR, dx, dz);
                    final int maxY = Math.min(surface - SURFACE_ROOM, height.max);
                    if (maxY <= height.min) continue;

                    final int y = this.stalactite ? this.findCeiling(ctx.region, dx, height.min, dz, maxY)
                        : this.findFloor(ctx.region, dx, maxY, dz, height.min);

                    if (y != NONE_FOUND && conditions.noise.getBoolean(dx, y, dz)) {
                        final BlockPos pos = new BlockPos(dx, y, dz);
                        if (checkSources(cfg.matchers, ctx.region, pos)) {
                            this.checkSpace(ctx, pos);
                        }
                    }
                }
            }
        }
    }

    private void checkSpace(final WorldContext ctx, final BlockPos pos) {
        final int length = this.cfg.length.rand(ctx.rand);
        final int needed = this.cfg.space + length;
        final int space = this.getSpace(ctx, pos, needed);
        if (this.speleothem && space > 2 * needed) {
            return;
        }
        if (space >= needed) {
            this.generateSingle(ctx, pos, length, !this.stalactite);
            if (this.speleothem) {
                this.generateSingle(ctx, pos.below(space), length, true);
            }
        }
    }

    private void generateSingle(final WorldContext ctx, final BlockPos pos, final int length, final boolean up) {
        this.place(ctx, pos, length, up);
        if (length > 2 && cfg.size != StalactiteSettings.Size.SMALL) {
            this.placeAll(ctx, length * 2 / 3, up, sidePositions(pos));
            this.placeAll(ctx, length / 4, up, cornerPositions(pos));
            if (length > 5 && cfg.size.ordinal() > StalactiteSettings.Size.MEDIUM.ordinal()) {
                this.placeAll(ctx, length / 4, up, middleSidePositions(pos));
                this.placeAll(ctx, length / 6, up, middleCornerPositions(pos));
                if (length > 9 && cfg.size.ordinal() > StalactiteSettings.Size.LARGE.ordinal()) {
                    this.placeAll(ctx, length / 8, up, outerSidePositions(pos));
                    this.placeAll(ctx, length / 11, up, outerCornerPositions(pos));
                    this.placeAll(ctx, length / 8, up, outerBetweenPositions(pos));
                }
            }
        }
    }

    private int getSpace(final WorldContext ctx, BlockPos pos, final int max) {
        for (int i = 0; i < max; i++) {
            // We will be at the top and need to look down.
            pos = this.stalactite ? pos.below() : pos.above();
            if (ctx.region.getBlockState(pos).getMaterial().isSolidBlocking()) {
                return i;
            }
        }
        return max;
    }

    private void place(final WorldContext ctx, BlockPos pos, final int length, final boolean up) {
        for (int i = 0; i < length; i++) {
            pos = up ? pos.above() : pos.below();
            ctx.region.setBlock(pos, cfg.state, 16);
        }
    }

    private void placeAll(final WorldContext ctx, int length, final boolean up, final BlockPos[] positions) {
        for (final BlockPos pos : positions) {
            if (!cfg.symmetrical) {
                final int min = length * 9 / 11;
                length = ctx.rand.nextInt(length - min + 1) + min;
            }
            this.findPlace(ctx, pos, length, up);
        }
    }

    private void findPlace(final WorldContext ctx, BlockPos pos, final int length, final boolean up) {
        for (int i = 0; i < 3; i++) {
            if (ctx.region.getBlockState(pos).getMaterial().isSolidBlocking()) {
                this.place(ctx, pos, length, up);
                return;
            } // Go in the opposite direction and find a surface.
            pos = up ? pos.below() : pos.above();
        }
    }

    private static BlockPos[] sidePositions(final BlockPos pos) {
        final int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        return new BlockPos[] {
            new BlockPos(x, y, z - 1), // North
            new BlockPos(x, y, z + 1), // South
            new BlockPos(x + 1, y, z), // East
            new BlockPos(x - 1, y, z)  // West
        };
    }

    private static BlockPos[] cornerPositions(final BlockPos pos) {
        final int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        return new BlockPos[] {
            new BlockPos(x + 1, y, z - 1), // Northeast
            new BlockPos(x + 1, y, z + 1), // Southeast
            new BlockPos(x - 1, y, z + 1), // Southwest
            new BlockPos(x - 1, y, z - 1)  // Northwest
        };
    }

    private static BlockPos[] middleSidePositions(final BlockPos pos) {
        final int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        return new BlockPos[] {
            new BlockPos(x, y, z - 2), // North
            new BlockPos(x, y, z + 2), // South
            new BlockPos(x + 2, y, z), // East
            new BlockPos(x - 2, y, z)  // West
        };
    }

    private static BlockPos[] middleCornerPositions(final BlockPos pos) {
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

    private static BlockPos[] outerSidePositions(final BlockPos pos) {
        final int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        return new BlockPos[] {
            new BlockPos(x, y, z - 3), // North
            new BlockPos(x, y, z + 3), // South
            new BlockPos(x + 3, y, z), // East
            new BlockPos(x - 3, y, z)  // West
        };
    }

    private static BlockPos[] outerBetweenPositions(final BlockPos pos) {
        final int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        return new BlockPos[] {
            new BlockPos(x + 2, y, z - 2), // Northeast
            new BlockPos(x + 2, y, z + 2), // Southeast
            new BlockPos(x - 2, y, z + 2), // Southwest
            new BlockPos(x - 2, y, z - 2)  // Northwest
        };
    }

    private static BlockPos[] outerCornerPositions(final BlockPos pos) {
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