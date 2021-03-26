package com.personthecat.cavegenerator.model;

import com.personthecat.cavegenerator.data.CaveBlockSettings;
import com.personthecat.cavegenerator.noise.DummyGenerator;
import fastnoise.FastNoise;
import net.minecraft.world.World;

public class ConfiguredCaveBlock {

    public final CaveBlockSettings cfg;
    public final FastNoise noise;

    public ConfiguredCaveBlock(CaveBlockSettings cfg, World world) {
        this.cfg = cfg;
        this.noise = cfg.noise.map(n -> n.getGenerator(world)).orElse(new DummyGenerator(0L));
    }

    public boolean canGenerate(int x, int y, int z, int chunkX, int chunkZ) {
        return canGenerateAtHeight(y) && testNoise(x, y, z, chunkX, chunkZ);
    }

    private boolean canGenerateAtHeight(final int y) {
        return y >= cfg.height.min && y <= cfg.height.max;
    }

    /**
     * Returns true if the replacement doesn't have noise or
     * if its noise at the given coords meets the threshold.
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

}
