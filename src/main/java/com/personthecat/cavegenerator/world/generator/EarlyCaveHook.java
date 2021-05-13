package com.personthecat.cavegenerator.world.generator;

import com.personthecat.cavegenerator.Main;
import com.personthecat.cavegenerator.config.ConfigFile;
import com.personthecat.cavegenerator.noise.CachedNoiseHelper;
import com.personthecat.cavegenerator.world.BiomeSearch;
import com.personthecat.cavegenerator.world.GeneratorController;
import com.personthecat.cavegenerator.world.HeightMapLocator;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.MapGenBase;
import org.apache.commons.lang3.ArrayUtils;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;

@ParametersAreNonnullByDefault
public class EarlyCaveHook extends MapGenBase {

    @Nullable MapGenBase priorCaves;

    public EarlyCaveHook(@Nullable MapGenBase priorCaves) {
        this.priorCaves = priorCaves;
    }

    @Override
    public void generate(World world, int x, int z, ChunkPrimer primer) {
        if (ConfigFile.otherGeneratorEnabled && this.priorCaves != null) {
            this.priorCaves.generate(world, x, z, primer);
        }
        // Don't really have a good way to access this without writing the game myself.
        final Map<String, GeneratorController> generators = Main.instance.loadGenerators(world);
        final int[][] heightmap = ArrayUtils.contains(ConfigFile.heightMapDims, world.provider.getDimension())
            ? HeightMapLocator.getHeightFromPrimer(primer)
            : HeightMapLocator.FAUX_MAP;

        final BiomeSearch biomes = BiomeSearch.in(world, x, z);
        final PrimerContext ctx = new PrimerContext(biomes, heightmap, world, x, z, primer);
        ctx.world.rand.setSeed(ctx.world.getSeed());
        for (GeneratorController generator : generators.values()) {
            generator.earlyGenerate(ctx);
            generator.mapGenerate(ctx);
        }
        CachedNoiseHelper.resetAll();
    }
}
