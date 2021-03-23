package com.personthecat.cavegenerator.world.generator;

import com.personthecat.cavegenerator.config.ConfigFile;
import com.personthecat.cavegenerator.data.CavernSettings;
import com.personthecat.cavegenerator.data.NoiseSettings;
import com.personthecat.cavegenerator.model.Range;
import com.personthecat.cavegenerator.world.HeightMapLocator;
import fastnoise.FastNoise;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkPrimer;
import org.apache.commons.lang3.ArrayUtils;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class CavernGenerator extends WorldCarver {

    private final List<FastNoise> generators;
    private final int maxY;

    public CavernGenerator(CavernSettings cfg, World world) {
        super(cfg.conditions, cfg.decorators, world);
        this.generators = createGenerators(cfg.generators, world);
        this.maxY = conditions.height.max + cfg.conditions.ceiling.map(n -> n.range.max).orElse(0);
    }

    private static List<FastNoise> createGenerators(List<NoiseSettings> settings, World world) {
        return settings.stream().map(s -> s.getGenerator(world)).collect(Collectors.toList());
    }

    @Override
    public void generate(PrimerContext ctx) {
        if (conditions.dimensions.test(ctx.world.provider.getDimension())) {
            generateChecked(ctx);
        }
    }

    @Override
    protected void generateChecked(PrimerContext ctx) {
        generateCaverns(ctx.world, ctx.heightmap, ctx.rand, ctx.primer, ctx.chunkX, ctx.chunkZ);
    }

    /** Generates giant air pockets in this chunk using a series of 3D noise generators. */
    private void generateCaverns(World world, int[][] heightmap, Random rand, ChunkPrimer primer, int chunkX, int chunkZ) {
        final boolean[][][] caverns = new boolean[maxY][16][16];
        for (int x = 0; x < 16; x++) {
            final int actualX = x + (chunkX * 16);
            for (int z = 0; z < 16; z++) {
                final int actualZ = z + (chunkZ * 16);
                if (!conditions.biomes.test(world.getBiome(new BlockPos(actualX, 0, actualZ)))) {
                    continue;
                }
                if (!conditions.region.GetBoolean(actualX, actualZ)) {
                    continue;
                }
                final int min = conditions.height.min + (int) conditions.floor.GetAdjustedNoise(actualX, actualZ);
                final int max = Math.min(conditions.height.max, heightmap [x][z]) + (int) conditions.ceiling.GetAdjustedNoise(actualX, actualZ);

                // if min == max -> stop
                for (int y = min; y < max; y++) {
                    if (!conditions.noise.GetBoolean(actualX, y, actualZ)) {
                        continue;
                    }

                    for (FastNoise noise : generators) {
                        if (noise.GetBoolean(actualX, y, actualZ)) {
                            replaceBlock(rand, primer, x, y, z, chunkX, chunkZ);
                            caverns[y][z][x] = true;
                            break;
                        }
                    }
                }
            }
        }
        // Caverns must be completed generated before decorating.
        if (hasLocalDecorators()) {
            decorateCaverns(caverns, rand, primer, chunkX, chunkZ);
        }
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
}
