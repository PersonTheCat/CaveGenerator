package personthecat.cavegenerator.model;

import net.minecraft.world.level.block.state.BlockState;
import personthecat.cavegenerator.data.PondSettings;
import personthecat.cavegenerator.noise.DummyGenerator;
import personthecat.fastnoise.FastNoise;

import java.util.Random;

public class ConfiguredPond {

    public final PondSettings cfg;
    public final FastNoise noise;

    public ConfiguredPond(final PondSettings cfg, final Random rand, final long seed) {
        this.cfg = cfg;
        this.noise = cfg.noise.map(n -> n.getGenerator(rand, seed))
            .orElse(new DummyGenerator(0L));
    }

    public boolean canGenerate(Random rand, BlockState state, int x, int y, int z, int chunkX, int chunkZ) {
        return canGenerate(rand, x, y, z, chunkX, chunkZ) && matchesBlock(state);
    }

    public boolean canGenerate(Random rand, int x, int y, int z, int chunkX, int chunkZ) {
        return y >= cfg.height.min && y <= cfg.height.max// Height bounds
            && testNoise(x, y, z, chunkX, chunkZ); // Noise
    }

    /**
     * Returns true if the replacement doesn't have noise or if its noise at the given
     * coordinates meets the threshold.
     */
    private boolean testNoise(final int x, final int y, final int z, final int chunkX, final int chunkZ) {
        int actualX = (chunkX * 16) + x;
        int actualZ = (chunkZ * 16) + z;
        return testNoise(actualX, y, actualZ);
    }

    /** Variant of testNoise() that uses absolute coordinates. */
    private boolean testNoise(final int x, final int y, final int z) {
        return noise.getBoolean(x, y, z);
    }

    public boolean matchesBlock(BlockState state) {
        for (final BlockState matcher : cfg.matchers) {
            if (matcher.equals(state)) {
                return true;
            }
        }
        return false;
    }
}
