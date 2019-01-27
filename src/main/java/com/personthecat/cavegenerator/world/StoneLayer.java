package com.personthecat.cavegenerator.world;

import com.personthecat.cavegenerator.util.NoiseSettings2D;
import fastnoise.FastNoise;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import org.hjson.JsonObject;

import static com.personthecat.cavegenerator.util.HjsonTools.*;
import static com.personthecat.cavegenerator.util.CommonMethods.*;

/** Data used for spawning giant layers of stone through ChunkPrimer. */
public class StoneLayer {
    /** Mandatory fields to be initialized by the constructor. */
    private final IBlockState state;
    private final int maxHeight;
    private final NoiseSettings2D settings;
    private final FastNoise noise;

    /** The default noise values used by this object. */
    public static final NoiseSettings2D DEFAULT_NOISE =
        new NoiseSettings2D(0.015f, 0.5f, -7, 7);

    /** Primary constructor. */
    public StoneLayer(IBlockState state, int maxHeight, NoiseSettings2D settings) {
        this.state = state;
        this.maxHeight = maxHeight;
        this.settings = settings;
        this.noise = setupNoise(settings);
    }

    /** An overloaded constructor which applies the default noise values. */
    public StoneLayer(IBlockState state, int maxHeight) {
        this(state, maxHeight, DEFAULT_NOISE);
    }

    /** From Json. */
    public StoneLayer(JsonObject layer) {
        this(
            getGuranteedState(layer, "StoneLayer"),
            getInt(layer, "maxHeight")
                .orElseThrow(() -> runEx("At least one StoneLayer does not contain a maxHeight.")),
            getNoiseSettingsOr(layer, "noise2D", DEFAULT_NOISE)
        );
    }

    private FastNoise setupNoise(NoiseSettings2D settings) {
        return settings.getGenerator(Block.getStateId(state));
    }

    public IBlockState getState() {
        return state;
    }

    public int getMaxHeight() {
        return maxHeight;
    }

    public NoiseSettings2D getSettings() {
        return settings;
    }

    public FastNoise getNoise() {
        return noise;
    }
}