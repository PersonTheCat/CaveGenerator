package personthecat.cavegenerator.world.config;

import lombok.AllArgsConstructor;
import net.minecraft.world.level.block.state.BlockState;
import personthecat.catlib.data.Range;
import personthecat.fastnoise.FastNoise;

import java.util.List;
import java.util.Random;
import java.util.Set;

@AllArgsConstructor
public class PondConfig {
    public final List<BlockState> states;
    public final double integrity;
    public final Range height;
    public final int depth;
    public final Set<BlockState> matchers;
    public final FastNoise noise;

    public boolean canGenerate(Random rand, BlockState state, int x, int y, int z, int chunkX, int chunkZ) {
        return canGenerate(rand, x, y, z, chunkX, chunkZ) && matchesBlock(state);
    }

    public boolean canGenerate(Random rand, int x, int y, int z, int chunkX, int chunkZ) {
        return y >= this.height.min && y <= this.height.max// Height bounds
            && testNoise(x, y, z, chunkX, chunkZ); // Noise
    }

    private boolean testNoise(final int x, final int y, final int z, final int chunkX, final int chunkZ) {
        int actualX = (chunkX * 16) + x;
        int actualZ = (chunkZ * 16) + z;
        return testNoise(actualX, y, actualZ);
    }

    private boolean testNoise(final int x, final int y, final int z) {
        return this.noise.getBoolean(x, y, z);
    }

    public boolean matchesBlock(final BlockState state) {
        for (final BlockState matcher : this.matchers) {
            if (matcher.equals(state)) {
                return true;
            }
        }
        return false;
    }
}
