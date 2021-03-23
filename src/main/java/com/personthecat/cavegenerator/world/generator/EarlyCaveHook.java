package com.personthecat.cavegenerator.world.generator;

import com.personthecat.cavegenerator.Main;
import com.personthecat.cavegenerator.config.ConfigFile;
import com.personthecat.cavegenerator.world.GeneratorController;
import com.personthecat.cavegenerator.world.HeightMapLocator;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.MapGenBase;
import org.apache.commons.lang3.ArrayUtils;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collection;
import java.util.Map;
import java.util.Random;

@ParametersAreNonnullByDefault
public class EarlyCaveHook extends MapGenBase {

    private final Random rand = new Random();
    @Nullable MapGenBase priorCaves;

    public EarlyCaveHook(@Nullable MapGenBase priorCaves) {
        this.priorCaves = priorCaves;
    }

    @Override
    public void generate(World world, int x, int z, ChunkPrimer primer) {
        if (ConfigFile.otherGeneratorEnabled && priorCaves != null) {
            priorCaves.generate(world, x, z, primer);
        }
        // Don't really have a good way to access this without writing the game myself.
        final Map<String, GeneratorController> generators = Main.instance.loadGenerators(world);
        final int[][] heightmap = ArrayUtils.contains(ConfigFile.heightMapDims, world.provider.getDimension())
            ? HeightMapLocator.getHeightFromPrimer(primer)
            : HeightMapLocator.FAUX_MAP;

        earlyGenerate(generators.values(), heightmap, world, x, z, primer);
        mapGenerate(generators.values(), heightmap, world, x, z, primer);
    }

    private void earlyGenerate(Collection<GeneratorController> generators, int[][] heightmap, World world, int x, int z, ChunkPrimer primer) {
        final PrimerContext ctx = new PrimerContext(heightmap, world, world.rand, x, z, x, z, primer);
        for (GeneratorController generator : generators) {
            generator.earlyGenerate(ctx);
        }
    }

    private void mapGenerate(Collection<GeneratorController> generators, int[][] heightmap, World world, int x, int z, ChunkPrimer primer) {
        final int range = ConfigFile.mapRange;
        this.rand.setSeed(world.getSeed());
        final long xMask = this.rand.nextLong();
        final long zMask = this.rand.nextLong();

        for (int destX = x - range; destX <= x + range; destX++) {
            for (int destZ = z - range; destZ <= z + range; destZ++) {
                long xHash = (long) destX * xMask;
                long zHash = (long) destZ * zMask;
                this.rand.setSeed(xHash ^ zHash ^ world.getSeed());

                final PrimerContext ctx = new PrimerContext(heightmap, world, rand, destX, destZ, x, z, primer);
                for (GeneratorController generator : generators) {
                    generator.mapGenerate(ctx);
                }
            }
        }
    }
}
