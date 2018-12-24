package com.personthecat.cavegenerator.world.feature;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenerator;

import java.util.Random;

public class LargeStalactite extends WorldGenerator {
    /** Required fields. Must be supplied by the constructor. */
    private final double chance;
    private final IBlockState state;
    private final int maxLength, minHeight, maxHeight;
    private final Type type;

    /** Optional fields with default values. Can be set later. */
    private boolean spawnInPatches = false;
    private double patchThreshold = 0.15;
    private int patchSpacing = 40;
    private IBlockState[] matchers = new IBlockState[0];

    public LargeStalactite(int maxLength, double chance, IBlockState state, int minHeight, int maxHeight, Type type) {
        this.maxLength = maxLength;
        this.chance = chance;
        this.minHeight = minHeight;
        this.maxHeight = maxHeight;
        this.state = state;
        this.type = type;
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

    /**
     * Whether the generator should run according to a
     * perlin noise generator.
     */
    public void setSpawnInPatches() {
        this.spawnInPatches = true;
    }

    public boolean shouldSpawnInPatches() {
        return spawnInPatches;
    }

    /**
     * The threshold for determining whether the noise
     * at any given coordinate is sufficient.
     */
    public void setPatchThreshold(double threshold) {
        this.patchThreshold = threshold;
    }

    public double getPatchThreshold() {
        return patchThreshold;
    }

    /**
     * Technically, this is the frequency value used
     * in the algorithm that fractalizes the noise.
     * It is renamed here to be more evocative of
     * its actual function, in this case.
     */
    public void setPatchSpacing(int spacing) {
        this.patchSpacing = spacing;
    }

    public int getPatchSpacing() {
        return patchSpacing;
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

    /**
     * Any source blocks that can be matched for this
     * spawner to run. Will spawn anywhere, if none.
     */
    public void setMatchers(IBlockState[] matchers) {
        this.matchers = matchers;
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