package com.personthecat.cavegenerator.world;

import com.personthecat.cavegenerator.util.NoiseSettings3D;
import com.personthecat.cavegenerator.util.SimplexNoise3D;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import org.hjson.JsonObject;

import java.util.Optional;

import static com.personthecat.cavegenerator.util.CommonMethods.*;
import static com.personthecat.cavegenerator.util.HjsonTools.*;

/** Contains all of the data needed for spawning alternative blocks in caves. */
public class CaveBlock {
    /** Percent spawn chance. */
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
    private final Optional<SimplexNoise3D> noise;
    private final Optional<NoiseSettings3D> settings;

    /** The default noise values for CaveBlocks with noise. */
    public static final NoiseSettings3D DEFAULT_NOISE =
        new NoiseSettings3D(0.02f, 0.10f, 1.00f, 1);
    /** An instance of the vanilla lava CaveBlocks that exists by default in all presets. */
    public static final CaveBlock VANILLA_LAVA =
        new CaveBlock(Blocks.LAVA.getDefaultState(),100.0, 0, 10, empty());

    public CaveBlock(IBlockState fillBlock, JsonObject caveBlock) {
        this(
            fillBlock,
            getFloatOr(caveBlock, "chance", 100.0f),
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
        this.noise = setupNoiseGenerator();
    }

    /** Determines whether to use noise based on the presence of noise settings. */
    private Optional<SimplexNoise3D> setupNoiseGenerator() {
        if (settings.isPresent()) {
            // The noise for this generator will be unique to the block ID.
            return full(new SimplexNoise3D(Block.getStateId(fillBlock)));
        }
        return empty();
    }

    public boolean spawnInPatches() {
        return noise.isPresent();
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
        // To-do: handle testing noise values here, as well.
        return canGenerateAtHeight(y);
    }

    public boolean canGenerateAtHeight(final int y) {
        return y >= minHeight && y <= maxHeight;
    }

//    /**
//     * Returns true if the replacement doesn't have noise or
//     * if its noise at the given coords meets the threshold.
//     */
//    public boolean testNoise(int chunkX, int chunkZ, int x, int y, int z) {
//        int actualX = (chunkX * 16) + x;
//        int actualZ = (chunkZ * 16) + z;
//        return testNoise(actualX, y, actualZ);
//    }
//
//    /** Variant of testNoise() that uses absolute coordinates. */
//    public boolean testNoise(int x, int y, int z) {
//        if (spawnInPatches()) {
//            double noise = getNoise().getFractalNoise(x, y, z, 1, getPatchFrequency(), 1);
//            return noise > getPatchThreshold();
//        }
//        return true;
//    }
}