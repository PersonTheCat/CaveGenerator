package personthecat.cavegenerator.world.generator;

import net.minecraft.core.BlockPos;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.FuzzyOffsetBiomeZoomer;

import java.util.function.Function;

public class DummyBiomeManager extends BiomeManager {
    private static final Biome DUMMY_NOISE_BIOME = BuiltinRegistries.BIOME.iterator().next();

    private final Function<BlockPos, Biome> wrapped;

    public DummyBiomeManager(final Function<BlockPos, Biome> wrapped) {
        super((x, y, z) -> { throw new AssertionError(); }, 0L, FuzzyOffsetBiomeZoomer.INSTANCE);
        this.wrapped = wrapped;
    }

    @Override
    public Biome getBiome(final BlockPos pos) {
        return this.wrapped.apply(pos);
    }

    @Override
    public Biome getNoiseBiomeAtQuart(int x, int y, int z) {
        return this.wrapped.apply(new BlockPos(x, y, z));
    }
}
