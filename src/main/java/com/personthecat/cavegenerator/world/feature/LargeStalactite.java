package com.personthecat.cavegenerator.world.feature;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenerator;

import com.personthecat.cavegenerator.util.NoiseSettings2D;
import org.hjson.JsonObject;

import java.util.Optional;
import java.util.Random;

import static com.personthecat.cavegenerator.util.HjsonTools.*;

public class LargeStalactite extends WorldGenerator {
    /** Required fields. Must be supplied by the constructor. */
    private final double chance;
    private final IBlockState state;
    private final int maxLength, minHeight, maxHeight;
    private final Type type;
    private final IBlockState[] matchers;
    private final Optional<NoiseSettings2D> settings;

    /** The default noise settings to be optionally used for stalactites. */
    public static final NoiseSettings2D DEFAULT_NOISE =
        new NoiseSettings2D(-0.7f, 40.0f, -1, 1);

    /** From Json. */
    public LargeStalactite(Type type, JsonObject stalactite) {
        this(
            getGuranteedState(stalactite, "LargeStalactite"),
            type,
            getFloatOr(stalactite, "chance", 16.7f),
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
        double chance,
        int maxLength,
        int minHeight,
        int maxHeight,
        IBlockState[] matchers,
        Optional<NoiseSettings2D> settings
    ) {
        this.state = state;
        this.type = type;
        this.chance = chance;
        this.maxLength = maxLength;
        this.minHeight = minHeight;
        this.maxHeight = maxHeight;
        this.matchers = matchers;
        this.settings = settings;
    }

    @Override
    public boolean generate(World world, Random localRand, BlockPos pos) {
        // Start by setting the initial position to the new state.
        world.setBlockState(pos, state, 16);

        for (int i = 1; i < maxLength; i++) {
            // Determine whether to go up or down.
            pos = type.equals(Type.STALACTITE) ? pos.down() : pos.up();

            // Stop randomly / when the current block is not solid.
            if (world.getBlockState(pos).isOpaqueCube() || localRand.nextInt(2) == 0) {
                break;
            }
            // Finally, set the new state.
            world.setBlockState(pos, state, 16);
        }

        return true;
    }

    public boolean spawnInPatches() {
        return settings.isPresent();
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