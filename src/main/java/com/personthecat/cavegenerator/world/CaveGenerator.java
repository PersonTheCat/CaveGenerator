package com.personthecat.cavegenerator.world;

import com.personthecat.cavegenerator.util.RandomChunkSelector;
import fastnoise.FastNoise;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.ChunkPrimer;

import com.personthecat.cavegenerator.world.StoneCluster.ClusterInfo;
import com.personthecat.cavegenerator.world.GeneratorSettings.*;
import net.minecraft.world.gen.NoiseGeneratorSimplex;
import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.personthecat.cavegenerator.util.CommonMethods.*;

public class CaveGenerator {
    /** A few convenient values. */
    private static final float PI_OVER_2 = (float) (Math.PI / 2);
    private static final IBlockState BLK_AIR = Blocks.AIR.getDefaultState();
    private static final IBlockState BLK_WATER = Blocks.WATER.getDefaultState();
    private static final IBlockState BLK_STONE = Blocks.STONE.getDefaultState();

    /** Mandatory fields that must be initialized by the constructor */
    private final World world;
    public final GeneratorSettings settings;

    /** Noise generators. */
    private final RandomChunkSelector selector;
    private final NoiseGeneratorSimplex miscNoise;
    private final FastNoise cavernNoise, ceilNoise, floorNoise;

    /** Primary constructor. */
    public CaveGenerator(World world, GeneratorSettings settings) {
        // Main values.
        this.world = world;
        this.settings = settings;
        // Noise generators.
        long seed = world.getSeed();
        this.selector = new RandomChunkSelector(seed);
        this.miscNoise = new NoiseGeneratorSimplex(new Random(seed));
        // To-do: ensure that this is more unique (integer overflow).
        this.cavernNoise = settings.caverns.noise.getGenerator((int) seed);
        this.ceilNoise = settings.caverns.ceilNoise.getGenerator((int) seed >> 2);
        this.floorNoise = settings.caverns.floorNoise.getGenerator((int) seed >> 4);
    }

    /** Returns whether the generator is enabled globally. */
    public boolean enabled() {
        return settings.conditions.enabled;
    }

    /** Returns whether the generator is enabled for the current dimension. */
    public boolean canGenerate(int dimension) {
        return enabled() && validDimension(dimension);
    }

    /** Returns whether the generator is enabled for the current biome. */
    public boolean canGenerate(Biome biome) {
        return enabled() && validBiome(biome);
    }

    /** Returns whether the generator is enabled for the current dimension and biome. */
    public boolean canGenerate(int dimension, Biome biome) {
        return canGenerate(dimension) && validBiome(biome);
    }

    /** Returns whether the input dimension is valid for this generator. */
    private boolean validDimension(int dim) {
        if (settings.conditions.dimensions.length == 0) {
            return true;
        }
        final boolean contains = ArrayUtils.contains(settings.conditions.dimensions, dim);
        // useBlacklist ? !contains : contains
        return settings.conditions.dimensionBlacklist != contains;
    }

    /** Returns whether the input biome is valid for this generator. */
    private boolean validBiome(Biome biome) {
        if (settings.conditions.biomes.length == 0) {
            return true;
        }
        final boolean contains = ArrayUtils.contains(settings.conditions.biomes, biome);
        return settings.conditions.biomeBlacklist != contains;
    }

    /** Returns whether the generator has any surface decorators. */
    public boolean hasLocalDecorators() {
        return settings.decorators.ceilingDecorators.length > 0 ||
            settings.decorators.floorDecorators.length > 0 ||
            settings.decorators.wallDecorators.length > 0;
    }

    /** Returns whether the generator has any world decorators. */
    public boolean hasWorldDecorators() {
        return settings.decorators.pillars.length > 0 ||
            settings.decorators.stalactites.length > 0 ||
            settings.structures.length > 0;
    }

    /** Starts a tunnel system between the input chunk coordinates. */
    public void startTunnels(Random rand, int destChunkX, int destChunkZ, int chunkX, int chunkZ, ChunkPrimer primer) {
        final int frequency = getTunnelFrequency(rand);
        for (int i = 0; i < frequency; i++) {
            // Retrieve the height parameters from the settings.
            final int minHeight = settings.tunnels.minHeight;
            final int maxHeight = settings.tunnels.maxHeight;
            final int heightDiff = maxHeight - minHeight;

            // Get random coordinates in destination chunk.
            final float x = (float) ((destChunkX * 16) + rand.nextInt(16));
            final float y = (float) (rand.nextInt(rand.nextInt(heightDiff) + minHeight));
            final float z = (float) ((destChunkZ * 16) + rand.nextInt(16));

            // Determine the number of branches to spawn.
            int branches = 1;
            if (rand.nextInt(settings.tunnels.systemInverseChance) == 0) {
                // Add a room at the center? of the system.
                branches += rand.nextInt(4); // To-do: make this variable.
            }

            for (int j = 0; j < branches; j++) {
                TunnelSettings cfg = settings.tunnels;
                final int distance = cfg.startDistance;
                PrimerData data = new PrimerData(primer, chunkX, chunkZ);
                TunnelPathInfo path = new TunnelPathInfo(cfg, rand, x, y, z);

                // Per-vanilla: this randomly increases the size.
                if (rand.nextInt(10) == 0) {
                    addRoom(rand, data, x, y, z);
                    // Randomly alter scale. Average difference depends on original value.
                    path.multiplyScale(rand.nextFloat() * rand.nextFloat() * 3.00F + 1.00F);
                }
                addTunnel(rand.nextLong(), data, path,0, distance);
            }
        }
    }

    /** Starts a ravine between the input chunk coordinates. */
    public void startRavine(Random rand, int destChunkX, int destChunkZ, int chunkX, int chunkZ, ChunkPrimer primer) {
        // Retrieve the height parameters from the settings.
        final int minHeight = settings.ravines.minHeight;
        final int maxHeight = settings.ravines.maxHeight;
        final int heightDiff = maxHeight - minHeight;

        // Get random coordinates in destination chunk.
        final float x = (float) ((destChunkX * 16) + rand.nextInt(16));
        // To-do: verify these numbers.
        final float y = (float) (rand.nextInt(rand.nextInt(maxHeight) + 8) + heightDiff);
        final float z = (float) ((destChunkZ * 16) + rand.nextInt(16));

        RavineSettings cfg = settings.ravines;
        final int distance = cfg.startDistance;
        PrimerData data = new PrimerData(primer, chunkX, chunkZ);
        TunnelPathInfo path = new TunnelPathInfo(cfg, rand, x, y, z);

        addRavine(rand.nextLong(), data, path, distance);
    }

    /** Generates any applicable noise-based features in the current chunk. */
    public void addNoiseFeatures(Random rand, ChunkPrimer primer, int chunkX, int chunkZ) {
        if (settings.caverns.enabled) {
            generateCaverns(rand, primer, chunkX, chunkZ);
        }
        if (settings.decorators.stoneClusters.length > 0) {
            generateClusters(rand, primer, chunkX, chunkZ);
        }
        if (settings.decorators.stoneLayers.length > 0) {
            generateLayers(primer, chunkX, chunkZ);
        }
    }

    /**
     * Mod of {~~@link net.minecraft.world.gen.MapGenCaves#addTunnel} by PersonTheCat.
     * This is the basic function responsible for spawning a chained sequence of
     * angled spheres in the world. Supports object-specific replacement of most
     * variables, as well as a few new variables for controlling shapes, adding
     * noise-based alternatives to air, and wall decorations.
     * @param seed      The world's seed. Use to create a local Random object for
     *                  regen parity.
     * @param data      Data containing the current chunk primer and coordinates.
     * @param path      Data containing information about the path of tunnel segments to be created.
     * @param position  A measure of progress until `distance`.
     * @param distance  The length of the tunnel. 0 -> # ( 132 to 176 ).
     */
    private void addTunnel(long seed, PrimerData data, TunnelPathInfo path, int position, int distance) {
        // Master RNG for this tunnel.
        final Random mast = new Random(seed);
        // Avoid issues with inconsistent Random calls.
        final Random dec = new Random(seed);
        distance = getTunnelDistance(mast, distance);
        // Determine where to place branches, if applicable.
        final int randomBranchIndex = mast.nextInt(distance / 2) + distance / 4;
        final boolean randomNoiseCorrection = mast.nextInt(6) == 0;

        for (int currentPos = position; currentPos < distance; currentPos++) {
            // Determine the radius by `scale`.
            final double radiusXZ = 1.5D + (MathHelper.sin(currentPos * (float) Math.PI / distance) * path.getScale());
            final double radiusY = radiusXZ * path.getScaleY();
            path.update(mast, settings.tunnels.noiseYReduction, randomNoiseCorrection ? 0.92F : 0.70F, 0.1F);

            if (path.getScale() > 1.00F && distance > 0 && currentPos == randomBranchIndex) {
                addBranches(mast, data, path, currentPos, distance);
                return;
            }
            // Randomly stop?
            if (mast.nextInt(4) == 0) {
                continue;
            }
            // Make sure we haven't travelled too far?
            if (path.travelledTooFar(data, currentPos, distance)) {
                return;
            }
            if (path.touchesChunk(data, radiusXZ * 2.0)) {
                // Calculate all of the positions in the section.
                // We'll be using them multiple times.
                final TunnelSectionInfo section =
                    new TunnelSectionInfo(data, path, radiusXZ, radiusY)
                    .calculate();
                createFullSection(dec, data, section);
            }
        }
    }

    /**
     * Variant of addTunnel() which extracts the features dedicated to generating
     * single, symmetrical spheres, known internally as "rooms." This may be
     * slightly more redundant, but it should increase the algorithm's readability.
     */
    private void addRoom(Random master, PrimerData data, double x, double y, double z) {
        // Construct these initial values using `rand`, consistent
        // with the vanilla setup.
        final long seed = master.nextLong();
        final float scale = master.nextFloat() * settings.rooms.scale + 1;
        final float scaleY = settings.rooms.scaleY;
        // Construct a local Random object for use within this function,
        // also matching the vanilla setup.
        final Random rand = new Random(seed);
        // To-do: Verify whether this can just be 0.
        final int distance = getTunnelDistance(rand, 0);
        // To-do: Verify why this is necessary.
        final int position = distance / 2;
        // Determine the radius by `scale`.
        final double radiusXZ = 1.5D + (MathHelper.sin(position * (float) Math.PI / distance) * scale);
        final double radiusY = radiusXZ * scaleY;
        // Coordinates are normally shifted based on angleXZ
        // and angleY. When the input angle would otherwise
        // just be 0.0, this is the only value that would
        // actually change.
        x += 1;

        // Calculate all of the positions in the section.
        // We'll be using them multiple times.
        final TunnelSectionInfo section =
            new TunnelSectionInfo(data, x, y, z, radiusXZ, radiusY)
            .calculate();
        createFullSection(master, data, section);
    }

    private void addBranches(Random rand, PrimerData data, TunnelPathInfo path, int currentPos, int distance) {
        final float angleXZ1 = path.getAngleXZ() - PI_OVER_2;
        final float angleXZ2 = path.getAngleXZ() + PI_OVER_2;
        final float angleY = path.getAngleY() / 3.0F;
        TunnelPathInfo reset1, reset2;

        if (settings.tunnels.resizeBranches) { // In vanilla, tunnels are resized when branching.
            reset1 = path.reset(angleXZ1, angleY, rand.nextFloat() * 0.5F + 0.5F, 1.00F);
            reset2 = path.reset(angleXZ2, angleY, rand.nextFloat() * 0.5F + 0.5F, 1.00F);
        } else { // Continue with the same size (not vanilla).
            reset1 = path.reset(angleXZ1, angleY, path.getScale(), path.getScaleY());
            reset2 = path.reset(angleXZ2, angleY, path.getScale(), path.getScaleY());
        }
        addTunnel(rand.nextLong(), data, reset1, currentPos, distance);
        addTunnel(rand.nextLong(), data, reset2, currentPos, distance);
    }

    /**
     * Variant of addTunnel() and {~~@link net.minecraft.world.gen.MapGenRavine#addTunnel}
     * which randomly alters the horizontal radius based on `mut`, a buffer of random
     * values between 1-4, stored above. The difference in scale typically observed in
     * ravines is the result of arguments input to this function.
     */
    private void addRavine(long seed, PrimerData data, TunnelPathInfo path, int distance) {
        // Master RNG for this tunnel.
        final Random mast = new Random(seed);
        // Avoid issues with inconsistent Random calls.
        final Random dec = new Random(seed);
        distance = getTunnelDistance(mast, distance);
        // Unique wall mutations for this chasm.
        final float[] mut = getMutations(mast);

        for (int currentPos = 0; currentPos < distance; currentPos++) {
            // Determine the radius by `scale`.
            final double radiusXZ = 1.5D + (MathHelper.sin(currentPos * (float) Math.PI / distance) * path.getScale());
            final double radiusY = radiusXZ * path.getScaleY();
            path.update(mast, true, settings.ravines.noiseYFactor, 0.05F);

            // Randomly stop?
            if (mast.nextInt(4) == 0) {
                continue;
            }
            // Make sure we haven't travelled too far?
            if (path.travelledTooFar(data, currentPos, distance)) {
                return;
            }
            if (path.touchesChunk(data, radiusXZ * 2.0)) {
                // Calculate all of the positions in the section.
                // We'll be using them multiple times.
                final TunnelSectionInfo section =
                    new TunnelSectionInfo(data, path, radiusXZ, radiusY)
                    .calculateMutated(mut);
                createFullSection(dec, data, section);
            }
        }
    }

    /** Determines the number of cave systems to try and spawn. */
    private int getTunnelFrequency(Random rand) {
        int frequency = settings.tunnels.frequency;
        // Verify that we have positive bounds to avoid a crash.
        if (frequency != 0) {
            frequency = rand.nextInt(rand.nextInt(rand.nextInt(frequency) + 1) + 1);
        }
        // Retrieve the baseline from the settings.
        final int chance = settings.tunnels.isolatedInverseChance;
        // Maintain seed integrity, where possible.
        // To-do: verify this logic with the original.
        if (chance != 0 && rand.nextInt(chance) != 0) {
            // Usually set frequency to 0, causing the systems to be
            // isolated from one another.
            frequency = 0;
        }
        return frequency;
    }

    /** Calculates the maximum distance for this tunnel, if needed. */
    private int getTunnelDistance(Random rand, int input) {
        if (input <= 0) {
            return 112 - rand.nextInt(28);
        }
        return input;
    }

    /** Used to produce the variations in horizontal scale seen in ravines. */
    private float[] getMutations(Random rand) {
        if (settings.ravines.useWallNoise) {
            return getMutationsNoise(rand);
        }
        return getMutationsVanilla(rand);
    }

    /** The effectively vanilla implementation of getMutations(). */
    private float[] getMutationsVanilla(Random rand) {
        float[] mut = new float[256];
        float val = 1.0f;
        for (int i = 0; i < mut.length; i++) {
            if (i == 0 || rand.nextInt(3) == 0) {
                val = rand.nextFloat() * rand.nextFloat() + 1.0f;
            }
            mut[i] = val * val;
        }
        return mut;
    }

    /** Variant of getMutations() which produces aberrations using a noise generator. */
    private float[] getMutationsNoise(Random rand) {
        float[] mut = new float[256];
        FastNoise noise = settings.ravines.wallNoise.getGenerator(rand.nextInt());
        for (int i = 0; i < mut.length; i++) {
            mut[i] = noise.GetAdjustedNoise(0, i);
        }
        return mut;
    }

    private void createFullSection(Random rand, PrimerData data, TunnelSectionInfo section) {
        // If we need to test this section for water -> is there water?
        if (!(shouldTestForWater(section.getHighestY()) && testForWater(data.p, section))) {
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

    /**
     * Returns whether a test should be run to determine whether water is
     * found and stop generating.
     */
    private boolean shouldTestForWater(int highestY) {
        for (CaveBlock filler : settings.decorators.caveBlocks) {
            if (filler.getFillBlock().equals(BLK_WATER) &&
                highestY <= filler.getMaxHeight() + 10) { // A little wiggle room.
                return false;
            }
        }
        return true;
    }

    /** Determines whether any water exists in the current section. */
    private boolean testForWater(ChunkPrimer primer, TunnelSectionInfo section) {
        return section.test(pos ->
            primer.getBlockState(pos.getX(), pos.getY(), pos.getZ()).equals(BLK_WATER)
        );
    }

    /** Generates giant air pockets in this chunk using a 3D noise generator. */
    private void generateCaverns(Random rand, ChunkPrimer primer, int chunkX, int chunkZ) {
        final int[][] heightMap = HeightMapLocator.getHeightFromPrimer(primer);
        // Using an array to store calculations instead of redoing all of the
        // noise generation below when decorating caverns. Some calculations
        // *cannot* be done twice, but this should still be faster, regardless.
        final boolean[][][] caverns = new boolean[getMaxCaveHeight(settings.caverns)][16][16];

        for (int x = 0; x < 16; x++) {
            final int actualX = x + (chunkX * 16);
            for (int z = 0; z < 16; z++) {
                final int actualZ = z + (chunkZ * 16);
                final int ceil = (int) ceilNoise.GetAdjustedNoise(actualX, actualZ);
                final int floor = (int) floorNoise.GetAdjustedNoise(actualX, actualZ);
                // Use this cavern's max height or the terrain height, whichever is lower.
                final int max = ceil + getMin(settings.caverns.maxHeight, heightMap[x][z]);
                final int min = floor + settings.caverns.minHeight;

                for (int y = min; y <= max; y++) {
                    final float scaledY = y / settings.caverns.noise.scaleY;

                    if (cavernNoise.GetBoolean(actualX, scaledY, actualZ)) {
                        replaceBlock(rand, primer, x, y, z, chunkX, chunkZ, false);
                        caverns[y][z][x] = true;
                    }
                }
            }
        }
        if (hasLocalDecorators()) {
            decorateCaverns(rand, primer, chunkX, chunkZ, caverns);
        }
    }

    private int getMaxCaveHeight(CavernSettings cfg) {
        final int ceilMax = cfg.ceilNoise.max;
        return cfg.maxHeight + (ceilMax > 0 ? ceilMax : 0) + 1;
    }

    /**
     * Uses the shape of some already-calculated caverns for decoration, instead of
     * regenerating. Could probably still be optimized.
     */
    private void decorateCaverns(Random rand, ChunkPrimer primer, int chunkX, int chunkZ, boolean[][][] caverns) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < settings.caverns.maxHeight; y++) {
                    if (caverns[y][z][x]) {
                        decorateBlock(rand, primer, x, y, z, chunkX, chunkZ);
                    }
                }
            }
        }
    }

    /** Generates any possible giant cluster sections in the current chunk. */
    private void generateClusters(Random rand, ChunkPrimer primer, int chunkX, int chunkZ) {
        rand.setSeed(world.getSeed()); // rand must be reset.
        List<ClusterInfo> info = locateFinalClusters(rand, chunkX, chunkZ);
        SpawnSettings cfg = settings.conditions;

        for (int x = 0; x < 16; x++) {
            final int actualX = x + (chunkX * 16);
            for (int z = 0; z < 16; z++) {
                final int actualZ = z + (chunkZ * 16);
                for (int y = cfg.maxHeight; y >= cfg.minHeight; y--) {
                    IBlockState original = primer.getBlockState(x, y, z);
                    // Only decorate actual stone.
                    if (original.equals(BLK_STONE)) {
                        applyClusters(primer, info, x, y, z, actualX, actualZ);
                    }
                }
            }
        }
    }

    /** Locates any StoneClusters that may intersect with the current chunk. */
    private List<ClusterInfo> locateFinalClusters(Random rand, int chunkX, int chunkZ) {
        List<ClusterInfo> info = new ArrayList<>();
        for (StoneCluster cluster : settings.decorators.stoneClusters) {
            // Basic info
            final int ID = cluster.getID();
            final int radiusVariance = cluster.getRadiusVariance();
            final int cRadiusX = ((cluster.getRadiusX() + radiusVariance) / 16) + 1;
            final int cRadiusZ = ((cluster.getRadiusZ() + radiusVariance) / 16) + 1;
            final int heightVariance = cluster.getHeightVariance();
            final double threshold = cluster.getSelectionThreshold();
            final int clusterSeed = rand.nextInt();

            // Locate any possible origins for this cluster based on its radii.
            for (int cX = chunkX - cRadiusX; cX < chunkX + cRadiusX; cX++) {
                for (int cZ = chunkZ - cRadiusZ; cZ < chunkZ + cRadiusZ; cZ++) {
                    if (selector.getBooleanForCoordinates(ID, cX, cZ, threshold)) {
                        // Get absolute coordinates, generate in the center.
                        final int x = (cX * 16) + 8, z = (cZ * 16) + 8;
                        // Origins can only spawn in valid biomes, but can extend
                        // as far as needed.
                        if (canGenerate(world.getBiome(new BlockPos(x, 0, z)))) {
                            // Get an RNG unique to this chunk.
                            final Random localRand = new Random(cX ^ cZ ^ clusterSeed);
                            // Randomly alter spawn height.
                            final double currentNoise = miscNoise.getValue(x, z) * heightVariance;
                            final int y = cluster.getStartHeight() + (int) currentNoise;
                            // Finalize all values.
                            final BlockPos origin = new BlockPos(x, y, z);
                            final int offset = radiusVariance / 2;
                            final int radiusX = cluster.getRadiusX() + localRand.nextInt(radiusVariance) - offset;
                            final int radiusY = cluster.getRadiusY() + localRand.nextInt(radiusVariance) - offset;
                            final int radiusZ = cluster.getRadiusZ() + localRand.nextInt(radiusVariance) - offset;
                            final int radiusX2 = radiusX * radiusX;
                            final int radiusY2 = radiusY * radiusY;
                            final int radiusZ2 = radiusZ * radiusZ;

                            // Add the new information to be returned.
                            info.add(new ClusterInfo(cluster, origin, radiusY, radiusX2, radiusY2, radiusZ2));
                        }
                    }
                }
            }
        }
        return info;
    }

    /** Applies all applicable clusters to the current coordinates. */
    private void applyClusters(ChunkPrimer primer, List<ClusterInfo> info, int x, int y, int z, double actualX, double actualZ) {
        for (ClusterInfo cluster : info) {
            final BlockPos origin = cluster.getCenter();
            final double distX = actualX - origin.getX();
            final double distY = y - origin.getY();
            final double distZ = actualZ - origin.getZ();
            final double distX2 = distX * distX;
            final double distY2 = distY * distY;
            final double distZ2 = distZ * distZ;

            // Ensure that we're within the sphere.
            if (distX2 / cluster.getRadiusX2() + distY2 / cluster.getRadiusY2() + distZ2 / cluster.getRadiusZ2() <= 1) {
                primer.setBlockState(x, y, z, cluster.getCluster().getState());
            }
        }
    }

    /** Generates all possible stone layers in the current chunk. */
    private void generateLayers(ChunkPrimer primer, int chunkX, int chunkZ) {
        for (int x = 0; x < 16; x++) {
            final float actualX = x + (chunkX * 16);
            for (int z = 0; z < 16; z++) {
                final float actualZ = z + (chunkZ * 16);
                int y = 0;

                for (StoneLayer layer : settings.decorators.stoneLayers) {
                    final int noise = (int) layer.getNoise().GetAdjustedNoise(actualX, actualZ);
                    final int maxHeight = layer.getMaxHeight() + noise;
                    final IBlockState state = layer.getState();

                    for (; y < maxHeight; y++) {
                        final IBlockState original = primer.getBlockState(x, y, z);
                        // Only decorate actual stone.
                        if (original.equals(BLK_STONE)) {
                            primer.setBlockState(x, y, z, state);
                        }
                    }
                }
            }
        }
    }

    /** Replaces all blocks inside of this section. */
    private void replaceSection(Random rand, PrimerData data, TunnelSectionInfo section) {
        section.run(pos -> {
            final int x = pos.getX();
            final int y = pos.getY();
            final int z = pos.getZ();
            final boolean isTopBlock = isTopBlock(data.p, x, y, z, data.chunkX, data.chunkZ);
            replaceBlock(rand, data.p, x, y, z, data.chunkX, data.chunkZ, isTopBlock);
        });
    }

    /** Decorates all blocks inside of this section. */
    private void decorateSection(Random rand, PrimerData data, TunnelSectionInfo section) {
        section.run(pos -> {
            final int x = pos.getX();
            final int y = pos.getY();
            final int z = pos.getZ();
            decorateBlock(rand, data.p, x, y, z, data.chunkX, data.chunkZ);
        });
    }

    /**
     * Whether the block at this location is the biome's topBlock.
     * Accounts? for a bug in vanilla that checks for grass in
     * biomes with sand. May remove anyway.
     */
    private boolean isTopBlock(ChunkPrimer primer, int x, int y, int z, int chunkX, int chunkZ) {
        Biome biome = world.getBiome(absoluteCoords(chunkX, chunkZ));
        IBlockState state = primer.getBlockState(x, y, z);
        return isExceptionBiome(biome) ? state.getBlock() == Blocks.GRASS : state.getBlock() == biome.topBlock;
    }

    /** From Forge docs: helps imitate vanilla bugs? */
    private boolean isExceptionBiome(Biome biome) {
        return biome.equals(Biomes.BEACH) || biome.equals(Biomes.DESERT);
    }

    /**
     * Mod of {~~@link net.minecraft.world.gen.MapGenCaves#digBlock} by
     * PersonTheCat. Allows alternatives of air to be randomly placed.
     *
     * @param primer   Block data array
     * @param x        local X position
     * @param y        local Y position
     * @param z        local Z position
     * @param chunkX   Chunk X position
     * @param chunkZ   Chunk Y position
     * @param foundTop True if we've encountered the biome's top block.
     *                 Ideally, if we've broken the surface.
     */
    private void replaceBlock(Random rand, ChunkPrimer primer, int x, int y, int z, int chunkX, int chunkZ, boolean foundTop) {
        final Biome biome = world.getBiome(absoluteCoords(chunkX, chunkZ));
        final IBlockState state = primer.getBlockState(x, y, z);
        final IBlockState top = biome.topBlock;
        final IBlockState filler = biome.fillerBlock;
        final int yDown = y - 1;

        if (canReplaceBlock(state) || state.equals(top) || state.equals(filler)) {
            // This must be a vanilla bug?
            if (foundTop && primer.getBlockState(x, yDown, z).equals(filler)) {
                primer.setBlockState(x, yDown, z, top);
            }
            for (CaveBlock block : settings.decorators.caveBlocks) {
                if (block.canGenerate(x, y, z, chunkX, chunkZ)) {
                    if (rand.nextFloat() <= block.getChance()) {
                        primer.setBlockState(x, y, z, block.getFillBlock());
                        return;
                    }
                }
            }
            primer.setBlockState(x, y, z, BLK_AIR);
        }
    }

    /** Conditionally replaces the current block with blocks from this generator's WallDecorators. */
    private void decorateBlock(Random rand, ChunkPrimer primer, int x, int y, int z, int chunkX, int chunkZ) {
        if (decorateVertical(rand, primer, x, y, z, chunkX, chunkZ, true)) {
            return;
        } else if (decorateVertical(rand, primer, x, y, z, chunkX, chunkZ, false)) {
            return;
        }
        decorateHorizontal(rand, primer, x, y, z, chunkX, chunkZ);
    }

    private boolean decorateVertical(Random rand, ChunkPrimer primer, int x, int y, int z, int chunkX, int chunkZ, boolean up) {
        // Up vs. down things.
        DecoratorSettings cfg = settings.decorators;
        int offset = up ? y + 1 : y - 1;
        WallDecorator[] decorators = up ? cfg.ceilingDecorators : cfg.floorDecorators;
        for (WallDecorator decorator : decorators) {
            // The candidate blockstate to be tested / replaced.
            IBlockState candidate = primer.getBlockState(x, offset, z);
            // Ignore air blocks.
            if (candidate.getMaterial().equals(Material.AIR)) {
                return false;
            }
            // Filter for valid generators at this position and for this blockstate.
            if (decorator.canGenerate(rand, candidate, x, y, z, chunkX, chunkZ)) {
                // Place block -> return success if original was replaced.
                if (decorator.decidePlace(primer, x, y, z, x, offset, z)) {
                    return true;
                } // else continue iterating through decorators.
            }
        }
        // Everything failed.
        return false;
    }

    private void decorateHorizontal(Random rand, ChunkPrimer primer, int x, int y, int z, int chunkX, int chunkZ ) {
        // Avoid repeated calculations.
        List<WallDecorator> testedDecorators = pretestDecorators(rand, x, y, z, chunkX, chunkZ);
        // We'll need to reiterate through those decorators below.
        for (BlockPos pos : nsew(x, y, z)) {
            if (!areCoordsInChunk(pos.getX(), pos.getZ())) {
                continue;
            }
            IBlockState candidate = primer.getBlockState(pos.getX(), pos.getY(), pos.getZ());
            // Ignore air blocks.
            if (candidate.getMaterial().equals(Material.AIR)) {
                continue;
            }
            for (WallDecorator decorator : testedDecorators) {
                if (decorator.matchesBlock(candidate)) {
                    // Place block -> return success if original was replaced.
                    if (decorator.decidePlace(primer, x, y, z, pos.getX(), pos.getY(), pos.getZ())) {
                        return;
                    } // else continue iterating through decorators.
                }
            }
        }
    }

    private List<WallDecorator> pretestDecorators(Random rand, int x, int y, int z, int chunkX, int chunkZ) {
        List<WallDecorator> testedDecorators = new ArrayList<>();
        for (WallDecorator decorator : settings.decorators.wallDecorators) {
            // Filter for valid generators at this position only.
            if (decorator.canGenerate(rand, x, y, z, chunkX, chunkZ)) {
                testedDecorators.add(decorator);
            }
        }
        return testedDecorators;
    }

    private BlockPos[] nsew(int x, int y, int z) {
        return new BlockPos[] {
            new BlockPos(x, y, z - 1), // North
            new BlockPos(x, y, z + 1), // South
            new BlockPos(x + 1, y, z), // East
            new BlockPos(x - 1, y, z)  // West
        };
    }

    private boolean areCoordsInChunk(int x, int z) {
        return x > -1 && x < 16 && z > -1 && z < 16;
    }

    /** Returns whether the input blockstate is on the list of replaceable blocks. */
    private boolean canReplaceBlock(IBlockState state) {
        if (state.getMaterial().equals(Material.WATER)){
            return false;
        }
        return find(settings.replaceable, blk -> blk.equals(state))
            .isPresent();
    }
}