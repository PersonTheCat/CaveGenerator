package personthecat.cavegenerator.world.hook;

import com.mojang.serialization.Codec;
import lombok.extern.log4j.Log4j2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.carver.CarverConfiguration;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.carver.NoneCarverConfiguration;
import net.minecraft.world.level.levelgen.carver.WorldCarver;
import personthecat.cavegenerator.CaveRegistries;
import personthecat.cavegenerator.config.Cfg;
import personthecat.cavegenerator.noise.CachedNoiseHelper;
import personthecat.cavegenerator.util.Reference;
import personthecat.cavegenerator.util.XoRoShiRo;
import personthecat.cavegenerator.world.BiomeSearch;
import personthecat.cavegenerator.world.GeneratorController;
import personthecat.cavegenerator.world.generator.DummyBiomeManager;
import personthecat.cavegenerator.world.generator.PrimerContext;
import personthecat.overwritevalidator.annotations.OverwriteTarget;

import java.util.BitSet;
import java.util.Random;
import java.util.function.Function;

@Log4j2
@OverwriteTarget
public class FallbackCarverHook extends WorldCarver<NoneCarverConfiguration> {

    private static final WorldCarver<NoneCarverConfiguration> INSTANCE = new FallbackCarverHook();
    public static final ConfiguredWorldCarver<?> HOOK = INSTANCE.configured(CarverConfiguration.NONE);

    private final long seed = Cfg.fallbackCarverSeed();

    private FallbackCarverHook() {
        super(Codec.unit(NoneCarverConfiguration.INSTANCE), 256);
    }

    @Override
    public boolean carve(ChunkAccess chunk, Function<BlockPos, Biome> biomes, Random rand, int seaLevel, int x, int z, int cX, int cZ, BitSet mask, NoneCarverConfiguration cfg) {
        if (x == cX && z == cZ) {
            final DummyBiomeManager manager = new DummyBiomeManager(biomes);
            final BiomeSearch search = BiomeSearch.in(manager, cX, cZ);
            final PrimerContext ctx = new PrimerContext(manager, search, this.seed, seaLevel, (ProtoChunk) chunk, GenerationStep.Carving.AIR);

            ctx.primeHeightmaps();
            CaveRegistries.CURRENT_SEED.setIfAbsent(new XoRoShiRo(this.seed), this.seed);
            for (final GeneratorController controller : CaveRegistries.GENERATORS) {
                controller.earlyGenerate(ctx);
                controller.mapGenerate(ctx);
            }
            CachedNoiseHelper.resetAll();
            return true;
        }
        return false;
    }

    @Override
    public boolean isStartChunk(Random random, int i, int j, NoneCarverConfiguration cfg) {
        return true;
    }

    @Override
    protected boolean skip(double d, double e, double f, int i) {
        return false;
    }

    public static void register() {
        log.info("Loading fallback carver generator.");

        Registry.register(Registry.CARVER,
            new ResourceLocation(Reference.MOD_ID, "fallback_carver"),
            INSTANCE);
        Registry.register(BuiltinRegistries.CONFIGURED_CARVER,
            new ResourceLocation(Reference.MOD_ID, "configured_fallback_carver"),
            HOOK);
    }
}
