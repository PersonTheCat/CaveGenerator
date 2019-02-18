package com.personthecat.cavegenerator.world.feature;

import com.personthecat.cavegenerator.CaveInit;
import com.personthecat.cavegenerator.Main;
import com.personthecat.cavegenerator.util.Direction;
import com.personthecat.cavegenerator.world.CaveGenerator;
import com.personthecat.cavegenerator.world.HeightMapLocator;
import fastnoise.FastNoise;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraft.world.gen.structure.template.Template;
import net.minecraftforge.fml.common.IWorldGenerator;

import java.util.Map;
import java.util.Optional;
import java.util.Random;

import static com.personthecat.cavegenerator.util.CommonMethods.*;
import com.personthecat.cavegenerator.world.GeneratorSettings.StructureSettings;

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
                    generateStructures(heightMap, generator, rand, chunkX, chunkZ, world);
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

                final int opening = findFloor(world, x, y, z, minHeight);
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
                    findCeiling(world, l, startHeight, d, maxHeight):
                    findFloor(world, l, startHeight, d, maxHeight);
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

    private static void generateStructures(int[][] heightMap, CaveGenerator gen, Random rand, int chunkX, int chunkZ, World world) {
        for (StructureSettings settings : gen.settings.structures) {
            for (int i = 0; i < settings.frequency; i++) {
                if (rand.nextDouble() * 100 <= settings.chance) {
                    generateStructure(heightMap, settings, rand, chunkX, chunkZ, world);
                }
            }
        }
    }

    private static void generateStructure(int[][] heightMap, StructureSettings settings, Random rand, int chunkX, int chunkZ, World world) {
        // As always, there's really no good way to retrieve this non-statically.
        // Would need to write the game myself to work around that.
        Map<String, Template> structures = Main.instance.structures;
        Template structure = StructureSpawner.getTemplate(structures, settings.name, world);
        // Attempt to locate a suitable spawn position and then proceed.
        getSpawnPos(heightMap, settings, structure, rand, chunkX, chunkZ, world).ifPresent(pos -> {
            if (canGenerateStructure(settings, pos, world)) {
                if (settings.rotateRandomly) {
                    Rotation randRotation = Rotation.values()[rand.nextInt(3)];
                    settings.settings.setRotation(randRotation);
                }
                if (settings.debugSpawns) {
                    debug("Spawning {} at {}", settings.name, pos);
                }
                BlockPos centered = centerBySize(pos, structure.getSize());
                StructureSpawner.spawnStructure(structure, settings.settings, world, centered);
            }
        });
    }

    /** Attempts to determine a suitable spawn point in the current location. */
    public static Optional<BlockPos> getSpawnPos(int[][] heightMap, StructureSettings settings, Template structure, Random rand, int chunkX, int chunkZ, World world) {
        final BlockPos xz = randCoords(rand, structure.getSize(), chunkX, chunkZ);
        final int x = xz.getX();
        final int z = xz.getZ();
        final int maxY = getMin(heightMap[x][z], settings.maxHeight);
        final int minY = settings.minHeight; // More readable?
        int y = -1;
        // A quicker test than searching either up and down separately.
        if (Direction.matchesVertical(settings.directions)) {
            y = findOpeningVertical(rand, world, x, z, minY, maxY);
        } else if (Direction.UP.matches(settings.directions)) {
            y = randFindCeiling(world, rand, x, z, minY, maxY);
        } else if (Direction.DOWN.matches(settings.directions)) {
            y = randFindFloor(world, rand, x, z, minY, maxY);
        }
        // To - do: horizontal matches.
        return y > 0 ? full(new BlockPos(x, y, z)) : empty();
    }

    /** Moves each dimension by half of `size` in the opposite direction. */
    private static BlockPos centerBySize(BlockPos toCenter, BlockPos size) {
        int xOffset = (size.getX() / 2) * -1;
        int zOffset = (size.getZ() / 2) * -1;
        return toCenter.add(xOffset, 0, zOffset);
    }

    private static boolean canGenerateStructure(StructureSettings settings, BlockPos pos, World world) {
        return matchSources(settings.matchers, world, pos) &&
            matchAir(settings.airMatchers, world, pos) &&
            matchSolid(settings.solidMatchers, world, pos);
    }

    /**
     * Locates the first cave opening from above within the specified range.
     * Returns -1 when no opening is found.
     */
    private static int findFloor(World world, int x, int y, int z, int minY) {
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
    private static int findCeiling(World world, int x, int y, int z, int maxY) {
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
     * Randomly locates a cave opening from above within the specified range.
     * Starts at a random coordinate, then starts from the top, if nothing is found.
     * Returns -1 when no opening is found.
     */
    private static int randFindFloor(World world, Random rand, int x, int z, int minY, int maxY) {
        // Start at a random coordinate. Then try from the top, if nothing is found.
        final int startY = rand.nextInt(maxY - minY) + minY;
        int y = findFloor(world, x, startY, z, minY);
        if (y < 0) {
            y = findFloor(world, x, maxY, z, startY);
        }
        return y;
    }

    /**
     * Randomly locates a cave opening from below within the specified range.
     * Starts at a random coordinate, then starts from the bottom, if nothing is found.
     * Returns -1 when no opening is found.
     */
    private static int randFindCeiling(World world, Random rand, int x, int z, int minY, int maxY) {
        // Start at a random coordinate. Then try from the top, if nothing is found.
        final int startY = rand.nextInt(maxY - minY) + minY;
        int y = findCeiling(world, x, startY, z, maxY);
        if (y < 0) {
            y = findCeiling(world, x, minY, z, startY);
        }
        return y;
    }

    /**
     * Locates the first cave opening from a random coordinate, randomly searching up or down.
     * Returns -1 when no opening is found.
     */
    private static int findOpeningVertical(Random rand, World world, int x, int z, int minY, int maxY) {
        final int startY = rand.nextInt(maxY - minY) + minY;
        if (rand.nextBoolean()) {
            // First search from the center up.
            final int fromCenter = findOpeningFromBelow(world, x, startY, z, maxY);
            if (fromCenter > 0) {
                return fromCenter;
            }
            // Then try from the bottom to the center.
            return findOpeningFromBelow(world, x, minY, z, startY);
        } else {
            final int fromCenter = findOpeningFromAbove(world, x, startY, z, minY);
            if (fromCenter > 0) {
                return fromCenter;
            }
            return findOpeningFromAbove(world, x, maxY, z, startY);
        }
    }

    /**
     * Searches up until an opening is found, either ceiling or floor.
     * Returns -1 when no opening is found.
     */
    private static int findOpeningFromBelow(World world, int x, int y, int z, int maxY) {
        boolean previouslySolid = isSolid(world, x, y, z);
        for (int h = y; h < maxY; h++) {
            final boolean currentlySolid = isSolid(world, x, h, z);
            if (previouslySolid ^ currentlySolid) {
                return h;
            }
            previouslySolid = currentlySolid;
        }
        return -1;
    }

    /**
     * Searches down until an opening is found, either ceiling or floor.
     * Returns -1 when no opening is found.
     */
    private static int findOpeningFromAbove(World world, int x, int y, int z, int minY) {
        boolean previouslySolid = isSolid(world, x, y, z);
        for (int h = y; h > minY; h--) {
            final boolean currentlySolid = isSolid(world, x, h, z);
            if (previouslySolid ^ currentlySolid) {
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

    /** Determines whether air blocks exist at each of the relative coordinates. */
    private static boolean matchAir(BlockPos[] relative, World world, BlockPos origin) {
        for (BlockPos p : relative) {
            if (isSolid(world, origin.add(p.getX(), p.getY(), p.getZ()))) {
                return false;
            }
        }
        return true;
    }

    /** Determines whether solid blocks exist at each of the relative coordinates. */
    private static boolean matchSolid(BlockPos[] relative, World world, BlockPos origin) {
        for (BlockPos p : relative) {
            if (!isSolid(world, origin.add(p.getX(), p.getY(), p.getZ()))) {
                return false;
            }
        }
        return true;
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
        return isSolid(world, new BlockPos(x, y, z));
    }

    /** Determines whether the IBlockState at the input coordinates is an opaque cube. */
    private static boolean isSolid(World world, BlockPos pos) {
        return world.getBlockState(pos).isOpaqueCube();
    }

    private static int randCoord(Random rand, int size, int chunk) {
        size = Math.abs(size);
        final int offset = (chunk * 16) + 18;
        if (size < 16) {
            // Don't let even numbers break chunk bounds (?)
            if (size % 2 == 0) {
                size += 1;
            }
            return rand.nextInt(16 - size) + (size / 2) + offset;
        }
        return offset + 8;
    }

    private static BlockPos randCoords(Random rand, BlockPos size, int chunkX, int chunkZ) {
        final int x = randCoord(rand, size.getX(), chunkX);
        final int z = randCoord(rand, size.getZ(), chunkZ);
        return new BlockPos(x, 0, z);
    }
}