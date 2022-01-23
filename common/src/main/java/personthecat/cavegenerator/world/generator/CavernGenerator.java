package personthecat.cavegenerator.world.generator;

import lombok.AllArgsConstructor;
import personthecat.catlib.data.Range;
import personthecat.cavegenerator.config.Cfg;
import personthecat.cavegenerator.model.PositionFlags;
import personthecat.cavegenerator.world.BiomeSearch;
import personthecat.cavegenerator.world.config.CavernConfig;
import personthecat.fastnoise.FastNoise;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class CavernGenerator extends CaveCarver implements TunnelSocket {

    private final List<ChunkTestData> invalidChunks;
    private final double[] wallNoise = new double[256];
    private final PositionFlags caverns;
    private final int maxY;
    private final int diffY;
    private final int curveOffset;
    protected final CavernConfig cfg;

    public CavernGenerator(final CavernConfig cfg, final Random rand, final long seed) {
        super(cfg.conditions, cfg.decorators, rand, seed);
        this.maxY = cfg.bounds.max;
        this.diffY = cfg.bounds.diff();
        this.caverns = new PositionFlags(16 * 16 * this.diffY);
        this.curveOffset = (this.diffY + 1) / -2;

        final int r = BiomeSearch.size();
        this.invalidChunks = new ArrayList<>(cfg.wallInterpolation ? (r * 2 + 1) * 2 - 1 : r);
        if (cfg.walls != null) {
            this.setupWallNoise(cfg.walls);
        } else {
            Arrays.fill(wallNoise, 15);
        }
        this.cfg = cfg;
    }

    private void setupWallNoise(FastNoise noise) {
        final int len = wallNoise.length;
        final double increment = 6.2830810546 / (double) len;
        final int r = 32; // Arbitrary radius and resolution

        // Wrap the noise around a circle, so it can be translated seamlessly.
        for (int i = 0; i < len; i++) {
            final double angle = (i + 1) * increment;
            final int x = (int) (r * Math.cos(angle));
            final int y = (int) (r * Math.sin(angle));
            this.wallNoise[i] = noise.getNoiseScaled(x, y);
        }
    }

    @Override
    protected void fillInvalidChunks(final BiomeSearch search, final int x, final int z) {
        if (this.cfg.wallInterpolation) {
            this.fillInterpolated(search, x, z);
        } else {
            this.fillBorder(search);
        }
    }

    private void fillBorder(final BiomeSearch search) {
        for (final BiomeSearch.Data d : search.surrounding.get()) {
            if (!(this.conditions.biomes.test(d.biome) && this.conditions.region.getBoolean(d.centerX, d.centerZ))) {
                // Translate the noise randomly for each chunk to minimize repetition.
                final int translateY = (int) this.cfg.wallOffset.getNoiseScaled(d.centerX, d.centerZ);
                this.invalidChunks.add(new ChunkTestData(d.centerX, d.centerZ, translateY));
            }
        }
    }

    // Todo: The biome search is probably the better place to handle interpolation
    private void fillInterpolated(final BiomeSearch biomes, final int x, final int z) {
        final int r = Cfg.biomeRange();
        final boolean[][] points = getBorderMatrix(biomes, r, x, z);
        interpolate(points);
        interpolate(points);
        createBorder(this.invalidChunks, points, this.cfg.wallOffset, r, x, z);
    }

    private boolean[][] getBorderMatrix(final BiomeSearch biomes, final int r, final int x, final int z) {
        final int size = r * 2 + 1; // include center
        final int interpolated = size * 2 - 1; // cut edges
        final boolean[][] points = new boolean[interpolated][interpolated];

        for (final BiomeSearch.Data d : biomes.surrounding.get()) {
            if (!(this.conditions.biomes.test(d.biome) && this.conditions.region.getBoolean(d.centerX, d.centerZ))) {
                final int relX = d.chunkX - x;
                final int relZ = d.chunkZ - z;
                points[(relX + r) * 2][(relZ + r) * 2] = true;
            }
        }
        return points;
    }

    private static void interpolate(final boolean[][] f) {
        final int len = f.length;
        for (int i = 0; i < len; i++) {
            for (int j = 0; j < len; j++) {
                if (f[i][j]) continue;
                // Determine if this is a corner point.
                if (i % 2 == 1 && j % 2 == 1) {
                    final boolean nw = f[i - 1][j - 1];
                    final boolean se = f[i + 1][j + 1];
                    final boolean ne = f[i - 1][j + 1];
                    final boolean sw = f[i + 1][j - 1];
                    f[i][j] = (nw && se) || (ne && sw);
                } else {
                    final boolean n = i > 0 && f[i - 1][j];
                    final boolean s = i < len - 1 && f[i + 1][j];
                    final boolean e = j > 0 && f[i][j - 1];
                    final boolean w = j < len - 1 && f[i][j + 1];
                    f[i][j] = (n && s) || (e && w);
                }
            }
        }
    }

    private static void createBorder(List<ChunkTestData> border, boolean[][] f, FastNoise noise, int r, int x, int z) {
        final int len = f.length;
        for (int i = 0; i < len; i++) {
            for (int j = 0; j < len; j++) {
                if (!f[i][j]) continue;
                // Convert to absolute coordinates
                final double cX = (i / 2.0) - r + x;
                final double cZ = (j / 2.0) - r + z;
                final int aX = ((int) cX * 16 + 8) + (cX % 1 == 0 ? 8 : 0);
                final int aZ = ((int) cZ * 16 + 8) + (cZ % 1 == 0 ? 8 : 0);
                final int translateY = (int) noise.getNoiseScaled(aX, aZ);
                border.add(new ChunkTestData(aX, aZ, translateY));
            }
        }
    }

    @Override
    protected void generateChecked(final PrimerContext ctx) {
        for (int x = 0; x < 16; x++) {
            final int aX = x + (ctx.actualX);
            for (int z = 0; z < 16; z++) {
                final int aZ = z + (ctx.actualZ);
                this.generateColumn(ctx, x, z, aX, aZ);
            }
        }
        // Caverns must be completely generated before decorating.
        this.decorateAll(ctx, this.caverns, ctx.localRand);
        this.caverns.reset();
    }


    private void generateColumn(final PrimerContext ctx, final int x, final int z, final int aX, final int aZ) {
        final BorderData border = this.getNearestBorder(aX, aZ);
        final double distance = border.distance;
        final int offset = border.offset;
        final Range height = this.conditions.getColumn(ctx, aX, aZ);
        // Adjust the height to accommodate the shell.
        final int d = (int) this.decorators.shell.radius;
        final int min = Math.max(1, height.min - d);
        final int max = Math.min(255, height.max + d);
        final int yO = (int) this.cfg.offset.getNoiseScaled(aX, aZ);
        for (int y = min; y < max; y++) {
            if (this.conditions.noise.getBoolean(aX, y + yO, aZ)) {
                final double relY = this.curveOffset + this.maxY - y;
                final double curve = distance - ((relY * relY) / this.diffY * this.cfg.wallCurveRatio);

                final double wall = this.wallNoise[(y + offset) & 255];
                if (curve > wall) {
                    this.place(ctx, height, x, y, z, yO, aX, aZ);
                } else if (curve > wall - d) {
                    this.generateShell(ctx, ctx.localRand, x, y, z, y);
                }
            }
        }
    }

    private void place(PrimerContext ctx, Range height, int x, int y, int z, int yO, int aX, int aZ) {
        for (final FastNoise noise : this.cfg.generators) {
            final float value = noise.getNoise(aX, y + yO, aZ);
            if (noise.isInThreshold(value)) {
                if (height.contains(y)) {
                    if (this.replaceBlock(ctx, ctx.localRand, x, y, z)) {
                        this.caverns.add(x, y, z);
                    }
                } else {
                    this.generateShell(ctx, ctx.localRand, x, y, z, y);
                }
                return;
            } else if (noise.isInThreshold(value, this.decorators.shell.noiseThreshold)) {
                this.generateShell(ctx, ctx.localRand, x, y, z, y);
            }
        }
    }

    private BorderData getNearestBorder(final int x, final int z) {
        double shortestDistance = Double.MAX_VALUE;
        int offset = 0;

        for (final ChunkTestData invalid : this.invalidChunks) {
            final double sum = Math.pow(x - invalid.x, 2) + Math.pow(z - invalid.z, 2);
            final double distance = Math.sqrt(sum);

            if (distance < shortestDistance) {
                shortestDistance = distance;
                offset = invalid.offset;
            }
        }
        return new BorderData(shortestDistance, offset);
    }

    @Override
    public int getTunnelHeight(Random rand, int x, int z, int chunkX, int chunkZ) {
        // Currently, ignores offset and general noise
        final Range height = this.conditions.getColumn(x, z);
        if (height.isEmpty()) {
            return CANNOT_SPAWN;
        }
        final int center = height.rand(rand);
        final int yO = (int) this.cfg.offset.getNoiseScaled(x, z);
        if (rand.nextBoolean()) {
            for (int y = center; y < height.max; y += this.cfg.resolution) {
                if (this.checkSingle(x, y + yO, z)) {
                    return y;
                }
            }
        } else {
            for (int y = center; y > height.min; y -= this.cfg.resolution) {
                if (this.checkSingle(x, y + yO, z)) {
                    return y;
                }
            }
        }
        return CANNOT_SPAWN;
    }

    private boolean checkSingle(final int aX, final int y, final int aZ) {
        if (!this.conditions.noise.getBoolean(aX, y, aZ)) {
            return false;
        }
        for (final FastNoise generator : this.cfg.generators) {
            if (generator.getBoolean(aX, y, aZ)) {
                return true;
            }
        }
        return false;
    }

    @AllArgsConstructor
    private static class ChunkTestData {
        final int x;
        final int z;
        final int offset;
    }

    @AllArgsConstructor
    private static class BorderData {
        final double distance;
        final int offset;
    }
}
