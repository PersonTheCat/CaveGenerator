package personthecat.cavegenerator.world.config;

import lombok.AllArgsConstructor;
import net.minecraft.world.level.block.state.BlockState;
import personthecat.catlib.data.Range;
import personthecat.fastnoise.FastNoise;

import java.util.List;
import java.util.Set;

@AllArgsConstructor
public class ShellConfig {
    public final float radius;
    public final int sphereResolution;
    public final float noiseThreshold;
    public final List<Decorator> decorators;

    @AllArgsConstructor
    public static class Decorator {
        public final List<BlockState> states;
        public final Set<BlockState> matchers;
        public final Range height;
        public final double integrity;
        public final FastNoise noise;

        public boolean matches(final BlockState state) {
            if (this.matchers.isEmpty()) {
                return state.getMaterial().isSolid();
            }
            return this.matchers.contains(state);
        }

        public boolean testNoise(final int x, final int y, final int z, final int chunkX, final int chunkZ) {
            int actualX = (chunkX * 16) + x;
            int actualZ = (chunkZ * 16) + z;
            return testNoise(actualX, y, actualZ);
        }

        private boolean testNoise(final int x, final int y, final int z) {
            return noise.getBoolean(x, y, z);
        }
    }
}
