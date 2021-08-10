package personthecat.cavegenerator.model;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import personthecat.cavegenerator.data.ShellSettings;
import personthecat.cavegenerator.noise.DummyGenerator;
import personthecat.fastnoise.FastNoise;

import java.util.Collections;
import java.util.List;

import static personthecat.catlib.util.Shorthand.map;

public class ConfiguredShell {

    public static final ConfiguredShell EMPTY_SHELL = new ConfiguredShell();

    public final ShellSettings cfg;
    public final double radius;
    public final float noiseThreshold;
    public final List<Decorator> decorators;

    public ConfiguredShell(final ShellSettings cfg, final Level level) {
        this.cfg = cfg;
        this.radius = cfg.radius;
        this.noiseThreshold = cfg.noiseThreshold.orElse(((float) cfg.radius + 0.0001F) / 10.0F);
        this.decorators = map(cfg.decorators, d -> new Decorator(d, level));
    }

    private ConfiguredShell() {
        this.cfg = ShellSettings.builder().build();
        this.radius = 0.0;
        this.noiseThreshold = 0.0F;
        this.decorators = Collections.emptyList();
    }

    public static class Decorator {

        public final ShellSettings.Decorator cfg;
        public final FastNoise noise;

        private Decorator(final ShellSettings.Decorator cfg, final Level level) {
            this.cfg = cfg;
            this.noise = cfg.noise.map(n -> n.getGenerator(level)).orElse(new DummyGenerator(0F));
        }

        public boolean matches(final BlockState state) {
            if (cfg.matchers.isEmpty()) {
                return state.getMaterial().isSolid();
            }
            return cfg.matchers.contains(state);
        }

        /**
         * Returns true if the replacement doesn't have noise or
         * if its noise at the given coords meets the threshold.
         */
        public boolean testNoise(final int x, final int y, final int z, final int chunkX, final int chunkZ) {
            int actualX = (chunkX * 16) + x;
            int actualZ = (chunkZ * 16) + z;
            return testNoise(actualX, y, actualZ);
        }

        /** Variant of testNoise() that uses absolute coordinates. */
        private boolean testNoise(final int x, final int y, final int z) {
            return noise.getBoolean(x, y, z);
        }
    }
}
