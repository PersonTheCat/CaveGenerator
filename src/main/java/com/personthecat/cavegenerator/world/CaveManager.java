package com.personthecat.cavegenerator.world;

import com.personthecat.cavegenerator.CaveInit;
import com.personthecat.cavegenerator.Main;
import com.personthecat.cavegenerator.config.ConfigFile;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.MapGenBase;

import java.util.Map;
import java.util.Optional;

import static com.personthecat.cavegenerator.util.CommonMethods.*;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class CaveManager extends MapGenBase {

    private final Optional<MapGenBase> priorCaves;

    public CaveManager(Optional<MapGenBase> priorCaves) {
        this.priorCaves = priorCaves;
    }

    @Override
    public void generate(World world, int x, int z, ChunkPrimer primer) {
        // Again, these must be retrieved statically. Can't
        // Change this method's signature.
        final Map<String, CaveGenerator> generators = Main.instance.loadGenerators(world);
        final int dimension = world.provider.getDimension();

        if (ConfigFile.otherGeneratorEnabled) {
            // Generate simultaneously with one other generator.
            priorCaves.ifPresent((gen) ->
                gen.generate(world, x, z, primer)
            );
        } else if (!CaveInit.anyGeneratorEnabled(generators, dimension)) {
            // No generators are enabled for this dimension.
            // Allow the most recent mod in the queue to
            // generate and then move on.
            priorCaves.ifPresent((gen) ->
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
        // Only generate this map once per chunk.
        final int[][] heightMap = CaveInit.anyCavernsEnabled(Main.instance.generators.get(dim), dim) ?
            HeightMapLocator.getHeightFromPrimer(primer) :
            HeightMapLocator.FAUX_MAP;

        final Biome centerBiome = world.getBiome(centerCoords(x, z));

        for (CaveGenerator generator : gens.values()) {
            // These have their own internal checks.
            generator.generateClusters(rand, primer, x, z);
            if (generator.canGenerate(dim, centerBiome)) {
                generator.generateLayers(primer, x, z);
            }
        }
        for (CaveGenerator generator : gens.values()) {
            // Don't allow caverns to be biome-specific, for now.
            if (!ConfigFile.forceEnableCavernBiomes || generator.canGenerate(centerBiome)) {
                if (generator.canGenerate(dim)) {
                    generator.generateCaverns(heightMap, rand, primer, x, z);
                }
            }
        }
    }

    @Override
    protected void recursiveGenerate(World world, int chunkX, int chunkZ, int originalX, int originalZ, ChunkPrimer primer) {
        // Generators were loaded before this function was called. No need to reload.
        for (CaveGenerator generator : Main.instance.loadGenerators(world).values()) {
            Biome centerBiome = world.getBiome(centerCoords(chunkX, chunkZ));
            if (generator.canGenerate(world.provider.getDimension(), centerBiome)) {
                generator.startTunnels(rand, chunkX, chunkZ, originalX, originalZ, primer);
            }
        }
    }
}