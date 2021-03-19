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

    private final CavernSettings cfg;
    private final List<FastNoise> generators;

    public CavernGenerator(CavernSettings cfg, World world) {
        super(cfg.conditions, cfg.decorators, world);
        this.cfg = cfg;
        this.generators = createGenerators(cfg.generators, world);
    }

    private static List<FastNoise> createGenerators(List<NoiseSettings> settings, World world) {
        return settings.stream().map(s -> s.getGenerator(world)).collect(Collectors.toList());
    }

    @Override
    protected void doGenerate(World world, Random rand, int destChunkX, int destChunkZ, int chunkX, int chunkZ, ChunkPrimer primer) {
        final int[][] heightmap = ArrayUtils.contains(ConfigFile.heightMapDims, world.provider.getDimension())
            ? HeightMapLocator.getHeightFromPrimer(primer)
            : HeightMapLocator.FAUX_MAP;

        generateCaverns(world, heightmap, rand, primer, chunkX, chunkZ);
    }

    /** Generates giant air pockets in this chunk using a 3D noise generator. */
    private void generateCaverns(World world, int[][] heightmap, Random rand, ChunkPrimer primer, int chunkX, int chunkZ) {
        for (int x = 0; x < 16; x++) {
            final int actualX = x + (chunkX * 16);
            for (int z = 0; z < 16; z++) {
                final int actualZ = z + (chunkZ * 16);
                if (!conditions.biomes.test(world.getBiome(new BlockPos(actualX, 0, actualZ)))) {
                    continue;
                }

                final Range height = conditions.getColumn(actualX, actualZ);
                final int max = Math.min(height.max, heightmap[x][z]);

                for (int y = height.min; y <= max; y++) {
                    for (FastNoise noise : generators) {
                        // Todo: Refactor this to store the actual noise so we can continue doing calculations.
                        if (noise.GetBoolean(actualX, y, actualZ)) {
                            replaceBlock(rand, primer, x, y, z, chunkX, chunkZ);
                            if (hasLocalDecorators()) {
                                decorateBlock(rand, primer, x, y, z, chunkX, chunkZ);
                            }
                            break;
                        }
                    }
                }
            }
        }
    }

}
