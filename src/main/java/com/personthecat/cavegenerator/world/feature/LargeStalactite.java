package com.personthecat.cavegenerator.world.feature;

import fastnoise.FastNoise;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.EqualsAndHashCode;
import lombok.Value;
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

@Value
@Builder
@AllArgsConstructor()
@EqualsAndHashCode(callSuper = false)
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class LargeStalactite extends WorldGenerator {

    /** The required state to make the body of this structure. */
    IBlockState state;

    /** Whether this structure should spawn upward or downward, i.e. stalagmite or stalactite. */
    Type type;

    /** Required fields. Must be supplied by the constructor. */
    @Default boolean wide = true;

    /** The 0-1 chance that this spawner should run in any given chunk. */
    @Default double chance = 0.167f;

    /** The maximum length to generate. */
    @Default int maxLength = 3;

    /** The minimum height bound. */
    @Default int minHeight = 11;

    /** The maximum height bound. */
    @Default int maxHeight = 55;

    /** Source blocks to check for before spawning. */
    @Default IBlockState[] matchers = {};

    /** Noise used to determine placement of this structure. */
    @Default Optional<NoiseSettings2D> settings = empty();

    /** The default noise settings to be optionally used for stalactites. */
    public static final NoiseSettings2D DEFAULT_NOISE = NoiseSettings2D.builder()
        .frequency(0.025f)
        .scale(0.7125f)
        .min(-1)
        .max(1)
        .build();

    /** From Json. */
    public static LargeStalactite from(Type type, JsonObject stalactite) {
        final Optional<NoiseSettings2D> noise = getObject(stalactite, "noise2D")
            .map(o -> toNoiseSettings(o, DEFAULT_NOISE));
        final LargeStalactiteBuilder builder = LargeStalactite.builder()
            .state(getGuaranteedState(stalactite, "LargeStalactite"))
            .type(type)
            .settings(noise);

        getBool(stalactite, "wide").ifPresent(builder::wide);
        getFloat(stalactite, "chance").ifPresent(builder::chance);
        getInt(stalactite, "maxLength").ifPresent(builder::maxLength);
        getInt(stalactite, "minHeight").ifPresent(builder::minHeight);
        getInt(stalactite, "maxHeight").ifPresent(builder::maxHeight);
        getBlocks(stalactite, "matchers").ifPresent(builder::matchers);
        return builder.build();
    }

    boolean spawnInPatches() {
        return settings.isPresent();
    }

    FastNoise getNoise(int seed) {
        return settings.orElse(DEFAULT_NOISE).getGenerator(seed);
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

    public enum Type {
        STALAGMITE,
        STALACTITE
    }
}