package personthecat.cavegenerator.world.hook;

import com.mojang.serialization.Codec;
import lombok.extern.log4j.Log4j2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.carver.CarverConfiguration;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.carver.NoneCarverConfiguration;
import net.minecraft.world.level.levelgen.carver.WorldCarver;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.ForgeRegistry;
import personthecat.cavegenerator.util.Reference;
import personthecat.overwritevalidator.annotations.Inherit;
import personthecat.overwritevalidator.annotations.InheritMissingMembers;
import personthecat.overwritevalidator.annotations.Overwrite;
import personthecat.overwritevalidator.annotations.OverwriteClass;

import java.util.BitSet;
import java.util.Random;
import java.util.function.Function;

@Log4j2
@OverwriteClass
@InheritMissingMembers
public class FallbackCarverHook extends WorldCarver<NoneCarverConfiguration> {

    @Inherit
    private static final WorldCarver<NoneCarverConfiguration> INSTANCE = new personthecat.cavegenerator.world.hook.FallbackCarverHook();

    @Inherit
    public static final ConfiguredWorldCarver<?> HOOK = INSTANCE.configured(CarverConfiguration.NONE);

    @Inherit
    public FallbackCarverHook() {
        super(Codec.unit(NoneCarverConfiguration.INSTANCE), 256);
    }

    @Inherit
    @Override
    public boolean carve(ChunkAccess chunk, Function<BlockPos, Biome> biomes, Random rand, int seaLevel, int x, int z, int cX, int cZ, BitSet mask, NoneCarverConfiguration cfg) {
        return false;
    }

    @Inherit
    @Override
    public boolean isStartChunk(Random random, int i, int j, NoneCarverConfiguration cfg) {
        return true;
    }

    @Inherit
    @Override
    protected boolean skip(double d, double e, double f, int i) {
        return false;
    }

    @Overwrite
    public static void register() {
        log.info("Loading fallback carver generator.");

        ((ForgeRegistry<?>) ForgeRegistries.WORLD_CARVERS).unfreeze();
        ForgeRegistries.WORLD_CARVERS
            .register(INSTANCE.setRegistryName(new ResourceLocation(Reference.MOD_ID, "fallback_carver")));
        ((ForgeRegistry<?>) ForgeRegistries.WORLD_CARVERS).freeze();

        Registry.register(BuiltinRegistries.CONFIGURED_CARVER,
            new ResourceLocation(Reference.MOD_ID, "configured_fallback_carver"),
            INSTANCE.configured(CarverConfiguration.NONE));
    }
}
