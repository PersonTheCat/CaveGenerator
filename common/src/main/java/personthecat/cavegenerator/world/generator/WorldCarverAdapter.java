package personthecat.cavegenerator.world.generator;

import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import personthecat.cavegenerator.config.Cfg;

import java.util.BitSet;
import java.util.List;
import java.util.function.Supplier;

public class WorldCarverAdapter {
    public static void generate(final PrimerContext ctx, final BiomeSource source) {
        final int cX = ctx.chunkX;
        final int cZ = ctx.chunkZ;
        final Biome sourceBiome = source.getNoiseBiome(cX << 2, 0, cZ << 2);
        final BiomeGenerationSettings settings = sourceBiome.getGenerationSettings();
        final BitSet carvingMask = ctx.primer.getOrCreateCarvingMask(ctx.step);
        final WorldgenRandom rand = new WorldgenRandom();

        final int r = Cfg.mapRange();
        for (int x = cX - r; x <= cX + r; x++) {
            for (int z = cZ - r; z <= cZ + r; z++) {
                final List<Supplier<ConfiguredWorldCarver<?>>> carvers = settings.getCarvers(ctx.step);

                for (int i = 0; i < carvers.size(); i++) {
                    final ConfiguredWorldCarver<?> carver = carvers.get(i).get();
                    rand.setLargeFeatureSeed(ctx.seed + i, x, z);
                    if (carver.isStartChunk(rand, x, z)) {
                        carver.carve(ctx.primer, ctx.provider::getBiome, rand, ctx.seaLevel, x, z, cX, cZ, carvingMask);
                    }
                }
            }
        }
    }
}
