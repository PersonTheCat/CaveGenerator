package com.personthecat.cavegenerator.world.generator;

import com.personthecat.cavegenerator.config.ConfigFile;
import com.personthecat.cavegenerator.data.CavernSettings;
import com.personthecat.cavegenerator.data.NoiseMapSettings;
import com.personthecat.cavegenerator.data.NoiseSettings;
import com.personthecat.cavegenerator.model.Range;
import com.personthecat.cavegenerator.noise.DummyGenerator;
import com.personthecat.cavegenerator.util.PositionFlags;
import com.personthecat.cavegenerator.world.BiomeSearch;
import fastnoise.FastNoise;
import lombok.AllArgsConstructor;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkPrimer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class CavernGenerator extends WorldCarver {

    private final List<ChunkTestData> invalidChunks;
    private final double[] wallNoise = new double[256];
    private final List<FastNoise> generators;
    private final FastNoise wallOffset;
    private final FastNoise heightOffset;
    private final PositionFlags caverns;
    private final double wallCurveRatio;
    private final boolean wallInterpolated;
    private final boolean hasShell;
    private final int maxY;
    private final int diffY;
    private final int curveOffset;

    public CavernGenerator(CavernSettings cfg, World world) {
        super(cfg.conditions, cfg.decorators, world);
        this.generators = createGenerators(cfg.generators, world);
        this.wallOffset = cfg.wallOffset.map(n -> n.getGenerator(world)).orElse(new DummyGenerator(0F));
        this.heightOffset = cfg.offset.map(n -> n.getGenerator(world)).orElse(new DummyGenerator(0F));
        this.wallCurveRatio = cfg.wallCurveRatio;
        this.wallInterpolated = cfg.wallInterpolation;
        this.hasShell = !this.decorators.shell.decorators.isEmpty();

        final int minY = conditions.height.min + cfg.conditions.floor.map(n -> n.range.min).orElse(0);
        this.maxY = conditions.height.max + cfg.conditions.ceiling.map(n -> n.range.max).orElse(0);
        this.diffY = this.maxY - minY;
        this.caverns = new PositionFlags(16 * 16 * this.maxY - minY);
        this.curveOffset = (this.diffY + 1) / -2;

        final int r = BiomeSearch.size();
        this.invalidChunks = new ArrayList<>(this.wallInterpolated ? (r * 2 + 1) * 2 - 1 : r);
        cfg.walls.ifPresent(n -> this.setupWallNoise(n, world));
    }

    private static List<FastNoise> createGenerators(List<NoiseSettings> settings, World world) {
        return settings.stream().map(s -> s.getGenerator(world)).collect(Collectors.toList());
    }

    private void setupWallNoise(NoiseMapSettings settings, World world) {
        final FastNoise noise = settings.getGenerator(world);
        final int len = wallNoise.length;
        final double increment = 6.2830810546 / (double) len;
        final int r = 32; //  Arbitrary radius and resolution

        // Wrap the noise around a circle so it can be translated seamlessly.
        for (int i = 0; i < len; i++) {
            final double angle = (i + 1) * increment;
            final int x = (int) (r * Math.cos(angle));
            final int y = (int) (r * Math.sin(angle));
            this.wallNoise[i] = noise.GetAdjustedNoise(x, y);
        }
    }

    @Override
    public void generate(PrimerContext ctx) {
        if (conditions.dimensions.test(ctx.world.provider.getDimension())) {
            if (conditions.hasBiomes) {
                if (ctx.biomes.anyMatches(conditions.biomes)) {
                    this.fillInvalidChunks(ctx.biomes, ctx.chunkX, ctx.chunkZ);
                    this.generateChecked(ctx);
                    this.invalidChunks.clear();
                }
            } else if (conditions.hasRegion) {
                if (conditions.region.GetBoolean(ctx.offsetX, ctx.offsetZ)) {
                    this.fillInvalidChunks(ctx.chunkX, ctx.chunkZ);
                    this.generateChecked(ctx);
                    this.invalidChunks.clear();
                }
            } else {
                this.generateChecked(ctx);
            }
            this.caverns.reset();
        }
    }

    private void fillInvalidChunks(BiomeSearch biomes, int x, int z) {
        if (this.wallInterpolated) {
            this.fillInterpolated(biomes, x, z);
        } else {
            this.fillBorder(biomes);
        }
    }

    private void fillBorder(BiomeSearch biomes) {
        for (BiomeSearch.Data d : biomes.surrounding.get()) {
            if (!(conditions.biomes.test(d.biome) && conditions.region.GetBoolean(d.centerX, d.centerZ))) {
                // Translate the noise randomly for each chunk to minimize repetition.
                final int translateY = (int) wallOffset.GetAdjustedNoise(d.centerX, d.centerZ);
                this.invalidChunks.add(new ChunkTestData(d.centerX, d.centerZ, translateY));
            }
        }
    }

    private void fillInterpolated(BiomeSearch biomes, int x, int z) {
        final int r = ConfigFile.biomeRange;
        final boolean[][] points = getBorderMatrix(biomes, r, x, z);
        interpolate(points);
        interpolate(points);
        createBorder(this.invalidChunks, points, wallOffset, r, x, z);
    }

    private boolean[][] getBorderMatrix(BiomeSearch biomes, int r, int x, int z) {
        final int size = r * 2 + 1; // include center
        final int interpolated = size * 2 - 1; // cut edges
        final boolean[][] points = new boolean[interpolated][interpolated];

        for (BiomeSearch.Data d : biomes.surrounding.get()) {
            if (!(conditions.biomes.test(d.biome) && conditions.region.GetBoolean(d.centerX, d.centerZ))) {
                final int relX = d.chunkX - x;
                final int relZ = d.chunkZ - z;
                points[(relX + r) * 2][(relZ + r) * 2] = true;
            }
        }
        return points;
    }

    private static void interpolate(boolean[][] f) {
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
                final int translateY = (int) noise.GetAdjustedNoise(aX, aZ);
                border.add(new ChunkTestData(aX, aZ, translateY));
            }
        }
    }

    private void fillInvalidChunks(int chunkX, int chunkZ) {
        final int range = ConfigFile.biomeRange;
        for (int cX = chunkX - range; cX <= chunkX + range; cX++) {
            for (int cZ = chunkZ - range; cZ < chunkZ + range; cZ++) {
                final int centerX = cX * 16 + 8;
                final int centerZ = cZ * 16 + 8;
                if (!conditions.region.GetBoolean(centerX, centerZ)) {
                    final int translateY = (int) wallOffset.GetAdjustedNoise(centerX, centerZ);
                    this.invalidChunks.add(new ChunkTestData(centerX, centerZ, translateY));
                }
            }
        }
    }

    @Override
    protected void generateChecked(PrimerContext ctx) {
        this.generateCaverns(ctx.heightmap, ctx.rand, ctx.primer, ctx.chunkX, ctx.chunkZ);
    }

    /** Generates giant air pockets in this chunk using a series of 3D noise generators. */
    private void generateCaverns(int[][] heightmap, Random rand, ChunkPrimer primer, int chunkX, int chunkZ) {
        for (int x = 0; x < 16; x++) {
            final int actualX = x + (chunkX * 16);
            for (int z = 0; z < 16; z++) {
                final int actualZ = z + (chunkZ * 16);
                this.generateColumn(heightmap, rand, primer, x, z, chunkX, chunkZ, actualX, actualZ);
            }
        }
        // Caverns must be completely generated before decorating.
        if (this.hasLocalDecorators()) {
            this.decorateCaverns(rand, primer, chunkX, chunkZ);
        }
    }

    private void generateColumn(int[][] heightmap, Random rand, ChunkPrimer primer, int x, int z, int chunkX, int chunkZ, int actualX, int actualZ) {
        final BorderData border = this.getNearestBorder(actualX, actualZ);
        final double distance = border.distance;
        final int offset = border.offset;
        final Range height = this.conditions.getColumn(heightmap, actualX, actualZ);
        // Adjust the height to accommodate the shell.
        final int d = (int) decorators.shell.sphereRadius;
        final int min = Math.max(1, height.min - d);
        final int max = Math.min(255, height.max + d);

        for (int y = min; y < max; y++) {
            final int yO = y + (int) this.heightOffset.GetAdjustedNoise(actualX, actualZ);
            if (this.conditions.noise.GetBoolean(actualX, yO, actualZ)) {
                final double relY = this.curveOffset + this.maxY - y;
                final double curve = distance - ((relY * relY) / this.diffY * this.wallCurveRatio);

                final double wall = this.wallNoise[(y + offset) & 255];
                if (curve > wall) {
                    this.place(rand, primer, height, x, y, z, chunkX, chunkZ, actualX, actualZ);
                } else if (curve > wall - d) {
                    this.generateShell(rand, primer, x, y, z, y, chunkX, chunkZ);
                }
            }
        }
    }

    private void place(Random rand, ChunkPrimer primer, Range height, int x, int y, int z, int chunkX, int chunkZ, int actualX, int actualZ) {
        for (FastNoise noise : this.generators) {
            final float value = noise.GetNoise(actualX, y, actualZ);
            if (noise.IsInThreshold(value)) {
                if (height.contains(y)) {
                    this.replaceBlock(rand, primer, x, y, z, chunkX, chunkZ);
                    this.caverns.add(x, y, z);
                } else {
                    this.generateShell(rand, primer, x, y, z, y, chunkX, chunkZ);
                }
                return;
            } else if (noise.IsOuter(value, decorators.shell.noiseThreshold)) {
                this.generateShell(rand, primer, x, y, z, y, chunkX, chunkZ);
            }
        }
    }

    private BorderData getNearestBorder(int x, int z) {
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

    private void decorateCaverns(Random rand, ChunkPrimer primer, int chunkX, int chunkZ) {
        this.caverns.forEach((x, y, z) -> this.decorateBlock(rand, primer, x, y, z, chunkX, chunkZ));
    }

    @Override
    protected void generateShell(Random rand, ChunkPrimer primer, int x, int y, int z, int cY, int chunkX, int chunkZ) {
        if (this.hasShell) {
            super.generateShell(rand, primer, x, y, z, cY, chunkX, chunkZ);
        }
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
