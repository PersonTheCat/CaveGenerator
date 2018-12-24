package com.personthecat.cavegenerator.world;

import com.personthecat.cavegenerator.util.NoiseSettings;
import com.personthecat.cavegenerator.util.SimplexNoiseGenerator3D;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import java.util.Optional;
import static com.personthecat.cavegenerator.util.CommonMethods.*;

public class CaveBlocks {
    /** Mandatory fields to be filled by the constructor. */
    private final double chance; // Percent spawn chance.
    private final IBlockState fillBlock; // The block to place instead of air.
    private final int minHeight, maxHeight; // Height bounds.

    /** Null-safe, optional noise settings. I'm not dealing with NPEs. */
    private Optional<SimplexNoiseGenerator3D> noise = Optional.empty(); // Noise generator.
    private Optional<NoiseSettings> noiseSettings = Optional.empty(); // Noise generator settings.

    /** The default noise values for CaveBlocks with noise. */
    public static final NoiseSettings DEFAULT_NOISE = new NoiseSettings(0.10f, 50.00f, 1.00f, 1.00f);

    public CaveBlocks(double chance, IBlockState fillBlock, int maxHeight, int minHeight) {
        this.chance = chance;
        this.fillBlock = fillBlock;
        this.maxHeight = maxHeight;
        this.minHeight = minHeight;
    }

    /** Sets up a noise generator to use for placement. */
    public void setSpawnInPatches(NoiseSettings settings) {
        // The noise for this generator will be unique to the block ID.
        this.noise = Optional.of(new SimplexNoiseGenerator3D(Block.getStateId(fillBlock)));
        this.noiseSettings = Optional.of(settings);
    }

    public boolean spawnInPatches() {
        return noise.isPresent();
    }

    public float getPatchThreshold() {
        return noiseSettings.orElseThrow(() ->
            runEx("Tried to get the patch threshold from a CaveBlocks object with no noise settings.")
        ).getSelectionThreshold();
    }

    public float getPatchFrequency() {
        return noiseSettings.orElseThrow(() ->
            runEx("Tried to get the frequency from a CaveBlocks object with no noise settings.")
        ).getFrequency();
    }

    public SimplexNoiseGenerator3D getNoise() {
        return noise.orElseThrow(() ->
            runEx("Error: Tried to get the noise generator from a CaveBlocks object with none setup.")
        );
    }

    public double getChance() {
        return chance;
    }

    public IBlockState getFillBlock() {
        return fillBlock;
    }

    public int getMinHeight() {
        return minHeight;
    }

    public int getMaxHeight() {
        return maxHeight;
    }

    public boolean canGenerateAtHeight(final int y) {
        return y >= minHeight && y <= maxHeight;
    }

    /**
     * Returns true if the replacement doesn't have noise or
     * if its noise at the given coords meets the threshold.
     */
    public boolean testNoise(int chunkX, int chunkZ, int x, int y, int z) {
        int actualX = (chunkX * 16) + x;
        int actualZ = (chunkZ * 16) + z;
        return testNoise(actualX, y, actualZ);
    }

    /** Variant of testNoise() that uses absolute coordinates. */
    public boolean testNoise(int x, int y, int z) {
        if (spawnInPatches()) {
            double noise = getNoise().getFractalNoise(x, y, z, 1, getPatchFrequency(), 1);
            return noise > getPatchThreshold();
        }
        return true;
    }
}