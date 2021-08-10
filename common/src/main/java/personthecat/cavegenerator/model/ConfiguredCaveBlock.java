package personthecat.cavegenerator.model;

import net.minecraft.world.level.Level;
import personthecat.cavegenerator.data.CaveBlockSettings;
import personthecat.cavegenerator.noise.DummyGenerator;
import personthecat.fastnoise.FastNoise;

public class ConfiguredCaveBlock {

    public final CaveBlockSettings cfg;
    public final FastNoise noise;

    public ConfiguredCaveBlock(final CaveBlockSettings cfg, final Level level) {
        this.cfg = cfg;
        this.noise = cfg.noise.map(n -> n.getGenerator(level)).orElse(new DummyGenerator(0L));
    }

    public boolean canGenerate(final int x, final int y, final int z, final int chunkX, final int chunkZ) {
        return canGenerateAtHeight(y) && testNoise(x, y, z, chunkX, chunkZ);
    }

    private boolean canGenerateAtHeight(final int y) {
        return y >= cfg.height.min && y <= cfg.height.max;
    }

    /**
     * Returns true if the replacement doesn't have noise or
     * if its noise at the given coords meets the threshold.
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

}
