package com.personthecat.cavegenerator.world.feature;

import com.personthecat.cavegenerator.CaveInit;
import com.personthecat.cavegenerator.Main;
import com.personthecat.cavegenerator.world.CaveGenerator;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraftforge.fml.common.IWorldGenerator;

import java.util.Map;
import java.util.Random;

public class CaveFeatureGenerator implements IWorldGenerator {
    @Override
    public void generate(Random rand, int chunkX, int chunkZ, World world, IChunkGenerator chunkGen, IChunkProvider chunkProv) {
        // Once again, there is no way to avoid retrieving this statically.
        final Map<String, CaveGenerator> generators = Main.instance.generators;
        final int dimension = world.provider.getDimension();

        if (CaveInit.anyGeneratorEnabled(generators, dimension)) {
            Chunk chunk = world.getChunkFromChunkCoords(chunkX, chunkZ);
            // int[][] heightMap = HeightMapLocator.getHeightFromChunk(chunk);

            for (CaveGenerator generator : generators.values()) {
                if (generator.canGenerate(dimension)) { // Do biome test later on.
                    // generatePillars(generator, rand, chunkX, chunkZ, world);
                    // generateStalactites(generator, rand, chunkX, chunkZ, world);
                    // generateStructures(heightMap, generator, rand, chunkX, chunkZ, world);
                }
            }
        }
    }
}