package personthecat.cavegenerator.model;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import personthecat.cavegenerator.data.WallDecoratorSettings;
import personthecat.cavegenerator.noise.DummyGenerator;
import personthecat.fastnoise.FastNoise;

import java.util.Random;

public class ConfiguredWallDecorator {

    public final WallDecoratorSettings cfg;
    public final FastNoise noise;

    public ConfiguredWallDecorator(final WallDecoratorSettings cfg, final Level level) {
        this.cfg = cfg;
        this.noise = cfg.noise.map(n -> n.getGenerator(level)).orElse(new DummyGenerator(0L));
    }

    public boolean canGenerate(Random rand, BlockState state, int x, int y, int z, int cX, int cZ) {
        return canGenerate(rand, x, y, z, cX, cZ) && matchesBlock(state);
    }

    public boolean canGenerate(final Random rand, final int x, final int y, int z, final int cX, final int cZ) {
        return y >= cfg.height.min && y <= cfg.height.max// Height bounds
            && testNoise(x, y, z, cX, cZ); // Noise
    }

    /**
     * Returns true if the replacement doesn't have noise or if its noise at the given
     * coordinates meets the threshold.
     */
    private boolean testNoise(int x, int y, int z, int chunkX, int chunkZ) {
        int actualX = (chunkX * 16) + x;
        int actualZ = (chunkZ * 16) + z;
        return testNoise(actualX, y, actualZ);
    }

    /** Variant of testNoise() that uses absolute coordinates. */
    private boolean testNoise(int x, int y, int z) {
        return noise.getBoolean(x, y, z);
    }

    public boolean matchesBlock(final BlockState state) {
        for (final BlockState matcher : cfg.matchers) {
            if (matcher.equals(state)) {
                return true;
            }
        }
        return false;
    }

//    public boolean decidePlace(BlockState state, ChunkPrimer primer, int xO, int yO, int zO, int xD, int yD, int zD) {
//        if (WallDecoratorSettings.Placement.OVERLAY.equals(cfg.placement)) {
//            primer.setBlockState(xO, yO, zO, state);
//            return true;
//        }
//        primer.setBlockState(xD, yD, zD, state);
//        return false;
//    }

}
