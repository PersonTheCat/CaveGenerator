package com.personthecat.cavegenerator.world.generator;

import com.personthecat.cavegenerator.model.NoiseSettings3D;
import fastnoise.FastNoise;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import org.hjson.JsonObject;

import java.util.Optional;

import static com.personthecat.cavegenerator.util.CommonMethods.*;
import static com.personthecat.cavegenerator.util.HjsonTools.*;

/** Contains all of the data needed for spawning alternative blocks in caves. */
@Value
@RequiredArgsConstructor
@Builder(toBuilder = true)
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class CaveBlock {

    /** The block to place instead of air. */
    IBlockState fillBlock;

    /** 0-1 spawn chance. */
    @Default double chance = 1.0;

    /** Minimum height bound. */
    @Default int minHeight = 0;

    /** Maximum height bound. */
    @Default int maxHeight = 50;

    /** Noise Generator corresponding to this block. */
    @Default Optional<FastNoise> noise = empty();

    /** The default noise values for CaveBlocks with noise. */
    public static final NoiseSettings3D DEFAULT_NOISE = NoiseSettings3D.builder()
        .frequency(0.02f)
        .scale(0.1f)
        .scaleY(1.0f)
        .octaves(1)
        .build();

    /** An instance of the vanilla lava CaveBlocks that exists by default in all presets. */
    public static final CaveBlock VANILLA_LAVA = CaveBlock.builder()
        .fillBlock(Blocks.LAVA.getDefaultState())
        .chance(1.0f)
        .minHeight(0)
        .maxHeight(10)
        .build();

    public static CaveBlock from(IBlockState fillBlock, JsonObject caveBlock) {
        final Optional<FastNoise> noise = getObject(caveBlock, "noise3D")
            .map(o -> toNoiseSettings(o, DEFAULT_NOISE).getGenerator(Block.getStateId(fillBlock)));
        final CaveBlockBuilder builder = CaveBlock.builder()
            .fillBlock(fillBlock)
            .noise(noise);

        getFloat(caveBlock, "chance").ifPresent(builder::chance);
        getInt(caveBlock, "minHeight").ifPresent(builder::minHeight);
        getInt(caveBlock, "maxHeight").ifPresent(builder::maxHeight);
        return builder.build();
    }

    public boolean canGenerate(int x, int y, int z, int chunkX, int chunkZ) {
        return canGenerateAtHeight(y) && testNoise(x, y, z, chunkX, chunkZ);
    }

    private boolean canGenerateAtHeight(final int y) {
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
        return noise.map(n -> n.GetBoolean(x, y, z)).orElse(true);
    }
}