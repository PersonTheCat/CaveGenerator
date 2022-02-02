package personthecat.cavegenerator.world.hook;

import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.levelgen.GenerationStep.Carving;
import personthecat.cavegenerator.CaveRegistries;
import personthecat.cavegenerator.config.Cfg;
import personthecat.cavegenerator.noise.CachedNoiseHelper;
import personthecat.cavegenerator.util.XoRoShiRo;
import personthecat.cavegenerator.world.BiomeSearch;
import personthecat.cavegenerator.world.GeneratorController;
import personthecat.cavegenerator.world.feature.WorldContext;
import personthecat.cavegenerator.world.generator.PrimerContext;
import personthecat.cavegenerator.world.generator.WorldCarverAdapter;

public class CaveHooks {

    private CaveHooks() {}

    public static void injectCarvers(final long seed, final BiomeManager biomes, final ChunkAccess chunk,
                                     final Carving step, final BiomeSource biomeSource, final int seaLevel) {
        final BiomeManager withSource = biomes.withDifferentSource(biomeSource);
        final ChunkPos pos = chunk.getPos();
        final BiomeSearch search = BiomeSearch.in(withSource, pos.x, pos.z);
        final ProtoChunk primer = (ProtoChunk) chunk;
        final PrimerContext ctx = new PrimerContext(withSource, search, seed, seaLevel, primer, step);

        if (Cfg.enableOtherGenerators()) {
            WorldCarverAdapter.generate(ctx, biomeSource);
        }
        if (step == Carving.AIR) {
            ctx.primeHeightmaps();
            CaveRegistries.CURRENT_SEED.setIfAbsent(new XoRoShiRo(seed), seed);
            for (final GeneratorController controller : CaveRegistries.GENERATORS) {
                controller.earlyGenerate(ctx);
                controller.mapGenerate(ctx);
            }
        }
        CachedNoiseHelper.resetAll();
    }

    public static void injectFeatures(final WorldGenRegion region) {
        final WorldContext ctx = new WorldContext(region);
        CaveRegistries.CURRENT_SEED.setIfAbsent(ctx.rand, ctx.seed);

        for (final GeneratorController controller : CaveRegistries.GENERATORS) {
            controller.featureGenerate(ctx);
        }
        CachedNoiseHelper.resetAll();
    }
}
