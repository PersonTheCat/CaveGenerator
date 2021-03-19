package com.personthecat.cavegenerator.world.generator;

import com.personthecat.cavegenerator.data.ConditionSettings;
import com.personthecat.cavegenerator.data.DecoratorSettings;
import com.personthecat.cavegenerator.model.ConfiguredCaveBlock;
import com.personthecat.cavegenerator.model.PrimerData;
import com.personthecat.cavegenerator.data.CaveBlockSettings;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkPrimer;

import java.util.Random;

public abstract class SphereGenerator extends WorldCarver {

    private static final IBlockState BLK_WATER = Blocks.WATER.getDefaultState();

    /** The vertical distance to the nearest water source block that can be ignored. */
    private static final int WATER_WIGGLE_ROOM = 7;

    public SphereGenerator(ConditionSettings conditions, DecoratorSettings decorators, World world) {
        super(conditions, decorators, world);
    }

    protected void generateSphere(Random rand, PrimerData data, TunnelSectionInfo section) {
        // If we need to test this section for water -> is there water?
        if (!(shouldTestForWater(section.getLowestY(), section.getHighestY()) && testForWater(data.p, section))) {
            // Generate the actual sphere.
            replaceSection(rand, data, section);
            // We need to generate twice; once to create walls,
            // and once again to decorate those walls.
            if (hasLocalDecorators()) {
                // Decorate the sphere.
                decorateSection(rand, data, section);
            }
        }
    }

    /** Calculates the maximum distance for this tunnel, if needed. */
    protected int getDistance(Random rand, int input) {
        if (input <= 0) {
            return 112 - rand.nextInt(28);
        }
        return input;
    }

    /**
     * Returns whether a test should be run to determine whether water is
     * found and stop generating.
     */
    private boolean shouldTestForWater(int lowestY, int highestY) {
        for (ConfiguredCaveBlock block : decorators.caveBlocks) {
            if (block.cfg.states.contains(BLK_WATER)) {
                if (highestY <= block.cfg.height.max + WATER_WIGGLE_ROOM
                    && lowestY >= block.cfg.height.min - WATER_WIGGLE_ROOM)
                {
                    return false;
                }
            }
        }
        return true;
    }

    /** Determines whether any water exists in the current section. */
    private boolean testForWater(ChunkPrimer primer, TunnelSectionInfo section) {
        return section.test(pos ->
            primer.getBlockState(pos.getX(), pos.getY() + 1, pos.getZ()).equals(BLK_WATER)
        );
    }

    /** Replaces all blocks inside of this section. */
    private void replaceSection(Random rand, PrimerData data, TunnelSectionInfo section) {
        section.run((x, y, z) -> replaceBlock(rand, data.p, x, y, z, data.chunkX, data.chunkZ));
    }

    /** Decorates all blocks inside of this section. */
    private void decorateSection(Random rand, PrimerData data, TunnelSectionInfo section) {
        section.run((x, y, z) -> decorateBlock(rand, data.p, x, y, z, data.chunkX, data.chunkZ));
    }
}
