package com.personthecat.cavegenerator.world;

import net.minecraft.block.state.IBlockState;
import net.minecraft.world.gen.NoiseGeneratorSimplex;

import static com.personthecat.cavegenerator.util.CommonMethods.*;

import java.util.Optional;

/** Data used for spawning giant layers of stone through ChunkPrimer. */
public class StoneLayer {
    /** Mandatory fields to be initialized by the constructor. */
    private final IBlockState state;
    private final int maxHeight;
    private final int variance;

    /** A null-safe, late-init field containing the noise generator. */
    private Optional<NoiseGeneratorSimplex> noise = Optional.empty();

    public StoneLayer(IBlockState state, int maxHeight, int variance) {
        this.state = state;
        this.maxHeight = maxHeight;
        this.variance = variance;
    }

    public IBlockState getState() {
        return state;
    }

    public int getMaxHeight() {
        return maxHeight;
    }

    public int getVariance() {
        return variance;
    }

    public void setNoise(NoiseGeneratorSimplex noise) {
        this.noise = Optional.of(noise);
    }

    public NoiseGeneratorSimplex getNoise() {
        if (noise.isPresent()) {
            return noise.get();
        }
        throw runEx("Tried to retrieve a noise generator that was not yet initialized.");
    }
}