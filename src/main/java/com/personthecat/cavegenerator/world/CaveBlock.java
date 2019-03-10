package com.personthecat.cavegenerator.world;

import com.personthecat.cavegenerator.util.NoiseSettings3D;
import fastnoise.FastNoise;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import org.hjson.JsonObject;

import java.util.Optional;

import static com.personthecat.cavegenerator.util.CommonMethods.*;
import static com.personthecat.cavegenerator.util.HjsonTools.*;

/** Contains all of the data needed for spawning alternative blocks in caves. */
public class CaveBlock {
    /** 0-1 spawn chance. */
    private final double chance;
    /** The block to place instead of air. */
    private final IBlockState fillBlock;
    /** Height bounds. */
    private final int minHeight, maxHeight;

    /**
     * Null-safe, optional noise settings. In the future, I may decide
     * to convert these into standard, nullable types in an effort to
     * improve performance (if necessary). However, in the meantime,
     * I'm not dealing with NPEs.
     */
    private final Optional<FastNoise> noise;
    private final Optional<NoiseSettings3D> settings;

    /** The default noise values for CaveBlocks with noise. */
    public static final NoiseSettings3D DEFAULT_NOISE =
        new NoiseSettings3D(0.02f, 0.10f, 1.00f, 1);
    /** An instance of the vanilla lava CaveBlocks that exists by default in all presets. */
    public static final CaveBlock VANILLA_LAVA =
        new CaveBlock(Blocks.LAVA.getDefaultState(),1.0, 0, 10, empty());

    public CaveBlock(IBlockState fillBlock, JsonObject caveBlock) {
        this(
            fillBlock,
            getFloatOr(caveBlock, "chance", 1.0f),
            getIntOr(caveBlock, "minHeight", 0),
            getIntOr(caveBlock, "maxHeight", 50),
            getObject(caveBlock, "noise3D").map(o -> toNoiseSettings(o, DEFAULT_NOISE))
        );
    }

    /** Primary constructor. */
    public CaveBlock(
        IBlockState fillBlock,
        double chance,
        int minHeight,
        int maxHeight,
        Optional<NoiseSettings3D> settings
    ) {
        this.fillBlock = fillBlock;
        this.chance = chance;
        this.minHeight = minHeight;
        this.maxHeight = maxHeight;
        this.settings = settings;
        this.noise = settings.map(s ->
            s.getGenerator(Block.getStateId(fillBlock)));
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

    public boolean canGenerate(int x, int y, int z, int chunkX, int chunkZ) {
        return canGenerateAtHeight(y) && testNoise(x, y, z, chunkX, chunkZ);
    }

    public boolean canGenerateAtHeight(final int y) {
        return y >= minHeight && y <= maxHeight;
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
        // Calling Optional#get because `settings` will always be present when `noise` is present.
        return noise.map(n -> n.GetAdjustedNoise(x, y, z) < settings.get().getBooleanThreshold())
            .orElse(true);
    }
}