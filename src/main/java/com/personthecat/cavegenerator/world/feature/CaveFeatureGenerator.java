package com.personthecat.cavegenerator.world.feature;

import com.personthecat.cavegenerator.CaveInit;
import com.personthecat.cavegenerator.Main;
import com.personthecat.cavegenerator.world.CaveGenerator;
import com.personthecat.cavegenerator.world.HeightMapLocator;
import fastnoise.FastNoise;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraftforge.fml.common.IWorldGenerator;

import java.util.Map;
import java.util.Random;

import static com.personthecat.cavegenerator.util.CommonMethods.*;

public class CaveFeatureGenerator implements IWorldGenerator {
    @Override
    public void generate(Random rand, int chunkX, int chunkZ, World world, IChunkGenerator chunkGen, IChunkProvider chunkProv) {
        // Once again, there is no way to avoid retrieving this statically.
        final Map<String, CaveGenerator> generators = Main.instance.generators;
        final int dimension = world.provider.getDimension();

        if (CaveInit.anyGeneratorEnabled(generators, dimension)) {
            final Chunk chunk = world.getChunkFromChunkCoords(chunkX, chunkZ);
            final int[][] heightMap = HeightMapLocator.getHeightFromChunk(world, chunk);

            for (CaveGenerator generator : generators.values()) {
                if (generator.canGenerate(dimension)) { // Do biome test later on.
                    generatePillars(generator, rand, chunkX, chunkZ, world);
                    generateStalactites(heightMap, generator, rand, chunkX, chunkZ, world);
                    // generateStructures(heightMap, generator, rand, chunkX, chunkZ, world);
                }
            }
        }
    }

    /**
     * Spawns a series of giant pillars in this chunk, according to the settings
     * contained in `gen`.
     */
    private static void generatePillars(CaveGenerator gen, Random rand, int chunkX, int chunkZ, World world) {
        for (GiantPillar pillar : gen.settings.decorators.pillars) {
            for (int i = 0; i < rand.nextInt(pillar.getFrequency() + 1); i++) {
                final int minHeight = pillar.getMinHeight();
                final int maxHeight = pillar.getMaxHeight();
                // Avoid pillars spawning right next to each other.
                final int x = ((rand.nextInt(6) * 2) + 2) + (chunkX * 16); // 2 to 14
                final int z = ((rand.nextInt(6) * 2) + 1) + (chunkZ * 16); // 1 to 13
                final int y = rand.nextInt(maxHeight - minHeight) + minHeight;

                final int opening = findOpeningFromAbove(world, x, y, z, minHeight);
                if (opening > 0) {
                    pillar.generate(world, rand, new BlockPos(x, opening, z));
                }
            }
        }
    }

    /** Runs the stalactite generator for each stalactite in `gen`. */
    private static void generateStalactites(int[][] heightMap, CaveGenerator gen, Random rand, int chunkX, int chunkZ, World world) {
        for (LargeStalactite st : gen.settings.decorators.stalactites) {
            generateStalactite(heightMap, gen, st, rand, chunkX, chunkZ, world);
        }
    }

    /** Generates a stalactite in the specified bounds. */
    private static void generateStalactite(int[][] heightMap, CaveGenerator gen, LargeStalactite st, Random rand, int chunkX, int chunkZ, World world) {
        final FastNoise noise = st.getNoise(rand.nextInt());
        final Random localRand = new Random(rand.nextInt());
        final int distance = stalactiteResolution(st.getChance());
        final int actualX = chunkX * 16;
        final int actualZ = chunkZ * 16;

        // Each iteration increments by `distance`. This changes the frequency
        // with which `noise` is calculated, theoretically impacting performance.
        // Lower frequencies do not require as high a resolution, as this
        // difference would typically not be visible.
        for (int x = actualX; x < actualX + 16; x = x + distance) {
            for (int z = actualZ; z < actualZ + 16; z = z + distance) {
                final Biome biome = world.getBiome(new BlockPos(x, 0, z));
                if (gen.canGenerate(biome) && canSpawnStalactite(st, noise, x, z)) {
                    handleStalactiteRegion(heightMap, st, localRand, x, z, distance, world);
                }
            }
        }
    }

    /** Attempts to spawn a stalactite at every coordinate pair in this region. */
    private static void handleStalactiteRegion(int[][] heightMap, LargeStalactite st, Random rand, int x, int z, int distance, World world) {
        for (int l = x; l < x + distance; l++) {
            for (int d = z; d < z + distance; d++) {
                // Check this earlier -> do less when it fails.
                if (rand.nextDouble() * 100 >= st.getChance()) {
                    continue;
                }
                final int maxHeight = getMin(heightMap[l & 15][d & 15], st.getMaxHeight());
                final int startHeight = rand.nextInt(maxHeight - st.getMinHeight()) + st.getMinHeight();
                // Stalactite -> go up and find a surface.
                // Stalagmite -> go down and find a surface.
                final int y = st.getType() == LargeStalactite.Type.STALACTITE ?
                    findOpeningFromBelow(world, l, startHeight, d, maxHeight):
                    findOpeningFromAbove(world, l, startHeight, d, maxHeight);
                trySpawnStalactite(st, rand, l, y, d, world);
            }
        }
    }

    /**
     * Attempts to spawn a single stalactite at this exact location. Fails
     * when y < 0 || blocks don't match || randomly, according to st#getChance.
     */
    private static void trySpawnStalactite(LargeStalactite st, Random rand, int x, int y, int z, World world) {
        if (y > 0) {
            final BlockPos pos = new BlockPos(x, y, z);
            if (matchSources(st.getMatchers(), world, pos)) {
                st.generate(world, rand, pos);
            }
        }
    }

    /**
     * Locates the first cave opening from above within the specified range.
     * Returns -1 when no opening is found.
     */
    private static int findOpeningFromAbove(World world, int x, int y, int z, int minY) {
        boolean previouslySolid = isSolid(world, x, y, z);
        for (int h = y; h > minY; h--) {
            final boolean currentlySolid = isSolid(world, x, h, z);
            if (previouslySolid && !currentlySolid) {
                return h;
            }
            previouslySolid = currentlySolid;
        }
        return -1;
    }

    /**
     * Locates the first cave opening from below within the specified range.
     * Returns -1 when no opening is found.
     */
    private static int findOpeningFromBelow(World world, int x, int y, int z, int maxY) {
        boolean previouslySolid = isSolid(world, x, y, z);
        for (int h = y; h < maxY; h++) {
            final boolean currentlySolid = isSolid(world, x, h, z);
            if (!previouslySolid && currentlySolid) {
                return h;
            }
            previouslySolid = currentlySolid;
        }
        return -1;
    }

    /**
     * Determines whether the block at the input location should spawn,
     * according to an array of matcher blocks.
     */
    private static boolean matchSources(IBlockState[] matchers, World world, BlockPos pos) {
        // No matchers -> always spawn.
        if (matchers.length == 0) {
            return true;
        }
        final IBlockState match = world.getBlockState(pos);
        for (IBlockState matcher : matchers) {
            if (match.equals(matcher)) {
                return true;
            }
        }
        return false;
    }

    private static boolean canSpawnStalactite(LargeStalactite st, FastNoise noise, int x, int z) {
        return !st.spawnInPatches() || noise.GetAdjustedNoise(x, z) > st.getThreshold();
    }

    /**
     * Determines the number of blocks between noise calculations.
     * Higher frequency -> higher resolution -> lower distance.
     */
    private static int stalactiteResolution(double fromChance) {
        return fromChance < 25.0 ? 16 : // 00 to 25 -> 1x / chunk
               fromChance < 35.0 ? 8 :  // 25 to 35 -> 4x / chunk
               fromChance < 55.0 ? 4 :  // 35 to 55 -> 16x / chunk
               fromChance < 75.0 ? 2 :  // 55 to 75 -> 64x / chunk
               1;                       // 75 to 100 -> 256x / chunk
    }

    /** Determines whether the IBlockState at the input coordinates is an opaque cube. */
    private static boolean isSolid(World world, int x, int y, int z) {
        return world.getBlockState(new BlockPos(x, y, z)).isOpaqueCube();
    }
}