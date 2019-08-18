package com.personthecat.cavegenerator.world;

import com.personthecat.cavegenerator.CaveInit;
import com.personthecat.cavegenerator.Main;
import com.personthecat.cavegenerator.config.ConfigFile;
import static com.personthecat.cavegenerator.util.CommonMethods.*;

import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.MapGenBase;

import java.util.Map;

public class RavineManager extends MapGenBase {
    @Override
    public void generate(World world, int x, int z, ChunkPrimer primer) {
        // Again, these must be retrieved statically. Can't
        // Change this method's signature.
        // Generators were loaded on CaveManager#generate.
        Map<String, CaveGenerator> generators = Main.instance.getGenerators(world);
        int dimension = world.provider.getDimension();

        if (ConfigFile.otherGeneratorEnabled) {
            // Generate simultaneously with one other generator.
            Main.instance.priorRavines.ifPresent((gen) ->
                gen.generate(world, x, z, primer)
            );
        } else if (!CaveInit.anyGeneratorEnabled(generators, dimension)) {
            // No generators are enabled for this dimension.
            // Allow the most recent mod in the queue to
            // generate and then move on.
            Main.instance.priorRavines.ifPresent((gen) ->
                gen.generate(world, x, z, primer)
            );
            return;
        }
        // Calls `recursiveGenerate()` recursively.
        super.generate(world, x, z, primer);
    }

    @Override
    protected void recursiveGenerate(World world, int chunkX, int chunkZ, int originalX, int originalZ, ChunkPrimer primer) {
        final int dimension = world.provider.getDimension();
        for (CaveGenerator gen : Main.instance.getGenerators(world).values()) {
            Biome centerBiome = world.getBiome(centerCoords(chunkX, chunkZ));
            int chance = gen.settings.ravines.inverseChance;

            // Filter generators that aren't enabled under these conditions
            // and generate by probability.
            if (gen.canGenerate(dimension, centerBiome) && rand.nextInt(chance) == 0) {
                gen.startRavine(rand, chunkX, chunkZ, originalX, originalZ, primer);
            }
        }
    }
}