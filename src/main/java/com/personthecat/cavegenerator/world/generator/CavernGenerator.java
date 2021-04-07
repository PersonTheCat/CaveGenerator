package com.personthecat.cavegenerator.world.generator;

import com.personthecat.cavegenerator.data.CavernSettings;
import com.personthecat.cavegenerator.data.NoiseMapSettings;
import com.personthecat.cavegenerator.data.NoiseSettings;
import com.personthecat.cavegenerator.model.Range;
import com.personthecat.cavegenerator.world.BiomeSearch;
import fastnoise.FastNoise;
import lombok.AllArgsConstructor;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.ChunkPrimer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class CavernGenerator extends WorldCarver {

    private final List<BiomeTestData> invalidBiomes = new ArrayList<>(BiomeSearch.size());
    private final double[] wallNoise = new double[256];
    private final List<FastNoise> generators;
    private final FastNoise wallOffset;
    private final int maxY;
    private final CavernSettings cfg;

    public CavernGenerator(CavernSettings cfg, World world) {
        super(cfg.conditions, cfg.decorators, world);
        this.generators = createGenerators(cfg.generators, world);
        this.wallOffset = cfg.wallOffset.getGenerator(world);
        this.maxY = conditions.height.max + cfg.conditions.ceiling.map(n -> n.range.max).orElse(0);
        this.cfg = cfg;
        setupWallNoise(cfg.walls, world);
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
            wallNoise[i] = noise.GetAdjustedNoise(x, y);
        }
    }

    @Override
    public void generate(PrimerContext ctx) {
        if (conditions.dimensions.test(ctx.world.provider.getDimension())) {
            if (conditions.hasBiomes) {
                if (anyMatches(ctx.biomes, conditions.biomes)) {
                    fillInvalidBiomes(ctx.biomes);
                    generateChecked(ctx);
                    invalidBiomes.clear();
                }
            } else {
                generateChecked(ctx);
            }
        }
    }

    private static boolean anyMatches(BiomeSearch biomes, Predicate<Biome> predicate) {
        for (Biome b : biomes.current.get()) {
            if (predicate.test(b)) {
                return true;
            }
        }
        return false;
    }

    private void fillInvalidBiomes(BiomeSearch biomes) {
        for (BiomeSearch.Data biome : biomes.surrounding.get()) {
            if (!conditions.biomes.test(biome.biome)) {
                // Translate the noise randomly for each chunk to minimize repetition.
                final int translateY = (int) wallOffset.GetAdjustedNoise(biome.centerX, biome.centerZ);
                invalidBiomes.add(new BiomeTestData(biome.centerX, biome.centerZ, translateY));
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

                for (int y : height) {
                    final int relative = minOffset + height.max - y;
                    final int sq = (relative * relative) / diff;
                    final double distance = border.distance - sq;

                    final double wall = this.wallNoise[(y + border.offset) & 255];
                    if (distance > wall && this.conditions.noise.GetBoolean(actualX, y, actualZ)) {
                        for (FastNoise noise : this.generators) {
                            final float value = noise.GetNoise(actualX, y, actualZ);

                            if (noise.IsInThreshold(value)) {
                                this.replaceBlock(rand, primer, x, y, z, chunkX, chunkZ);
                                caverns[y][z][x] = true;
                                break;
                            } else if (noise.IsOuter(value, decorators.shell.noiseThreshold)) {
                                this.generateShell(rand, primer, x, y, z, y, chunkX, chunkZ);
                            }
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
