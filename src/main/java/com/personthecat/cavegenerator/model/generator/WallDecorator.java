package com.personthecat.cavegenerator.model.generator;

import com.personthecat.cavegenerator.model.Direction;
import com.personthecat.cavegenerator.util.HjsonTools;
import com.personthecat.cavegenerator.model.NoiseSettings3D;
import fastnoise.FastNoise;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Value;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.world.chunk.ChunkPrimer;
import org.hjson.JsonObject;

import java.util.Arrays;
import java.util.Optional;
import java.util.Random;

import static com.personthecat.cavegenerator.util.CommonMethods.*;
import static com.personthecat.cavegenerator.util.HjsonTools.*;

@Value
@AllArgsConstructor
@Builder(toBuilder = true)
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class WallDecorator {

    /** The block to use for decorating walls. */
    IBlockState fillBlock;

    /** The 0-1 chance that any block will be placed. */
    @Default double chance = 1.0;

    /** Minimum height bounds. */
    @Default int minHeight = 10;

    /** Maximum height bounds. */
    @Default int maxHeight = 50;

    /** A list of directions to place blocks. */
    @Default Direction[] directions = { Direction.ALL };

    /** A list of blocks to check for. */
    @Default IBlockState[] matchers = { Blocks.STONE.getDefaultState() };

    /** Whether to place <b>on</b> the wall or <b>in</b> the wall. */
    @Default Preference preference = Preference.REPLACE_MATCH;

    /** Optional noise generator used determine valid placements. */
    @Default Optional<FastNoise> noise = empty();

    /** Optional noise generation settings used for placement. */
    @Default Optional<NoiseSettings3D> settings = empty();

    /** The default noise values for WallDecorators with noise. */
    public static final NoiseSettings3D DEFAULT_NOISE = NoiseSettings3D.builder()
        .frequency(0.02f)
        .scale(0.5f)
        .scaleY(1.0f)
        .octaves(1)
        .build();

    /** From Json. */
    public static WallDecorator from(IBlockState fillBlock, JsonObject wall) {
        final WallDecoratorBuilder builder = WallDecorator.builder()
            .fillBlock(fillBlock);

        getObject(wall, "noise3D").map(o -> toNoiseSettings(o, DEFAULT_NOISE)).ifPresent(s -> {
            builder.settings(full(s));
            builder.noise(full(s.getGenerator(Block.getStateId(fillBlock))));
        });
        getFloat(wall, "chance").ifPresent(builder::chance);
        getInt(wall, "minHeight").ifPresent(builder::minHeight);
        getInt(wall, "maxHeight").ifPresent(builder::maxHeight);
        getBlocks(wall, "matchers").ifPresent(builder::matchers);
        HjsonTools.getDirections(wall, "directions").ifPresent(builder::directions);
        HjsonTools.getPreference(wall, "preference").ifPresent(builder::preference);
        return builder.build();
    }

    public boolean spawnInPatches() {
        return noise.isPresent();
    }

    public boolean canGenerate(Random rand, IBlockState state, int x, int y, int z, int chunkX, int chunkZ) {
        return canGenerate(rand, x, y, z, chunkX, chunkZ) &&
            matchesBlock(state);
    }

    public boolean canGenerate(Random rand, int x, int y, int z, int chunkX, int chunkZ) {
        return y >= minHeight && y <= maxHeight && // Height bounds
            rand.nextDouble() <= chance && // Probability
            testNoise(x, y, z, chunkX, chunkZ); // Noise
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
        return noise.map(n -> n.GetBoolean(x, y, z))
            .orElse(true);
    }

    public boolean matchesBlock(IBlockState state) {
        for (IBlockState matcher : matchers) {
            if (matcher.equals(state)){
                return true;
            }
        }
        return false;
    }

    public boolean decidePlace(ChunkPrimer primer, int xO, int yO, int zO, int xD, int yD, int zD) {
        if (preference.equals(Preference.REPLACE_ORIGINAL)) {
            primer.setBlockState(xO, yO, zO, fillBlock);
            return true;
        } else {
            primer.setBlockState(xD, yD, zD, fillBlock);
            return false;
        }
    }

    /**
     * Indicates whether to place blocks inside of or on top of a wall. As much as I
     * would love to rename these, I do think it's a little too late.
     */
    public enum Preference {
        REPLACE_ORIGINAL,
        REPLACE_MATCH;

        public static Preference from(final String s) {
            Optional<Preference> pref = find(values(), (v) -> v.toString().equalsIgnoreCase(s));
            return pref.orElseThrow(() -> {
                final String o = Arrays.toString(values());
                return runExF("Error: Preference \"%s\" does not exist. The following are valid options:\n\n", s, o);
            });
        }
    }
}