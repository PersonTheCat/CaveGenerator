package personthecat.cavegenerator.world.generator;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import personthecat.cavegenerator.config.Cfg;
import personthecat.cavegenerator.model.ConfiguredCaveBlock;
import personthecat.cavegenerator.model.PositionFlags;
import personthecat.cavegenerator.model.SphereData;
import personthecat.cavegenerator.presets.data.ConditionSettings;
import personthecat.cavegenerator.presets.data.DecoratorSettings;
import personthecat.cavegenerator.world.BiomeSearch;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static personthecat.cavegenerator.util.CommonBlocks.BLK_WATER;

public abstract class MapGenerator extends CaveCarver {

    /** The vertical distance to the nearest water source block that can be ignored. */
    private static final int WATER_WIGGLE_ROOM = 7;

    protected final List<BlockPos> invalidChunks = new ArrayList<>();
    private final SphereData sphere = new SphereData();
    protected final Random rand;
    private final boolean checkWater;

    public MapGenerator(ConditionSettings conditions, DecoratorSettings decorators, Random rand, long seed, boolean checkWater) {
        super(conditions, decorators, rand, seed);
        this.rand = new Random(seed);
        this.checkWater = checkWater;
    }

    @Override
    public final void generate(final PrimerContext ctx) {
        // Todo: dimensions
        if (this.conditions.hasBiomes) {
            if (ctx.search.anyMatches(this.conditions.biomes)) {
                this.fillInvalidChunks(ctx.search);
                this.generateChecked(ctx);
                this.invalidChunks.clear();
            }
        } else if (this.conditions.hasRegion) {
            if (this.conditions.region.getBoolean(ctx.actualX, ctx.actualZ)) {
                this.fillInvalidChunks(ctx.chunkX, ctx.chunkZ);
                this.generateChecked(ctx);
                this.invalidChunks.clear();
            }
        } else {
            this.generateChecked(ctx);
        }
    }

    /**
     * Checks the biome and noise conditions in all of the surrounding chunks for this generator.
     *
     * <p>Any chunks that do not pass will be added into the {@link #invalidChunks} context and
     * used to form a distance-based, cylindrical chunk border in future.
     *
     * @param search The lazily initialized biome search utility providing biome data.
     */
    private void fillInvalidChunks(final BiomeSearch search) {
        for (final BiomeSearch.Data d : search.surrounding.get()) {
            if (!(this.conditions.biomes.test(d.biome) && this.conditions.region.getBoolean(d.centerX, d.centerZ))) {
                this.invalidChunks.add(new BlockPos(d.centerX, 0, d.centerZ));
            }
        }
    }

    /**
     * Variant of {@link #fillInvalidChunks(BiomeSearch)} which only checks the region noise
     * parameters in the surrounding chunks.
     *
     * <p>Because we do not have position data from the otherwise provided {@link BiomeSearch}
     * utility, the positions must be reconstructed here. In the future, it would be cleaner
     * to reduce this code duplication.
     *
     * @param chunkX The chunk x-coordinate (abs / 16)
     * @param chunkZ The chunk z-coordinate (abs / 16)
     */
    private void fillInvalidChunks(final int chunkX, final int chunkZ) {
        final int range = Cfg.BIOME_RANGE.getAsInt();
        for (int cX = chunkX - range; cX <= chunkX + range; cX++) {
            for (int cZ = chunkZ - range; cZ < chunkZ + range; cZ++) {
                final int centerX = cX * 16 + 8;
                final int centerZ = cZ * 16 + 8;
                if (!this.conditions.region.getBoolean(centerX, centerZ)) {
                    this.invalidChunks.add(new BlockPos(centerX, 0, centerZ));
                }
            }
        }
    }

    @Override
    protected final void generateChecked(final PrimerContext ctx) {
        final int range = Cfg.MAP_RANGE.getAsInt();
        this.rand.setSeed(this.seed);
        final long xMask = this.rand.nextLong();
        final long zMask = this.rand.nextLong();

        for (int destX = ctx.chunkX - range; destX <= ctx.chunkX + range; destX++) {
            for (int destZ = ctx.chunkZ - range; destZ <= ctx.chunkZ + range; destZ++) {
                long xHash = (long) destX * xMask;
                long zHash = (long) destZ * zMask;
                this.rand.setSeed(xHash ^ zHash ^ this.seed);
                this.mapGenerate(ctx, destX, destZ);
            }
        }
    }

    /**
     * Runs the generator in either the current chunk <b>or a foreign chunk</b> to determine
     * if its features will touch this chunk. Unlike its counterpart used by most other
     * generators, {@link #generateChecked}
     *
     * @param ctx   The current early generation context.
     * @param destX The x-coordinate of the chunk being evaluated (but not generated)
     * @param destZ The z-coordinate of the chunk being evaluated (but not generated)
     */
    protected abstract void mapGenerate(final PrimerContext ctx, final int destX, final int destZ);

    /**
     * Checks all of the invalid chunk block positions inside of {@link #invalidChunks} to
     * find whichever is closest.
     *
     * @param x The absolute x-coordinate.
     * @param z The absolute z-coordinate.
     * @return The raw distance to the nearest invalid biome or region noise.
     */
    protected double getNearestBorder(final int x, final int z) {
        double shortestDistance = Double.MAX_VALUE;

        for (final BlockPos invalid : this.invalidChunks) {
            final double sum = Math.pow(x - invalid.getX(), 2) + Math.pow(z - invalid.getZ(), 2);
            final double distance = Math.sqrt(sum);

            shortestDistance = Math.min(distance, shortestDistance);
        }
        return shortestDistance;
    }

    /**
     * Generates all of the applicable features for a single sphere in the current chunk.
     *
     * @param ctx  The current early generation context.
     * @param rand A RNG used for <b>decoration purposes only</b>.
     * @param x    The absolute center x-coordinate of the sphere.
     * @param y    The absolute center y-coordinate of the sphere.
     * @param z    The absolute center z-coordinate of the sphere.
     * @param rXZ  The horizontal radius of the sphere, in blocks.
     * @param rY   The vertical radius of the sphere, in blocks.
     * @param roXZ The horizontal radius of the outer shell, in blocks.
     * @param roY  The vertical radius of the outer shell, in blocks.
     */
    protected final void generateSphere(PrimerContext ctx, Random rand, double x, double y, double z,
                                        double rXZ, double rY, double roXZ, double roY) {
        final int miX = limitXZ(Mth.floor(x - roXZ) - ctx.actualX - 1);
        final int maX = limitXZ(Mth.floor(x + roXZ) - ctx.actualX + 1);
        final int miY = limitY(Mth.floor(y - roY) - 1);
        final int maY = limitY(Mth.floor(y + roY) + 1);
        final int miZ = limitXZ(Mth.floor(z - roXZ) - ctx.actualZ - 1);
        final int maZ = limitXZ(Mth.floor(z + roXZ) - ctx.actualZ + 1);

        this.sphere.reset();
        this.sphere.grow(maX - miX, maY - miY, maZ - miZ);

        if (roXZ - rXZ != 0 && rand.nextInt(decorators.shell.cfg.sphereResolution) == 0) {
            this.fillDouble(ctx, this.sphere, x, y, z, rXZ, rY, roXZ, roY, miX, maX, miY, maY, miZ, maZ);
        } else {
            this.fillSphere(ctx, this.sphere, x, y, z, rXZ, rY, miX, maX, miY, maY, miZ, maZ);
        }

        // If we need to test this section for water -> is there water?
        if (!(this.shouldTestForWater(miY, maY) && this.testForWater(ctx, this.sphere.inner))) {
            this.generateShell(ctx, rand, this.sphere.shell, (int) y);
            this.replaceSphere(ctx, rand, this.sphere.inner);
            this.decorateAll(ctx, this.sphere.inner, rand);
        }
    }

    /**
     * Makes sure the input value stays within horizontal chunk bounds.
     *
     * @param xz The minimum <em>or maximum</em> horizontal relative coordinate.
     */
    private static int limitXZ(final int xz) {
        return xz < 0 ? 0 : Math.min(xz, 16);
    }

    /**
     * Makes sure the input value stays within vertical chunk bounds.
     *
     * @param y The minimum <em>or maximum</em> vertical coordinate.
     */
    private static int limitY(final int y) {
        return y < 1 ? 1 : Math.min(y, 256);
    }

    /**
     * Provides reusable instructions on where to place and decorate blocks in a sphere.
     *
     * <p>While the number of parameters <em>is</em> fairly high, this design choice is
     * intentional and serves to reduce the number of unnecessary allocations during
     * world generation.
     *
     * @param ctx    The current early generation context.
     * @param sphere The set of relative block coordinates being generated.
     * @param cX     The absolute center x-coordinate of this sphere.
     * @param cY     The absolute center y-coordinate of this sphere.
     * @param cZ     The absolute center z-coordinate of this sphere.
     * @param rXZ    The horizontal radius of this sphere, in blocks.
     * @param rY     The vertical radius of this sphere, in blocks.
     * @param miX    The minimum relative x-coordinate of this sphere.
     * @param maX    The maximum relative x-coordinate of this sphere.
     * @param miY    The minimum relative y-coordinate of this sphere.
     * @param maY    The maximum relative y-coordinate of this sphere.
     * @param miZ    The minimum relative z-coordinate of this sphere.
     * @param maZ    The maximum relative z-coordinate of this sphere.
     */
    protected abstract void fillSphere(PrimerContext ctx, SphereData sphere, double cX, double cY, double cZ,
            double rXZ, double rY, int miX, int maX, int miY, int maY, int miZ, int maZ);

    /**
     * Variant of {@link #fillSphere} which includes an outer shell. This is more expensive.
     *
     * @param ctx    The current early generation context.
     * @param sphere The set of relative block coordinates being generated.
     * @param cX     The absolute center x-coordinate of this sphere.
     * @param cY     The absolute center y-coordinate of this sphere.
     * @param cZ     The absolute center z-coordinate of this sphere.
     * @param rXZ    The horizontal radius of this sphere, in blocks.
     * @param rY     The vertical radius of this sphere, in blocks.
     * @param roXZ   The horizontal radius of the outer shell, in blocks.
     * @param roY    The vertical radius of the outer shell, in blocks.
     * @param miX    The minimum relative x-coordinate of this sphere.
     * @param maX    The maximum relative x-coordinate of this sphere.
     * @param miY    The minimum relative y-coordinate of this sphere.
     * @param maY    The maximum relative y-coordinate of this sphere.
     * @param miZ    The minimum relative z-coordinate of this sphere.
     * @param maZ    The maximum relative z-coordinate of this sphere.
     */
    protected abstract void fillDouble(PrimerContext ctx, SphereData sphere, double cX, double cY, double cZ,
            double rXZ, double rY, double roXZ, double roY, int miX, int maX, int miY, int maY, int miZ, int maZ);

    /**
     * Calculates the maximum distance for this tunnel, if needed.
     *
     * @param rand  The primary RNG used for regular generator calculations.
     * @param input The config value setup for this generator.
     * @return The actual distance (in blocks) for a <b>single</b> feature.
     */
    protected int getDistance(final Random rand, final int input) {
        if (input <= 0) {
            return 112 - rand.nextInt(28);
        }
        return input;
    }

    /**
     * Returns whether a test should be run to determine whether water is found and stop
     * generating.
     *
     * @param miY The lowest y-coordinate of a single sphere.
     * @param maY The highest y-coordinate of a single sphere.
     * @return <code>true</code> If the sphere must be tested.
     */
    private boolean shouldTestForWater(final int miY, final int maY) {
        if (!this.checkWater) {
            return false;
        }
        for (final ConfiguredCaveBlock block : decorators.caveBlocks) {
            if (block.cfg.states.contains(BLK_WATER)) {
                if (maY <= block.cfg.height.max + WATER_WIGGLE_ROOM
                    && miY >= block.cfg.height.min - WATER_WIGGLE_ROOM) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Checks all of the blocks in an entire sphere to see if water exists at this
     * point.
     *
     * Todo: it may be possible to discover this information using heightmaps.
     *
     * @param ctx    The current early generation context.
     * @param sphere A set of all of the relative coordinates being generated.
     * @return <code>true</code>, if any block is regular water.
     */
    protected boolean testForWater(final PrimerContext ctx, final PositionFlags sphere) {
        return sphere.anyMatches((x, y, z) -> BLK_WATER.equals(ctx.get(x, y, z)));
    }

    /**
     * Replaces all of the blocks around the current sphere, if applicable.
     *
     * @param ctx    The current early generation context.
     * @param rand   A RNG used for <b>decoration purposes only</b>.
     * @param sphere A set of all of the relative coordinates being generated.
     * @param cY     The center height which this shell is to generate around.
     */
    protected void generateShell(PrimerContext ctx, Random rand, PositionFlags sphere, int cY) {
        sphere.forEach((x, y, z) -> this.generateShell(ctx, rand, x, y, z, cY));
    }

    /**
     * Replaces all of the blocks in the current sphere, if applicable.
     *
     * @param ctx    The current early generation context.
     * @param rand   A RNG used for <b>decoration purposes only</b>.
     * @param sphere A set of all of the relative coordinates being generated.
     */
    protected void replaceSphere(PrimerContext ctx, Random rand, PositionFlags sphere) {
        sphere.forEach((x, y, z) -> this.replaceBlock(ctx, rand, x, y, z));
    }

    /**
     * Decorates all of the blocks inside of this sphere, if applicable.
     *
     * @param ctx    The current early generation context.
     * @param rand   A RNG used for <b>decoration purposes only</b>.
     * @param sphere A set of all of the relative coordinates being generated.
     */
    protected void decorateSphere(PrimerContext ctx, Random rand, PositionFlags sphere) {
        sphere.forEach((x, y, z) -> this.decorateBlock(ctx, rand, x, y, z));
    }
}
