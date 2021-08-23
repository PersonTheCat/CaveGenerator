package personthecat.cavegenerator.model;

import net.minecraft.world.level.block.state.BlockState;
import personthecat.cavegenerator.presets.data.WallDecoratorSettings;
import personthecat.cavegenerator.noise.DummyGenerator;
import personthecat.cavegenerator.world.generator.PrimerContext;
import personthecat.fastnoise.FastNoise;

import java.util.Random;

public class ConfiguredWallDecorator {

    public final WallDecoratorSettings cfg;
    public final FastNoise noise;

    public ConfiguredWallDecorator(final WallDecoratorSettings cfg, final Random rand, final long seed) {
        this.cfg = cfg;
        this.noise = cfg.noise.map(n -> n.getGenerator(rand, seed))
            .orElse(new DummyGenerator(0L));
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

    public boolean decidePlace(PrimerContext ctx, BlockState state, int x0, int y0, int z0, int xD, int yD, int zD) {
        if (WallDecoratorSettings.Placement.OVERLAY.equals(cfg.placement)) {
            ctx.set(x0, y0, z0, state);
            return true;
        }
        ctx.set(xD, yD, zD, state);
        return false;
    }

}
