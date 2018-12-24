package com.personthecat.cavegenerator.world;

import com.personthecat.cavegenerator.CaveInit;
import com.personthecat.cavegenerator.Main;
import com.personthecat.cavegenerator.config.ConfigFile;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.MapGenBase;

import java.util.Map;

import static com.personthecat.cavegenerator.util.CommonMethods.*;

public class CaveManager extends MapGenBase {
    @Override
    public void generate(World world, int x, int z, ChunkPrimer primer) {
        // Again, these must be retrieved statically. Can't
        // Change this method's signature.
        Map<String, CaveGenerator> generators = Main.instance.generators;
        int dimension = world.provider.getDimension();

        if (ConfigFile.otherGeneratorEnabled) {
            // Generate simultaneously with one other generator.
            Main.instance.priorCaves.ifPresent((gen) ->
                gen.generate(world, x, z, primer)
            );
        } else if (!CaveInit.anyGeneratorEnabled(generators, dimension)) {
            // No generators are enabled for this dimension.
            // Allow the most recent mod in the queue to
            // generate and then move on.
            Main.instance.priorCaves.ifPresent((gen) ->
                gen.generate(world, x, z, primer)
            );
            return;
        }
        noiseGenerate(generators, world, dimension, x, z, primer);
        // Calls `recursiveGenerate()` recursively.
        super.generate(world, x, z, primer);
    }

    /** Handle all noise-based generation for this generator. */
    private void noiseGenerate(Map<String, CaveGenerator> gens, World world, int dim, int x, int z, ChunkPrimer primer) {
        // To-do
    }

    @Override
    protected void recursiveGenerate(World world, int chunkX, int chunkZ, int originalX, int originalZ, ChunkPrimer primer) {
        for (CaveGenerator generator : Main.instance.generators.values()) {
            Biome centerBiome = world.getBiome(centerCoords(chunkX, chunkZ));
            if (generator.canGenerate(world.provider.getDimension(), centerBiome)) {
                generator.startTunnelSystem(rand, chunkX, chunkZ, originalX, originalZ, primer);
            }
        }
    }
}