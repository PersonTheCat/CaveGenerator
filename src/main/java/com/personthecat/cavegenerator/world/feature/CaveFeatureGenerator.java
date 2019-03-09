package com.personthecat.cavegenerator.world.feature;

import com.personthecat.cavegenerator.CaveInit;
import com.personthecat.cavegenerator.Main;
import com.personthecat.cavegenerator.world.CaveGenerator;
import com.personthecat.cavegenerator.world.HeightMapLocator;
import fastnoise.FastNoise;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
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
    /** The number of times to try locating vertical surfaces for structures. */
    private static final int VERTICAL_RETRIES = 3;
    /** The number of times to try locating horizontal surfaces for structures. */
    private static final int HORIZONTAL_RETRIES = 20;
    /** The value returned by any surface locator when no surface is found. */
    private static final int NONE_FOUND = Integer.MIN_VALUE;

    @Override
    public void generate(Random rand, int chunkX, int chunkZ, World world, IChunkGenerator chunkGen, IChunkProvider chunkProv) {
        // Once again, there is no way to avoid retrieving this statically.
        final Map<String, CaveGenerator> generators = Main.instance.generators;
        final int dimension = world.provider.getDimension();

        if (CaveInit.anyHasWorldDecorator(generators, dimension)) {
            final int[][] heightMap = HeightMapLocator.getHeightFromWorld(world, chunkX, chunkZ);

            for (CaveGenerator generator : generators.values()) {
                if (generator.canGenerate(dimension)) { // Do biome test later on.
                    DecoratorInfo info = new DecoratorInfo(heightMap, generator, rand, chunkX, chunkZ, world);
                    generatePillars(info);
                    generateStalactites(info);
                    generateStructures(info);
                }
            }
        }
    }

    /**
     * Spawns a series of giant pillars in this chunk, according to the settings
     * contained in `gen`.
     */
    private static void generatePillars(DecoratorInfo info) {
        for (GiantPillar pillar : info.gen.settings.decorators.pillars) {
            for (int i = 0; i < info.rand.nextInt(pillar.getFrequency() + 1); i++) {
                // Avoid pillars spawning right next to each other.
                final int x = ((info.rand.nextInt(6) * 2) + 2) + (info.chunkX * 16); // 2 to 14
                final int z = ((info.rand.nextInt(6) * 2) + 1) + (info.chunkZ * 16); // 1 to 13
                final int y = numBetween(info.rand, pillar.getMinHeight(), pillar.getMaxHeight());

                final int opening = findCeiling(info.world, x, y, z, pillar.getMaxHeight());
                if (opening != NONE_FOUND) {
                    pillar.generate(info.world, info.rand, new BlockPos(x, opening, z));
                }
            }
        }
    }

    /** Runs the stalactite generator for each stalactite in `gen`. */
    private static void generateStalactites(DecoratorInfo info) {
        for (LargeStalactite st : info.gen.settings.decorators.stalactites) {
            generateStalactite(st, info);
        }
    }

    /** Generates a stalactite in the specified bounds. */
    private static void generateStalactite(LargeStalactite st, DecoratorInfo info) {
        final FastNoise noise = st.getNoise(info.rand.nextInt());
        final Random localRand = new Random(info.rand.nextInt());
        final int distance = stalactiteResolution(st.getChance());

        // Each iteration increments by `distance`. This changes the frequency
        // with which `noise` is calculated, theoretically impacting performance.
        // Lower frequencies do not require as high a resolution, as this
        // difference would typically not be visible.
        for (int x = info.offsetX; x < info.offsetX + 16; x = x + distance) {
            for (int z = info.offsetZ; z < info.offsetZ + 16; z = z + distance) {
                final Biome biome = info.world.getBiome(new BlockPos(x, 0, z));
                if (info.gen.canGenerate(biome) && canSpawnStalactite(st, noise, x, z)) {
                    handleStalactiteRegion(info, st, localRand, x, z, distance);
                }
            }
        }
    }

    /** Attempts to spawn a stalactite at every coordinate pair in this region. */
    private static void handleStalactiteRegion(DecoratorInfo info, LargeStalactite st, Random rand, int x, int z, int distance) {
        for (int l = x; l < x + distance; l++) {
            for (int d = z; d < z + distance; d++) {
                // Check this earlier -> do less when it fails.
                if (rand.nextDouble() >= st.getChance()) {
                    continue;
                }
                final int maxHeight = getMin(info.heightMap[l & 15][d & 15], st.getMaxHeight());
                if (st.getMinHeight() >= maxHeight) continue; // If the heightmap value is <= minHeight;
                final int startHeight = numBetween(rand, st.getMinHeight(), maxHeight);
                // Stalactite -> go up and find a surface.
                // Stalagmite -> go down and find a surface.
                final int y = st.getType() == LargeStalactite.Type.STALACTITE ?
                    findCeiling(info.world, l, startHeight, d, maxHeight):
                    findFloor(info.world, l, startHeight, d, st.getMinHeight());
                trySpawnStalactite(st, rand, l, y, d, info.world);
            }
        }
    }

    /**
     * Attempts to spawn a single stalactite at this exact location. Fails
     * when y < 0 || blocks don't match || randomly, according to st#getChance.
     */
    private static void trySpawnStalactite(LargeStalactite st, Random rand, int x, int y, int z, World world) {
        if (y != NONE_FOUND) {
            final BlockPos pos = new BlockPos(x, y, z);
            if (matchSources(st.getMatchers(), world, pos)) {
                st.generate(world, rand, pos);
            }
        }
    }

    /** Spawns a series of applicable structures at the input coorindates. */
    private static void generateStructures(DecoratorInfo info) {
        for (StructureSettings settings : info.gen.settings.structures) {
            for (int i = 0; i < settings.frequency; i++) {
                if (info.rand.nextDouble() <= settings.chance) {
                    generateStructure(info, settings);
                }
            }
        }
    }

    /** Attempts to spawn a structure at the input chunk coordinates. */
    private static void generateStructure(DecoratorInfo info, StructureSettings settings) {
        // As always, there's really no good way to retrieve this non-statically.
        // Would need to write the game myself to work around that.
        Map<String, Template> structures = Main.instance.structures;
        Template structure = StructureSpawner.getTemplate(structures, settings.name, info.world);
        // Attempt to locate a suitable spawn position and then proceed.
        getSpawnPos(info, settings, structure).ifPresent(pos -> {
            if (canGenerateStructure(settings, pos, info.world)) {
                preStructureSpawn(info, settings, pos);
                BlockPos adjusted = offset(centerBySize(pos, structure.getSize()), settings.offset);
                StructureSpawner.spawnStructure(structure, settings.settings, info.world, adjusted);
            }
        });
    }

    /** Attempts to determine a suitable spawn point in the current location. */
    private static Optional<BlockPos> getSpawnPos(DecoratorInfo info, StructureSettings settings, Template structure) {
        // Favor vertical spawns, detecting horizontal surfaces first.
        if (settings.directions.up || settings.directions.down) {
            Optional<BlockPos> vertical =
                getSpawnPosVertical(info, settings, structure);
            if (vertical.isPresent()) {
                return vertical;
            } // else, try horizontal
        }
        // Attempt to locate any vertical surfaces, if necessary.
        if (settings.directions.side) {
            return getSpawnPosHorizontal(info, settings, structure);
        }
        return empty();
    }

    /** Attempts to find a spawn point for this structure on the vertical axis. */
    private static Optional<BlockPos> getSpawnPosVertical(DecoratorInfo info, StructureSettings settings, Template structure) {
        for (int i = 0; i < VERTICAL_RETRIES; i++) {
            // Start with random (x, z) coordinates.
            final BlockPos xz = randCoords(info.rand, structure.getSize(), info.offsetX, info.offsetZ);
            // Destructure the resultant coordinates.
            final int x = xz.getX();
            final int z = xz.getZ();
            // Determine the height bounds for these coordinates.
            final int maxY = getMin(info.heightMap[x & 15][z & 15], settings.maxHeight);
            final int minY = settings.minHeight; // More readable?

            int y = NONE_FOUND;
            // Search both -> just up -> just down.
            if (settings.directions.up && settings.directions.down) {
                y = findOpeningVertical(info.rand, info.world, x, z, minY, maxY);
            } else if (settings.directions.up) {
                y = randFindCeiling(info.world, info.rand, x, z, minY, maxY);
            } else {
                y = randFindFloor(info.world, info.rand, x, z, minY, maxY);
            }
            // Check to see if an opening was found, else retry;
            if (y != NONE_FOUND) {
                return full(new BlockPos(x, y, z));
            }
        }
        return empty();
    }

    /** Attempts to find a spawn point for this structure on the horizontal axes. */
    private static Optional<BlockPos> getSpawnPosHorizontal(DecoratorInfo info, StructureSettings settings, Template structure) {
        final BlockPos size = structure.getSize();
        for (int i = 0; i < HORIZONTAL_RETRIES; i++) {
            Optional<BlockPos> pos = empty();
            if (info.rand.nextBoolean()) {
                pos = randCoordsNS(info, size.getX(), settings.minHeight, settings.maxHeight);
            } else {
                pos = randCoordsEW(info, size.getZ(), settings.minHeight, settings.maxHeight);
            }
            if (pos.isPresent()) {
                return pos;
            }
        }
        return empty();
    }

    /** Moves each dimension by half of `size` in the opposite direction. */
    private static BlockPos centerBySize(BlockPos toCenter, BlockPos size) {
        final int xOffset = (size.getX() / 2) * -1;
        final int zOffset = (size.getZ() / 2) * -1;
        return toCenter.add(xOffset, 0, zOffset);
    }

    /** Applies an offset to the original BlockPos. */
    private static BlockPos offset(BlockPos original, BlockPos offset) {
        return original.add(offset.getX(), offset.getY(), offset.getZ());
    }

    private static boolean canGenerateStructure(StructureSettings settings, BlockPos pos, World world) {
        return matchSources(settings.matchers, world, pos) &&
            matchNonSolid(settings.nonSolidMatchers, world, pos) &&
            matchSolid(settings.solidMatchers, world, pos) &&
            matchAir(settings.airMatchers, world, pos) &&
            matchWater(settings.waterMatchers, world, pos);
    }

    /** All operations related to structures before spawning should be organized herein. */
    private static void preStructureSpawn(DecoratorInfo info, StructureSettings settings, BlockPos pos) {
        if (settings.rotateRandomly) {
            Rotation randRotation = Rotation.values()[info.rand.nextInt(3)];
            settings.settings.setRotation(randRotation);
        }
        if (settings.debugSpawns) {
            info("Spawning {} at {}", settings.name, pos);
        }
    }

    /**
     * Locates the first cave opening from above within the specified range.
     * Returns NONE_FOUND when no opening is found.
     */
    private static int findFloor(World world, int x, int y, int z, int minY) {
        boolean previouslySolid = isSolid(world, x, y, z);
        for (int h = y - 1; h > minY; h--) {
            final boolean currentlySolid = isSolid(world, x, h, z);
            if (!previouslySolid && currentlySolid) {
                return h;
            }
            previouslySolid = currentlySolid;
        }
        return NONE_FOUND;
    }

    /**
     * Locates the first cave opening from below within the specified range.
     * Returns NONE_FOUND when no opening is found.
     */
    private static int findCeiling(World world, int x, int y, int z, int maxY) {
        boolean previouslySolid = isSolid(world, x, y, z);
        for (int h = y + 1; h < maxY; h++) {
            final boolean currentlySolid = isSolid(world, x, h, z);
            if (!previouslySolid && currentlySolid) {
                return h;
            }
            previouslySolid = currentlySolid;
        }
        return NONE_FOUND;
    }

    /**
     * Randomly locates a cave opening from above within the specified range.
     * Starts at a random coordinate, then starts from the top, if nothing is found.
     * Returns NONE_FOUND when no opening is found.
     */
    private static int randFindFloor(World world, Random rand, int x, int z, int minY, int maxY) {
        // Start at a random coordinate. Then try from the top, if nothing is found.
        final int startY = numBetween(rand, minY, maxY);
        int y = findFloor(world, x, startY, z, minY);
        if (y == NONE_FOUND) {
            y = findFloor(world, x, maxY, z, startY);
        }
        return y;
    }

    /**
     * Randomly locates a cave opening from below within the specified range.
     * Starts at a random coordinate, then starts from the bottom, if nothing is found.
     * Returns NONE_FOUND when no opening is found.
     */
    private static int randFindCeiling(World world, Random rand, int x, int z, int minY, int maxY) {
        // Start at a random coordinate. Then try from the top, if nothing is found.
        final int startY = numBetween(rand, minY, maxY);
        int y = findCeiling(world, x, startY, z, maxY);
        if (y == NONE_FOUND) {
            y = findCeiling(world, x, minY, z, startY);
        }
        return y;
    }

    /**
     * Locates the first cave opening from a random coordinate, randomly searching up or down.
     * Returns NONE_FOUND when no opening is found.
     */
    private static int findOpeningVertical(Random rand, World world, int x, int z, int minY, int maxY) {
        final int startY = numBetween(rand, minY, maxY);
        if (rand.nextBoolean()) {
            // First search from the center up.
            final int fromCenter = findOpeningFromBelow(world, x, startY, z, maxY);
            if (fromCenter != NONE_FOUND) {
                return fromCenter;
            }
            // Then try from the bottom to the center.
            return findOpeningFromBelow(world, x, minY, z, startY);
        } else {
            final int fromCenter = findOpeningFromAbove(world, x, startY, z, minY);
            if (fromCenter != NONE_FOUND) {
                return fromCenter;
            }
            return findOpeningFromAbove(world, x, maxY, z, startY);
        }
    }

    /**
     * Searches up until an opening is found, either ceiling or floor.
     * Returns NONE_FOUND when no opening is found.
     */
    private static int findOpeningFromBelow(World world, int x, int y, int z, int maxY) {
        boolean previouslySolid = isSolid(world, x, y, z);
        for (int h = y; h < maxY; h++) {
            final boolean currentlySolid = isSolid(world, x, h, z);
            if (previouslySolid ^ currentlySolid) {
                return currentlySolid ? h : h - 1;
            }
            previouslySolid = currentlySolid;
        }
        return NONE_FOUND;
    }

    /**
     * Searches down until an opening is found, either ceiling or floor.
     * Returns NONE_FOUND when no opening is found.
     */
    private static int findOpeningFromAbove(World world, int x, int y, int z, int minY) {
        boolean previouslySolid = isSolid(world, x, y, z);
        for (int h = y; h > minY; h--) {
            final boolean currentlySolid = isSolid(world, x, h, z);
            if (previouslySolid ^ currentlySolid) {
                return currentlySolid ? h : h + 1;
            }
            previouslySolid = currentlySolid;
        }
        return NONE_FOUND;
    }

    /**
     * Searches north with and offset of 8 until an opening is found.
     * Returns NONE_FOUND if no opening is found.
     */
    private static int findOpeningNorth(World world, int x, int y, int offsetZ) {
        boolean previouslySolid = isSolid(world, x, y, offsetZ + 15);
        for (int z = offsetZ + 14; z >= offsetZ; z--) {
            final boolean currentlySolid = isSolid(world, x, y, z);
            if (previouslySolid ^ currentlySolid) {
                return z;
            }
            previouslySolid = currentlySolid;
        }
        return NONE_FOUND;
    }

    /**
     * Searches south with and offset of 8 until an opening is found.
     * Returns NONE_FOUND if no opening is found.
     */
    private static int findOpeningSouth(World world, int x, int y, int offsetZ) {
        boolean previouslySolid = isSolid(world, x, y, offsetZ);
        for (int z = offsetZ + 1; z < offsetZ + 16; z++) {
            final boolean currentlySolid = isSolid(world, x, y, z);
            if (previouslySolid ^ currentlySolid) {
                return z;
            }
            previouslySolid = currentlySolid;
        }
        return NONE_FOUND;
    }

    /**
     * Searches east with and offset of 8 until an opening is found.
     * Returns NONE_FOUND if no opening is found.
     */
    private static int findOpeningEast(World world, int y, int z, int offsetX) {
        boolean previouslySolid = isSolid(world, offsetX, y, z);
        for (int x = offsetX + 1; x < offsetX + 16; x++) {
            final boolean currentlySolid = isSolid(world, x, y, z);
            if (previouslySolid ^ currentlySolid) {
                return x;
            }
            previouslySolid = currentlySolid;
        }
        return NONE_FOUND;
    }

    /**
     * Searches west with and offset of 8 until an opening is found.
     * Returns NONE_FOUND if no opening is found.
     */
    private static int findOpeningWest(World world, int y, int z, int offsetX) {
        boolean previouslySolid = isSolid(world, offsetX + 15, y, z);
        for (int x = offsetX + 14; x >= offsetX; x--) {
            final boolean currentlySolid = isSolid(world, x, y, z);
            if (previouslySolid ^ currentlySolid) {
                return x;
            }
            previouslySolid = currentlySolid;
        }
        return NONE_FOUND;
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

    /** Determines whether non-solid blocks exist at each of the relative coordinates. */
    private static boolean matchNonSolid(BlockPos[] relative, World world, BlockPos origin) {
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

    /** Determines whether air blocks exist at each of the relative coordinates. */
    private static boolean matchAir(BlockPos[] relative, World world, BlockPos origin) {
        for (BlockPos p : relative) {
            if (!world.getBlockState(origin.add(p.getX(), p.getY(), p.getZ())).equals(Blocks.AIR.getDefaultState())) {
                return false;
            }
        }
        return true;
    }

    /** Determines whether air blocks exist at each of the relative coordinates. */
    private static boolean matchWater(BlockPos[] relative, World world, BlockPos origin) {
        for (BlockPos p : relative) {
            if (!world.getBlockState(origin.add(p.getX(), p.getY(), p.getZ())).equals(Blocks.WATER.getDefaultState())) {
                return false;
            }
        }
        return true;
    }

    /** Determines whether the input LargeStalactite can spawn at this location. */
    private static boolean canSpawnStalactite(LargeStalactite st, FastNoise noise, int x, int z) {
        return !st.spawnInPatches() || noise.GetAdjustedNoise(x, z) < st.getThreshold();
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

    /** Returns a random number between the input bounds. */
    private static int numBetween(Random rand, int min, int max) {
        return min == max ? min : rand.nextInt(max - min) + min;
    }

    /**
     * Returns a random relative coordinate such that result + size <= 16.
     * In other words, when a structure is started at this coordinate, the
     * other end cannot exceed chunk bounds.
     */
    private static int cornerInsideChunkBounds(Random rand, int size) {
        return rand.nextInt(16 - size) + (size / 2);
    }

    /**
     * Generate a random x or z coordinate based on size. Ensure that
     * the resultant coordinate will not produce cascading gen lag.
     */
    private static int randCoord(Random rand, int size, int offset) {
        size = Math.abs(size);
        if (size < 16) {
            // Don't let even numbers break chunk bounds (?)
            if (size % 2 == 0) {
                size += 1;
            }
            return cornerInsideChunkBounds(rand, size) + offset;
        }
        // The size is too large. Spawn at the intersection
        // of all four chunks.
        return offset + 8; // chunk * 16 + 16
    }

    /** Generates random, valid coordinate pair for this location. */
    private static BlockPos randCoords(Random rand, BlockPos size, int offsetX, int offsetZ) {
        final int x = randCoord(rand, size.getX(), offsetX);
        final int z = randCoord(rand, size.getZ(), offsetZ);
        return new BlockPos(x, 0, z);
    }

    /**
     * Attempts to find a random surface on the east-west axis by scaling
     * north-south in a random direction.
     * Returns Optional#empty if no surface is found, or if the surface
     * found is not below the terrain height.
     */
    private static Optional<BlockPos> randCoordsNS(DecoratorInfo info, int sizeX, int minY, int maxY) {
        final int x = cornerInsideChunkBounds(info.rand, sizeX) + info.offsetX;
        final int y = numBetween(info.rand, minY, maxY);
        int z = NONE_FOUND;
        if (info.rand.nextBoolean()) {
            z = findOpeningNorth(info.world, x, y, info.offsetZ);
        } else {
            z = findOpeningSouth(info.world, x, y, info.offsetZ);
        }
        if (z != NONE_FOUND && y < info.heightMap[x & 15][z & 15]) {
            return full(new BlockPos(x, y, z));
        }
        return empty();
    }

    /**
     * Attempts to find a random surface on the north-south axis by scaling
     * east-west in a random direction.
     * Returns Optional#empty if no surface is found, or if the surface
     * found is not below the terrain height.
     */
    private static Optional<BlockPos> randCoordsEW(DecoratorInfo info, int sizeZ, int minY, int maxY) {
        final int z = cornerInsideChunkBounds(info.rand, sizeZ) + info.offsetZ;
        final int y = numBetween(info.rand, minY, maxY);
        int x = NONE_FOUND;
        if (info.rand.nextBoolean()) {
            x = findOpeningEast(info.world, y, z, info.offsetX);
        } else {
            x = findOpeningWest(info.world, y, z, info.offsetX);
        }
        if (x != NONE_FOUND && y < info.heightMap[x & 15][z & 15]) {
            return full(new BlockPos(x, y, z));
        }
        return empty();
    }

    /**
     * A DTO used for transferring information used
     * by all of the generators in this class.
     */
    private static class DecoratorInfo {
        final int[][] heightMap;
        final CaveGenerator gen;
        final Random rand;
        final int chunkX, chunkZ, offsetX, offsetZ;
        final World world;

        private DecoratorInfo(
            int[][] heightMap,
            CaveGenerator gen,
            Random rand,
            int chunkX,
            int chunkZ,
            World world
        ) {
            this.heightMap = heightMap;
            this.gen = gen;
            this.rand = rand;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.offsetX = chunkX * 16 + 8;
            this.offsetZ = chunkZ * 16 + 8;
            this.world = world;
        }
    }
}