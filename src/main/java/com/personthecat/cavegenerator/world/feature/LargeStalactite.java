package com.personthecat.cavegenerator.world.feature;

import fastnoise.FastNoise;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenerator;

import com.personthecat.cavegenerator.util.NoiseSettings2D;
import org.hjson.JsonObject;

import java.util.Optional;
import java.util.Random;

import static com.personthecat.cavegenerator.util.HjsonTools.*;
import static com.personthecat.cavegenerator.util.CommonMethods.*;

public class LargeStalactite extends WorldGenerator {
    /** Required fields. Must be supplied by the constructor. */
    private final boolean wide;
    private final double chance;
    private final IBlockState state;
    private final int maxLength, minHeight, maxHeight;
    private final Type type;
    private final IBlockState[] matchers;
    private final Optional<NoiseSettings2D> settings;

    /** The default noise settings to be optionally used for stalactites. */
    public static final NoiseSettings2D DEFAULT_NOISE =
        new NoiseSettings2D(0.025f, 0.7125f, -1, 1);

    /** From Json. */
    public LargeStalactite(Type type, JsonObject stalactite) {
        this(
            getGuranteedState(stalactite, "LargeStalactite"),
            type,
            getBoolOr(stalactite, "wide", true),
            getFloatOr(stalactite, "chance", 0.167f),
            getIntOr(stalactite, "maxLength", 3),
            getIntOr(stalactite, "minHeight", 11),
            getIntOr(stalactite, "maxHeight", 55),
            getBlocksOr(stalactite, "matchers" /* No defaults */),
            getObject(stalactite, "noise2D").map(o -> toNoiseSettings(o, DEFAULT_NOISE))
        );
    }

    public LargeStalactite(
        IBlockState state,
        Type type,
        boolean wide,
        double chance,
        int maxLength,
        int minHeight,
        int maxHeight,
        IBlockState[] matchers,
        Optional<NoiseSettings2D> settings
    ) {
        this.state = state;
        this.type = type;
        this.wide = wide;
        this.chance = chance;
        this.maxLength = maxLength;
        this.minHeight = minHeight;
        this.maxHeight = maxHeight;
        this.matchers = matchers;
        this.settings = settings;
    }

    @Override
    public boolean generate(World world, Random rand, BlockPos pos) {
        if (wide) {
            int length = place(world, rand, pos, maxLength, 4);
            placeSides(world, rand, pos, length * 2 / 3, length + 1);
            placeCorners(world, rand, pos, length / 4, 3);
        } else { // Just place the single column and stop.
            place(world, rand, pos, maxLength, 3);
        }
        return true;
    }

    private int place(World world, Random rand, BlockPos start, int length, int stopChance) {
        BlockPos pos = start;
        int i = 1; // Skip the initial position. It's the surface.
        for (; i < length; i++) {
            // Determine whether to go up or down.
            pos = type.equals(Type.STALACTITE) ? pos.down() : pos.up();
            // Stop randomly / when the current block is solid.
            if (world.getBlockState(pos).isOpaqueCube() || rand.nextInt(stopChance) == 0) {
                break;
            } // Set the new state.
            world.setBlockState(pos, state, 16);
        }
        return i; // Return the actual length.
    }

    private void placeSides(World world, Random rand, BlockPos pos, int length, int stopChance) {
        for (BlockPos cardinal : sidePositions(pos)) {
            findPlace(world, rand, cardinal, length, stopChance);
        }
    }

    private void placeCorners(World world, Random rand, BlockPos pos, int length, int stopChance) {
        for (BlockPos ordinal : cornerPositions(pos)) {
            findPlace(world, rand, ordinal, length, stopChance);
        }
    }

    private void findPlace(World world, Random rand, BlockPos pos, int length, int stopChance) {
        for (int i = 0; i < 3; i++) {
            if (world.getBlockState(pos).isOpaqueCube()) {
                place(world, rand, pos, length, stopChance);
                return;
            } // Go in the opposite direction and find a surface.
            pos = type.equals(Type.STALACTITE) ? pos.up() : pos.down();
        }
    }

    private static BlockPos[] sidePositions(BlockPos pos) {
        final int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        return new BlockPos[] {
            new BlockPos(x, y, z - 1), // North
            new BlockPos(x, y, z + 1), // South
            new BlockPos(x + 1, y, z), // East
            new BlockPos(x - 1, y, z)  // West
        };
    }

    private static BlockPos[] cornerPositions(BlockPos pos) {
        final int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        return new BlockPos[] {
            new BlockPos(x + 1, y, z - 1), // Northeast
            new BlockPos(x + 1, y, z + 1), // Southeast
            new BlockPos(x - 1, y, z + 1), // Southwest
            new BlockPos(x - 1, y, z - 1)  // Northwest
        };
    }

    public Optional<NoiseSettings2D> getSettings() {
        return settings;
    }

    public boolean spawnInPatches() {
        return settings.isPresent();
    }

    public FastNoise getNoise(int seed) {
        return settings
            .orElse(DEFAULT_NOISE)
            .getGenerator(seed);
    }

    public IBlockState getState() {
        return state;
    }

    /**
     * The percent chance that this spawner should
     * run in any given chunk.
     */
    public double getChance() {
        return chance;
    }

    public int getMinHeight() {
        return minHeight;
    }

    public int getMaxHeight() {
        return maxHeight;
    }

    public IBlockState[] getMatchers() {
        return matchers;
    }

    /**
     * Whether this structure should spawn upward or
     * downward, i.e. stalagmite or stalactite.
     */
    public Type getType() {
        return type;
    }

    public enum Type {
        STALAGMITE,
        STALACTITE
    }
}