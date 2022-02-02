package personthecat.cavegenerator.world.hook;

import com.mojang.serialization.Codec;
import lombok.extern.log4j.Log4j2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import personthecat.cavegenerator.CaveRegistries;
import personthecat.cavegenerator.noise.CachedNoiseHelper;
import personthecat.cavegenerator.util.Reference;
import personthecat.cavegenerator.world.GeneratorController;
import personthecat.cavegenerator.world.feature.WorldContext;
import personthecat.overwritevalidator.annotations.OverwriteTarget;

import java.util.Random;

@Log4j2
@OverwriteTarget
public class FallbackFeatureHook extends Feature<NoneFeatureConfiguration> {

    private static final FallbackFeatureHook INSTANCE = new FallbackFeatureHook();
    public static final ConfiguredFeature<?, ?> HOOK = INSTANCE.configured(FeatureConfiguration.NONE);

    private FallbackFeatureHook() {
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

    public static void register() {
        log.info("Loading fallback feature generator.");

        Registry.register(Registry.FEATURE,
            new ResourceLocation(Reference.MOD_ID, "fallback_feature"),
            INSTANCE);
        Registry.register(BuiltinRegistries.CONFIGURED_FEATURE,
            new ResourceLocation(Reference.MOD_ID, "configured_fallback_feature"),
            HOOK);
    }
}
