package com.personthecat.cavegenerator.world;

import com.personthecat.cavegenerator.util.Direction;
import com.personthecat.cavegenerator.util.NoiseSettings3D;
import fastnoise.FastNoise;
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

public class WallDecorator {
    /** Mandatory fields to be filled by the constructor. */
    private final double chance;
    private final IBlockState fillBlock;
    private final int minHeight, maxHeight;
    private final Direction[] directions;
    private final IBlockState[] matchers;
    private final Preference preference;

    /** Null-safe, optional noise settings. I'm not dealing with NPEs. */
    private final Optional<FastNoise> noise;
    private final Optional<NoiseSettings3D> settings;

    /** The default noise values for WallDecorators with noise. */
    public static final NoiseSettings3D DEFAULT_NOISE =
        new NoiseSettings3D(0.02f, 0.50f, 1.00f, 1);

    /** From Json. */
    public WallDecorator(IBlockState fillBlock, JsonObject wall) {
        this(
            fillBlock,
            getFloatOr(wall, "chance", 100.0f),
            getIntOr(wall, "minHeight", 10),
            getIntOr(wall, "maxHeight", 50),
            getDirectionsOr(wall, "directions", Direction.ALL),
            getBlocksOr(wall, "matchers", Blocks.STONE.getDefaultState()),
            getPreferenceOr(wall, "preference", Preference.REPLACE_MATCH),
            getObject(wall, "noise3D").map(o -> toNoiseSettings(o, DEFAULT_NOISE))
        );
    }

    public WallDecorator(
        IBlockState fillBlock,
        double chance,
        int minHeight,
        int maxHeight,
        Direction[] directions,
        IBlockState[] matchers,
        Preference preference,
        Optional<NoiseSettings3D> settings
    ) {
        this.fillBlock = fillBlock;
        this.chance = chance;
        this.minHeight = minHeight;
        this.maxHeight = maxHeight;
        this.directions = directions;
        this.matchers = matchers;
        this.preference = preference;
        this.settings = settings;
        this.noise = settings.map(s ->
            s.getNoise(Block.getStateId(fillBlock)));
    }

    public boolean spawnInPatches() {
        return noise.isPresent();
    }

    public Optional<NoiseSettings3D> getSettings() {
        return settings;
    }

    public Direction[] getDirections() {
        return directions;
    }

    public double getChance() {
        return chance;
    }

    public int getMinHeight() {
        return minHeight;
    }

    public int getMaxHeight() {
        return maxHeight;
    }

    public IBlockState getFillBlock() {
        return fillBlock;
    }

    public boolean canGenerate(Random rand, IBlockState state, int x, int y, int z, int chunkX, int chunkZ) {
        return canGenerate(rand, x, y, z, chunkX, chunkZ) &&
            matchesBlock(state);
    }

    public boolean canGenerate(Random rand, int x, int y, int z, int chunkX, int chunkZ) {
        return y >= minHeight && y <= maxHeight && // Height bounds
            rand.nextDouble() * 100 <= chance && // Probability
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
        return noise.map(n -> n.GetAdjustedNoise(x, y, z) < settings.get().getSelectionThreshold())
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