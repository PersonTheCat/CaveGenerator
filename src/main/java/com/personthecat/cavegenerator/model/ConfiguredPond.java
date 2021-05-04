package com.personthecat.cavegenerator.model;

import com.personthecat.cavegenerator.data.PondSettings;
import com.personthecat.cavegenerator.noise.DummyGenerator;
import fastnoise.FastNoise;
import net.minecraft.block.state.IBlockState;
import net.minecraft.world.World;
import java.util.Random;

public class ConfiguredPond {

    public final PondSettings cfg;
    public final FastNoise noise;

    public ConfiguredPond(PondSettings cfg, World world) {
        this.cfg = cfg;
        this.noise = cfg.noise.map(n -> n.getGenerator(world)).orElse(new DummyGenerator(0L));
    }

    public boolean canGenerate(Random rand, IBlockState state, int x, int y, int z, int chunkX, int chunkZ) {
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
    private boolean testNoise(int x, int y, int z, int chunkX, int chunkZ) {
        int actualX = (chunkX * 16) + x;
        int actualZ = (chunkZ * 16) + z;
        return testNoise(actualX, y, actualZ);
    }

    /** Variant of testNoise() that uses absolute coordinates. */
    private boolean testNoise(int x, int y, int z) {
        return noise.GetBoolean(x, y, z);
    }

    public boolean matchesBlock(IBlockState state) {
        for (IBlockState matcher : cfg.matchers) {
            if (matcher.equals(state)) {
                return true;
            }
        }
        return false;
    }
}
