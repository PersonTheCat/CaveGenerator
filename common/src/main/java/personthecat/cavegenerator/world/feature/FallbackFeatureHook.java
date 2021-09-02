package personthecat.cavegenerator.world.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import personthecat.cavegenerator.CaveRegistries;
import personthecat.cavegenerator.noise.CachedNoiseHelper;
import personthecat.cavegenerator.world.GeneratorController;

import java.util.Random;

public class FallbackFeatureHook extends Feature<NoneFeatureConfiguration> {

    public FallbackFeatureHook() {
        super(Codec.unit(NoneFeatureConfiguration.INSTANCE));
    }

    @Override
    public boolean place(WorldGenLevel world, ChunkGenerator chunk, Random rand, BlockPos pos, NoneFeatureConfiguration cfg) {
        final WorldContext ctx = new WorldContext((WorldGenRegion) world);
        CaveRegistries.CURRENT_SEED.setIfAbsent(ctx.rand, ctx.seed);

        for (final GeneratorController controller : CaveRegistries.GENERATORS) {
            controller.featureGenerate(ctx);
        }
        CachedNoiseHelper.resetAll();
        return false;
    }
}
