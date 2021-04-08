package com.personthecat.cavegenerator.world.generator;

import com.personthecat.cavegenerator.data.CavernSettings;
import com.personthecat.cavegenerator.data.NoiseMapSettings;
import com.personthecat.cavegenerator.data.NoiseSettings;
import com.personthecat.cavegenerator.model.Range;
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

    private final List<BiomeTestData> invalidBiomes = new ArrayList<>(BiomeSearch.size());
    private final double[] wallNoise = new double[256];
    private final List<FastNoise> generators;
    private final FastNoise wallOffset;
    private final int maxY;

    public CavernGenerator(CavernSettings cfg, World world) {
        super(cfg.conditions, cfg.decorators, world);
        this.generators = createGenerators(cfg.generators, world);
        this.wallOffset = cfg.wallOffset.getGenerator(world);
        this.maxY = conditions.height.max + cfg.conditions.ceiling.map(n -> n.range.max).orElse(0);
        this.setupWallNoise(cfg.walls, world);
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
                    this.fillInvalidBiomes(ctx.biomes);
                    this.generateChecked(ctx);
                    this.invalidBiomes.clear();
                }
            } else {
                this.generateChecked(ctx);
            }
        }
    }

    private void fillInvalidBiomes(BiomeSearch biomes) {
        for (BiomeSearch.Data d : biomes.surrounding.get()) {
            if (!(conditions.biomes.test(d.biome) && conditions.region.GetBoolean(d.centerX, d.centerZ))) {
                // Translate the noise randomly for each chunk to minimize repetition.
                final int translateY = (int) wallOffset.GetAdjustedNoise(d.centerX, d.centerZ);
                this.invalidBiomes.add(new BiomeTestData(d.centerX, d.centerZ, translateY));
            }
        }
    }

    @Override
    protected void generateChecked(PrimerContext ctx) {
        generateCaverns(ctx.heightmap, ctx.rand, ctx.primer, ctx.chunkX, ctx.chunkZ);
    }

    /** Generates giant air pockets in this chunk using a series of 3D noise generators. */
    private void generateCaverns(int[][] heightmap, Random rand, ChunkPrimer primer, int chunkX, int chunkZ) {
        final boolean[][][] caverns = new boolean[maxY][16][16];
        for (int x = 0; x < 16; x++) {
            final int actualX = x + (chunkX * 16);
            for (int z = 0; z < 16; z++) {
                final int actualZ = z + (chunkZ * 16);
                final BorderData border = this.getNearestBorder(actualX, actualZ);
                final Range height = this.conditions.getColumn(heightmap, actualX, actualZ);
                final int diff = height.diff() + 1; // Must be positive.
                final int minOffset = diff / -2;
                // Adjust the height to accommodate the shell.
                final int d = (int) decorators.shell.sphereRadius;
                final int min = Math.max(1, height.min - d);
                final int max = Math.min(255, height.max + d);

                for (int y = max; y > min; y--) {
                    if (this.conditions.noise.GetBoolean(actualX, y, actualZ)) {
                        final int relative = minOffset + height.max - y;
                        final int sq = (relative * relative) / diff;
                        final double distance = border.distance - sq;

                        final double wall = this.wallNoise[(y + border.offset) & 255];
                        if (distance > wall) {
                            for (FastNoise noise : this.generators) {
                                final float value = noise.GetNoise(actualX, y, actualZ);

                                if (noise.IsInThreshold(value)) {
                                    if (height.contains(y)) {
                                        this.replaceBlock(rand, primer, x, y, z, chunkX, chunkZ);
                                        caverns[y][z][x] = true;
                                        break;
                                    } else {
                                        this.generateShell(rand, primer, x, y, z, y, chunkX, chunkZ);
                                    }
                                } else if (noise.IsOuter(value, decorators.shell.noiseThreshold)) {
                                    this.generateShell(rand, primer, x, y, z, y, chunkX, chunkZ);
                                }
                            }
                        } else if (distance > wall - d) {
                            this.generateShell(rand, primer, x, y, z, y, chunkX, chunkZ);
                        }
                    }
                }
            }
        }
        // Caverns must be completely generated before decorating.
        if (this.hasLocalDecorators()) {
            this.decorateCaverns(caverns, rand, primer, chunkX, chunkZ);
        }
    }

    private BorderData getNearestBorder(int x, int z) {
        double shortestDistance = Double.MAX_VALUE;
        int offset = 0;

        for (BiomeTestData invalid : invalidBiomes) {
            final double sum = Math.pow(x - invalid.x, 2) + Math.pow(z - invalid.z, 2);
            final double distance = Math.sqrt(sum);

            if (distance < shortestDistance) {
                shortestDistance = distance;
                offset = invalid.offset;
            }
        }
        return new BorderData(shortestDistance, offset);
    }

    private void decorateCaverns(boolean[][][] caverns, Random rand, ChunkPrimer primer, int chunkX, int chunkZ) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < maxY; y++) {
                    if (caverns[y][z][x]) {
                        decorateBlock(rand, primer, x, y, z, chunkX, chunkZ);
                    }
                }
            }
        }
    }

    @AllArgsConstructor
    private static class BiomeTestData {
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
