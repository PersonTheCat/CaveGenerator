package personthecat.cavegenerator.world.config;

import lombok.AllArgsConstructor;
import net.minecraft.world.level.block.state.BlockState;
import personthecat.catlib.data.Range;
import personthecat.cavegenerator.model.Direction;
import personthecat.cavegenerator.presets.data.WallDecoratorSettings;
import personthecat.cavegenerator.world.generator.PrimerContext;
import personthecat.fastnoise.FastNoise;

import java.util.List;
import java.util.Random;
import java.util.Set;

@AllArgsConstructor
public class WallDecoratorConfig {
    public final List<BlockState> states;
    public final double integrity;
    public final Range height;
    public final List<Direction> directions;
    public final Set<BlockState> matchers;
    public final WallDecoratorSettings.Placement placement;
    public final FastNoise noise;

    public boolean canGenerate(Random rand, BlockState state, int x, int y, int z, int cX, int cZ) {
        return canGenerate(rand, x, y, z, cX, cZ) && matchesBlock(state);
    }

    public boolean canGenerate(final Random rand, final int x, final int y, int z, final int cX, final int cZ) {
        return y >= this.height.min && y <= this.height.max// Height bounds
            && testNoise(x, y, z, cX, cZ); // Noise
    }

    private boolean testNoise(int x, int y, int z, int chunkX, int chunkZ) {
        int actualX = (chunkX * 16) + x;
        int actualZ = (chunkZ * 16) + z;
        return testNoise(actualX, y, actualZ);
    }

    private boolean testNoise(int x, int y, int z) {
        return noise.getBoolean(x, y, z);
    }

    public boolean matchesBlock(final BlockState state) {
        return this.matchers.contains(state);
    }

    public boolean decidePlace(PrimerContext ctx, BlockState state, int x0, int y0, int z0, int xD, int yD, int zD) {
        if (WallDecoratorSettings.Placement.OVERLAY.equals(this.placement)) {
            ctx.set(x0, y0, z0, state);
            return true;
        }
        ctx.set(xD, yD, zD, state);
        return false;
    }

}
