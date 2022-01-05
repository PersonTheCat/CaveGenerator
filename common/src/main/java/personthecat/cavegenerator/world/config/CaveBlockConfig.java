package personthecat.cavegenerator.world.config;

import lombok.AllArgsConstructor;
import net.minecraft.world.level.block.state.BlockState;
import personthecat.catlib.data.Range;
import personthecat.fastnoise.FastNoise;

import java.util.List;

@AllArgsConstructor
public class CaveBlockConfig {
    public final List<BlockState> states;
    public final double integrity;
    public final Range height;
    public final FastNoise noise;

    public boolean canGenerate(final int x, final int y, final int z, final int chunkX, final int chunkZ) {
        return canGenerateAtHeight(y) && testNoise(x, y, z, chunkX, chunkZ);
    }

    private boolean canGenerateAtHeight(final int y) {
        return y >= this.height.min && y <= this.height.max;
    }

    private boolean testNoise(final int x, final int y, final int z, final int chunkX, final int chunkZ) {
        int actualX = (chunkX * 16) + x;
        int actualZ = (chunkZ * 16) + z;
        return testNoise(actualX, y, actualZ);
    }

    private boolean testNoise(final int x, final int y, final int z) {
        return this.noise.getBoolean(x, y, z);
    }
}
