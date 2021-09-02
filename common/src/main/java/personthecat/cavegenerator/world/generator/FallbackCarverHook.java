package personthecat.cavegenerator.world.generator;

import com.mojang.serialization.Codec;
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
import net.minecraft.world.level.levelgen.feature.Feature;
import personthecat.catlib.data.Lazy;
import personthecat.cavegenerator.CaveRegistries;
import personthecat.cavegenerator.config.Cfg;
import personthecat.cavegenerator.noise.CachedNoiseHelper;
import personthecat.cavegenerator.util.Reference;
import personthecat.cavegenerator.util.XoRoShiRo;
import personthecat.cavegenerator.world.BiomeSearch;
import personthecat.cavegenerator.world.GeneratorController;

import java.util.BitSet;
import java.util.Random;
import java.util.function.Function;

public class FallbackCarverHook extends WorldCarver<NoneCarverConfiguration> {

    public static final Lazy<ConfiguredWorldCarver<?>> INSTANCE =
        Lazy.of(() -> Registry.register(BuiltinRegistries.CONFIGURED_CARVER,
            new ResourceLocation(Reference.MOD_ID, "fallback_carver"),
            new FallbackCarverHook().configured(CarverConfiguration.NONE)));

    private final long seed = Cfg.FALLBACK_CARVER_SEED.getAsLong();

    public FallbackCarverHook() {
        super(Codec.unit(NoneCarverConfiguration.INSTANCE), 256);
    }

    @Override
    public boolean carve(ChunkAccess chunk, Function<BlockPos, Biome> biomes, Random rand, int seaLevel, int x, int z, int cX, int cZ, BitSet mask, NoneCarverConfiguration cfg) {
        if (x == cX && z == cZ) {
            final DummyBiomeManager manager = new DummyBiomeManager(biomes);
            final BiomeSearch search = BiomeSearch.in(manager, cX, cZ);
            final PrimerContext ctx = new PrimerContext(manager, search, this.seed, seaLevel, (ProtoChunk) chunk, GenerationStep.Carving.AIR);

            ctx.primeHeightmaps();
            CaveRegistries.CURRENT_SEED.set(new XoRoShiRo(this.seed), this.seed);
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
}
