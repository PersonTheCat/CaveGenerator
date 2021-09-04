package personthecat.cavegenerator.world.feature;

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
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.ForgeRegistry;
import personthecat.catlib.data.Lazy;
import personthecat.cavegenerator.CaveRegistries;
import personthecat.cavegenerator.noise.CachedNoiseHelper;
import personthecat.cavegenerator.util.Reference;
import personthecat.cavegenerator.world.GeneratorController;
import personthecat.overwritevalidator.annotations.Inherit;
import personthecat.overwritevalidator.annotations.InheritMissingMembers;
import personthecat.overwritevalidator.annotations.Overwrite;
import personthecat.overwritevalidator.annotations.OverwriteClass;

import java.util.Random;

@Log4j2
@OverwriteClass
@InheritMissingMembers
public class FallbackFeatureHook extends Feature<NoneFeatureConfiguration> {

    @Inherit
    private static final FallbackFeatureHook INSTANCE = new FallbackFeatureHook();

    @Inherit
    public static final ConfiguredFeature<?, ?> HOOK = INSTANCE.configured(FeatureConfiguration.NONE);

    @Inherit
    private FallbackFeatureHook() {
        super(Codec.unit(NoneFeatureConfiguration.INSTANCE));
    }

    @Inherit
    @Override
    public boolean place(WorldGenLevel world, ChunkGenerator chunk, Random rand, BlockPos pos, NoneFeatureConfiguration cfg) {
        return false;
    }

    @Overwrite
    public static void register() {
        log.info("Loading fallback feature generator.");

        ((ForgeRegistry<?>) ForgeRegistries.FEATURES).unfreeze();
        ForgeRegistries.FEATURES
            .register(INSTANCE.setRegistryName(new ResourceLocation(Reference.MOD_ID, "fallback_feature")));
        ((ForgeRegistry<?>) ForgeRegistries.FEATURES).freeze();

        Registry.register(BuiltinRegistries.CONFIGURED_FEATURE,
            new ResourceLocation(Reference.MOD_ID, "configured_fallback_feature"),
            HOOK);
    }
}
