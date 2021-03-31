package com.personthecat.cavegenerator.world.generator;

import com.personthecat.cavegenerator.config.ConfigFile;
import com.personthecat.cavegenerator.data.ConditionSettings;
import com.personthecat.cavegenerator.data.DecoratorSettings;
import com.personthecat.cavegenerator.model.ConfiguredCaveBlock;
import com.personthecat.cavegenerator.model.PrimerData;
import com.personthecat.cavegenerator.world.BiomeSearch;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.ChunkPrimer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

public abstract class MapGenerator extends WorldCarver {

    private static final IBlockState BLK_WATER = Blocks.WATER.getDefaultState();

    /** The vertical distance to the nearest water source block that can be ignored. */
    private static final int WATER_WIGGLE_ROOM = 7;

    protected final List<BlockPos> invalidBiomes = new ArrayList<>(BiomeSearch.size());
    protected final Random rand = new Random();

    public MapGenerator(ConditionSettings conditions, DecoratorSettings decorators, World world) {
        super(conditions, decorators, world);
    }

    @Override
    public final void generate(PrimerContext ctx) {
        if (conditions.dimensions.test(ctx.world.provider.getDimension())) {
            if (conditions.hasBiomes) {
                if (anyMatches(ctx.biomes, conditions.biomes)) {
                    fillInvalidBiomes(ctx.biomes);
                    generateChecked(ctx);
                    invalidBiomes.clear();
                }
            } else {
                generateChecked(ctx);
            }
        }
    }

    private static boolean anyMatches(BiomeSearch biomes, Predicate<Biome> predicate) {
        for (Biome b : biomes.current.get()) {
            if (predicate.test(b)) {
               return true;
            }
        }
        return false;
    }

    private void fillInvalidBiomes(BiomeSearch biomes) {
        for (BiomeSearch.Data biome : biomes.surrounding.get()) {
            if (!conditions.biomes.test(biome.biome)) {
                invalidBiomes.add(new BlockPos(biome.centerX, 0, biome.centerZ));
            }
        }
    }

    @Override
    protected final void generateChecked(PrimerContext ctx) {
        final int range = ConfigFile.mapRange;
        this.rand.setSeed(ctx.world.getSeed());
        final long xMask = this.rand.nextLong();
        final long zMask = this.rand.nextLong();

        for (int destX = ctx.chunkX - range; destX <= ctx.chunkX + range; destX++) {
            for (int destZ = ctx.chunkZ - range; destZ <= ctx.chunkZ + range; destZ++) {
                long xHash = (long) destX * xMask;
                long zHash = (long) destZ * zMask;
                this.rand.setSeed(xHash ^ zHash ^ ctx.world.getSeed());
                this.mapGenerate(new MapGenerationContext(ctx, rand, destX, destZ));
            }
        }
    }

    protected abstract void mapGenerate(MapGenerationContext ctx);

    protected double getNearestBorder(int x, int z) {
        double shortestDistance = Double.MAX_VALUE;

        for (BlockPos invalid : invalidBiomes) {
            final double sum = Math.pow(x - invalid.getX(), 2) + Math.pow(z - invalid.getZ(), 2);
            final double distance = Math.sqrt(sum);

            shortestDistance = Math.min(distance, shortestDistance);
        }
        return shortestDistance;
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

    protected static class MapGenerationContext {
        protected final int[][] heightmap;
        protected final World world;
        protected final Random rand;
        protected final int destChunkX;
        protected final int destChunkZ;
        protected final int chunkX;
        protected final int chunkZ;
        protected final int offsetX;
        protected final int offsetZ;
        protected final ChunkPrimer primer;

        private MapGenerationContext(PrimerContext ctx, Random rand, int destChunkX, int destChunkZ) {
            this.heightmap = ctx.heightmap;
            this.world = ctx.world;
            this.rand = rand;
            this.destChunkX = destChunkX;
            this.destChunkZ = destChunkZ;
            this.chunkX = ctx.chunkX;
            this.chunkZ = ctx.chunkZ;
            this.offsetX = ctx.offsetX;
            this.offsetZ = ctx.offsetZ;
            this.primer = ctx.primer;
        }
    }
}
