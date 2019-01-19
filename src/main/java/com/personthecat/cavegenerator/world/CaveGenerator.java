package com.personthecat.cavegenerator.world;

import com.personthecat.cavegenerator.util.ScalableFloat;
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
import com.personthecat.cavegenerator.world.GeneratorSettings.DecoratorSettings;
import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.personthecat.cavegenerator.util.CommonMethods.*;

public class CaveGenerator {
    /** A few convenient values. */
    private static final float PI_TIMES_2 = (float) (Math.PI * 2);
    private static final float PI_OVER_2 = (float) (Math.PI / 2);
    private static final IBlockState BLK_AIR = Blocks.AIR.getDefaultState();
    private static final IBlockState BLK_WATER = Blocks.WATER.getDefaultState();
    private static final IBlockState BLK_STONE = Blocks.STONE.getDefaultState();

    /** Mandatory fields that must be initialized by the constructor */
    private final World world;
    // The elusive public final field. It's just too convenient
    // in this context.
    public final GeneratorSettings settings;

    /** Information regarding the stone clusters to be spawned. */
    private final List<ClusterInfo> finalClusters = new ArrayList<>();
    /** A series of random floats between 1-4 for distorting ravine walls. */
    private final float[] mut = new float[256];

    public CaveGenerator(World world, GeneratorSettings settings) {
        this.world = world;
        this.settings = settings;
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

    /** Returns whether the generator has any wall decorators. */
    public boolean hasLocalWallDecorators() {
        return settings.decorators.wallDecorators.length > 0;
    }

    /**
     * Returns whether this generator is both enabled and has wall decorators, indicating
     * that a special chunk border correction process will need to take place.
     */
    public boolean requiresCorrections() {
        return enabled() && hasLocalWallDecorators();
    }

    /** Starts a tunnel system between the input chunk coordinates. */
    public void startTunnelSystem(Random rand, int chunkX, int chunkZ, int originalX, int originalZ, ChunkPrimer primer) {
        final int frequency = getTunnelFrequency(rand);
        for (int i = 0; i < frequency; i++) {
            // Retrieve the height parameters from the settings.
            final int minHeight = settings.tunnels.minHeight;
            final int maxHeight = settings.tunnels.maxHeight;
            final int heightDiff = maxHeight - minHeight;

            // Get random coordinates in destination chunk.
            final double x = (double) ((chunkX * 16) + rand.nextInt(16));
            final double y = (double) (rand.nextInt(rand.nextInt(heightDiff) + minHeight));
            final double z = (double) ((chunkZ * 16) + rand.nextInt(16));

            // Determine the number of branches to spawn.
            int branches = 1;
            if (rand.nextInt(settings.tunnels.spawnInSystemInverseChance) == 0) {
                // Add a room at the center? of the system.
                branches += rand.nextInt(4); // To-do: make this variable.
            }

            for (int j = 0; j < branches; j++) {
                float angleXZ = settings.tunnels.angleXZ.startVal;
                float angleY = settings.tunnels.angleY.startVal;
                float scale = settings.tunnels.scale.startVal;

                angleXZ += settings.tunnels.angleXZ.startValRandFactor * (rand.nextFloat() * PI_TIMES_2);
                angleY += settings.tunnels.angleY.startValRandFactor * (rand.nextFloat() - 0.50f);
                scale += settings.tunnels.scale.startValRandFactor * (rand.nextFloat() * 2.00f + rand.nextFloat());

                // Per-vanilla: this randomly increases the size.
                if (rand.nextInt(10) == 0) {
                    addRoom(rand, originalX, originalZ, primer, x, y, z);
                    // Exact equation might be silly. Maintains seed fidelity.
                    scale *= rand.nextFloat() * rand.nextFloat() * 3.00f + 1.00f;
                }

                final int distance = settings.tunnels.startingDistance;
                final float scaleY = settings.tunnels.scaleY.startVal;

                addTunnel(rand.nextLong(), originalX, originalZ, primer, x, y, z, scale, angleXZ, angleY, 0, distance, scaleY);
            }
        }
    }

    /** Starts a ravine between the input chunk coordinates. */
    public void startRavine(Random rand, int chunkX, int chunkZ, int originalX, int originalZ, ChunkPrimer primer) {
        // Retrieve the height parameters from the settings.
        final int minHeight = settings.ravines.minHeight;
        final int maxHeight = settings.ravines.maxHeight;
        final int heightDiff = maxHeight - minHeight;

        // Get random coordinates in destination chunk.
        final double x = (double) ((chunkX * 16) + rand.nextInt(16));
        // To-do: verify these numbers.
        final double y = (double) (rand.nextInt(rand.nextInt(maxHeight) + 8) + heightDiff);
        final double z = (double) ((chunkZ * 16) + rand.nextInt(16));

        float angleXZ = settings.ravines.angleXZ.startVal;
        float angleY = settings.ravines.angleY.startVal;
        float scale = settings.ravines.scale.startVal;

        // Randomly orient the angle.
        angleXZ += rand.nextFloat() * PI_TIMES_2;
        angleY += settings.ravines.angleY.startValRandFactor * (rand.nextFloat() - 0.50f);
        // Randomly adjust the scale.
        scale += settings.ravines.scale.startValRandFactor * (rand.nextFloat() * 2.00f + rand.nextFloat());

        final int distance = settings.ravines.startingDistance;
        final float scaleY = settings.ravines.scaleY.startVal;

        addRavine(rand.nextLong(), originalX, originalZ, primer, x, y, z, scale, angleXZ, angleY, 0, distance, scaleY);
    }

    /**
     * Mod of {~~@link net.minecraft.world.gen.MapGenCaves#addTunnel} by PersonTheCat.
     * The is the basic function responsible for spawning a chained sequence of
     * angled spheres in the world. Supports object-specific replacement of most
     * variables, as well as a few new variables for controlling shapes, adding
     * noise-base alternatives to air, and wall decorations.
     *
     * @param seed      The world's seed. Use to create a local Random object for
     *                  regen parity.
     * @param originalX The X coordinate of the starting chunk.
     * @param originalZ The Z coordinate of the starting chunk.
     * @param primer    Contains data about the chunk being generated.
     * @param x         The X coordinate of the destination block.
     * @param y         The Y coordinate of the destination block.
     * @param z         The Z coordinate of the destination block.
     * @param scale     The diameter? in blocks of the current tunnel.
     * @param angleXZ   The horizontal angle in radians for starting this tunnel.
     * @param angleY    The vertical angle in radians for starting this tunnel.
     * @param position  A measure of progress until `distance`.
     * @param distance  The length of the tunnel. 0 -> # ( 132 to 176 ).
     * @param scaleY    A vertical multiple of scale. 1.0 -> same as scale.
     */
    private void addTunnel(
        long seed,
        int originalX,
        int originalZ,
        ChunkPrimer primer,
        double x,
        double y,
        double z,
        float scale,
        float angleXZ,
        float angleY,
        int position,
        int distance,
        float scaleY
    ) {
        // The amount to alter angle(XZ/Y) per-segment.
        float twistXZ = settings.tunnels.twistXZ.startVal;
        float twistY = settings.tunnels.twistY.startVal;
        // The center of the current chunk;
        final double centerX = originalX * 16 + 8;
        final double centerZ = originalZ * 16 + 8;
        // Initialize the local Random object.
        final Random rand = new Random(seed);
        // A second rand to avoid breaking seeds;
        final Random rand2 = new Random(seed);
        distance = getTunnelDistance(rand, distance);
        // Determine where to place branches, if applicable.
        final int randomBranchIndex = rand.nextInt(distance / 2) + distance / 4;
        final boolean randomNoiseCorrection = rand.nextInt(6) == 0;

        for (int currentPos = position; currentPos < distance; currentPos++) {
            // Determine the radius by `scale`.
            final double radiusXZ = 1.5D + (MathHelper.sin(currentPos * (float) Math.PI / distance) * scale);
            final double radiusY = radiusXZ * scaleY;
            // To-do: verify this function's purpose.
            final float cos = MathHelper.cos(angleY);
            final float sin = MathHelper.sin(angleY);
            x += MathHelper.cos(angleXZ) * cos;
            y += sin;
            z += MathHelper.sin(angleXZ) * cos;
            // Vertical noise control.
            if (settings.tunnels.noiseYReduction) {
                angleY *= randomNoiseCorrection ? 0.92f : 0.70f;
            }
            // Adjust the angle based on current twist(XZ/Y). twist
            // will have been recalculated on subsequent iterations.
            // The potency of twist is reduced immediately.
            angleXZ += twistXZ * 0.1f;
            angleY += twistY * 0.1f;
            // Rotates the beginning of the chain around the end.
            twistY = adjustTwist(twistY, rand, settings.tunnels.twistY);
            // Positive is counterclockwise, negative is clockwise.
            twistXZ = adjustTwist(twistXZ, rand, settings.tunnels.twistXZ);
            // Adjust the scale each iteration. This doesn't? happen
            // in vanilla, so a separate Random object is used in
            // order to avoid breaking seeds, as much as possible.
            scale = adjustScale(scale, rand2, settings.tunnels.scale);
            scaleY = adjustScale(scaleY, rand2, settings.tunnels.scaleY);

            // Add branches.
            if (scale > 1.00f && distance > 0 && currentPos == randomBranchIndex) {
                if (settings.tunnels.resizeBranches) {
                    // In vanilla, tunnels are resized when branching.
                    addTunnel(rand.nextLong(), originalX, originalZ, primer, x, y, z, rand.nextFloat() * 0.5F + 0.5F,
                        angleXZ - PI_OVER_2, angleY / 3.0F, currentPos, distance, 1.00f);
                    addTunnel(rand.nextLong(), originalX, originalZ, primer, x, y, z, rand.nextFloat() * 0.5F + 0.5F,
                        angleXZ + PI_OVER_2, angleY / 3.0F, currentPos, distance, 1.00f);
                } else {
                    // Continue with the same size (not vanilla).
                    addTunnel(rand.nextLong(), originalX, originalZ, primer, x, y, z, scale,
                        angleXZ - PI_OVER_2, angleY / 3.0F, currentPos, distance, scaleY);
                    addTunnel(rand.nextLong(), originalX, originalZ, primer, x, y, z, scale,
                        angleXZ + PI_OVER_2, angleY / 3.0F, currentPos, distance, scaleY);
                }
                return;
            }
            // Occasionally do nothing when no branch is placed.
            // Not sure why we came this far.
            if (rand.nextInt(4) == 0) {
                continue;
            }
            // Make sure we haven't travelled too far?
            if (travelledTooFar(x, centerX, z, centerZ, distance, currentPos, scale)) {
                // Leaving a comment here to remember the 50% increase
                // in generation time caused by using `continue`
                // instead of `return` here. Lest we never forget.
                return;
            }
            // Make sure we're inside of the sphere?
            double diameterXZ = radiusXZ * 2.0;
            if (x >= centerX - 16.0 - diameterXZ &&
                z >= centerZ - 16.0 - diameterXZ &&
                x <= centerX + 16.0 + diameterXZ &&
                z <= centerZ + 16.0 + diameterXZ
            ) {
                // Calculate all of the positions in the section.
                // We'll be using them multiple times.
                final TunnelSectionInfo sectionInfo = new TunnelSectionInfo(x, y, z, radiusXZ, radiusY, originalX, originalZ);
                sectionInfo.calculate();

                // If we need to test this section for water -> is there water?
                if (!(shouldTestForWater(sectionInfo.getHighestY()) && testForWater(primer, sectionInfo))) {
                    // Generate the actual sphere.
                    replaceSection(rand2, primer, sectionInfo, originalX, originalZ);
                    // We need to generate twice; once to create walls,
                    // and once again to decorate those walls.
                    if (hasLocalDecorators()) {
                        // Decorate the sphere.
                        decorateSection(rand2, primer, sectionInfo, false, originalX, originalZ);
                    }
                }
            }
        }
    }

    /**
     * Variant of addTunnel() which extracts the features dedicated to generating
     * single, symmetrical spheres, known internally as "rooms." This may be
     * slightly more redundant, but it should increase the algorithm's readability.
     */
    private void addRoom(Random master, int originalX, int originalZ, ChunkPrimer primer, double x, double y, double z) {
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
        final TunnelSectionInfo sectionInfo = new TunnelSectionInfo(x, y, z, radiusXZ, radiusY, originalX, originalZ);
        sectionInfo.calculate();

        // If we need to test this section for water -> is there water?
        if (!(shouldTestForWater(sectionInfo.getHighestY()) && testForWater(primer, sectionInfo))) {
            // Generate the actual sphere.
            replaceSection(rand, primer, sectionInfo, originalX, originalZ);
            // We need to generate twice; once to create walls,
            // and once again to decorate those walls.
            if (hasLocalDecorators()) {
                // Decorate the sphere.
                decorateSection(rand, primer, sectionInfo, false, originalX, originalZ);
            }
        }
    }

    /**
     * Variant of addTunnel() and {~~@link net.minecraft.world.gen.MapGenRavine#addTunnel}
     * which randomly alters the horizontal radius based on `mut`, a buffer of random
     * values between 1-4, stored above. The difference in scale typically observed in
     * ravines is the result of arguments input to this function.
     */
    private void addRavine(
        long seed,
        int originalX,
        int originalZ,
        ChunkPrimer primer,
        double x,
        double y,
        double z,
        float scale,
        float angleXZ,
        float angleY,
        int position,
        int distance,
        float scaleY
    ) {
        // The amount to alter angle(XZ/Y) per-segment.
        float twistXZ = settings.ravines.twistXZ.startVal;
        float twistY = settings.ravines.twistY.startVal;
        // The center of the current chunk;
        final double centerX = originalX * 16 + 8;
        final double centerZ = originalZ * 16 + 8;
        // Initialize the local Random object.
        final Random rand = new Random(seed);
        // A second rand to avoid breaking seeds;
        final Random rand2 = new Random(seed);
        distance = getTunnelDistance(rand, distance);
        // Update the ravine wall mutations to be unique to this chasm.
        updateMutations(rand);

        for (int currentPos = position; currentPos < distance; currentPos++) {
            // Determine the radius by `scale`.
            final double radiusXZ = 1.5D + (MathHelper.sin(currentPos * (float) Math.PI / distance) * scale);
            final double radiusY = radiusXZ * scaleY;
            // To-do: verify this function's purpose.
            final float cos = MathHelper.cos(angleY);
            final float sin = MathHelper.sin(angleY);
            x += MathHelper.cos(angleXZ) * cos;
            y += sin;
            z += MathHelper.sin(angleXZ) * cos;
            // Vertical noise control.
            angleY *= settings.ravines.noiseYFactor;
            // Adjust the angle based on current twist(XZ/Y). twist
            // will have been recalculated on subsequent iterations.
            // The potency of twist is reduced immediately.
            angleXZ += twistXZ * 0.05f; // Adjustments matter less for ravines
            angleY += twistY * 0.05f;
            // Rotates the beginning of the chain around the end.
            twistY = adjustTwist(twistY, rand, settings.ravines.twistY);
            // Positive is counterclockwise, negative is clockwise.
            twistXZ = adjustTwist(twistXZ, rand, settings.ravines.twistXZ);
            // Adjust the scale each iteration. This doesn't? happen
            // in vanilla, so a separate Random object is used in
            // order to avoid breaking seeds, as much as possible.
            scale = adjustScale(scale, rand2, settings.ravines.scale);
            scaleY = adjustScale(scaleY, rand2, settings.ravines.scaleY);

            // Not sure why we came this far.
            // Even more so in the case of ravines.
            if (rand.nextInt(4) == 0) {
                continue;
            }
            // Make sure we haven't travelled too far?
            if (travelledTooFar(x, centerX, z, centerZ, distance, currentPos, scale)) {
                return;
            }
            // Make sure we're inside of the sphere?
            double diameterXZ = radiusXZ * 2.0;
            if (x >= centerX - 16.0 - diameterXZ &&
                z >= centerZ - 16.0 - diameterXZ &&
                x <= centerX + 16.0 + diameterXZ &&
                z <= centerZ + 16.0 + diameterXZ
            ) {
                // Calculate all of the positions in the section.
                // We'll be using them multiple times.
                final TunnelSectionInfo sectionInfo = new TunnelSectionInfo(x, y, z, radiusXZ, radiusY, originalX, originalZ);
                sectionInfo.calculateMutated(mut);

                // If we need to test this section for water -> is there water?
                if (!(shouldTestForWater(sectionInfo.getHighestY()) && testForWater(primer, sectionInfo))) {
                    // Generate the actual sphere.
                    replaceSection(rand2, primer, sectionInfo, originalX, originalZ);
                    // We need to generate twice; once to create walls,
                    // and once again to decorate those walls.
                    if (hasLocalDecorators()) {
                        // Decorate the sphere.
                        decorateSection(rand2, primer, sectionInfo, true, originalX, originalZ);
                    }
                }
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
        final int chance = settings.tunnels.spawnIsolatedInverseChance;
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

    /** Updates the value of `original` based on the input settings. */
    private float adjustTwist(float original, Random rand, ScalableFloat f) {
        original = (float) Math.pow(original, f.exponent);
        original *= f.factor;
        original += f.randFactor * (rand.nextFloat() - rand.nextFloat()) * rand.nextFloat();
        return original;
    }

    /** Updates the value of `original` based on the input settings. */
    private float adjustScale(float original, Random rand, ScalableFloat f) {
        original = (float) Math.pow(original, f.exponent);
        original *= f.factor;
        original += f.randFactor * (rand.nextFloat() - 0.5F);
        if (original < 0) original = 0;
        return original;
    }

    /** Used to produce the variations in horizontal scale seen in ravines. */
    private void updateMutations(Random rand) {
        if (settings.ravines.useWallNoise) {
            updateMutationsNoise(rand);
        } else {
            updateMutationsVanilla(rand);
        }
    }

    /** The effectively vanilla implementation of updateMutations(). */
    private void updateMutationsVanilla(Random rand) {
        float val = 1.0f;
        for (int i = 0; i < 256; i++) {
            if (i == 0 || rand.nextInt(3) == 0) {
                val = rand.nextFloat() * rand.nextFloat() + 1.0f;
            }
            mut[i] = val * val;
        }
    }

    /** Variant of updateMutations() which produces aberrations using a noise generator. */
    private void updateMutationsNoise(Random rand) {
        FastNoise noise = settings.ravines.wallNoise.getGenerator(rand.nextInt());
        for (int i = 0; i < 256; i++) {
            mut[i] = noise.GetAdjustedNoise(0, i);
        }
    }

    /** To-do: Better commentary. */
    private boolean travelledTooFar(double x, double centerX, double z, double centerZ, int distance, int currentPos, float scale) {
        final double fromCenterX = x - centerX;
        final double fromCenterZ = z - centerZ;
        // Name? Is this related to Y?
        final double distanceRemaining = distance - currentPos;
        final double adjustedScale = scale + 18.00;

        final double fromCenterX2 = fromCenterX * fromCenterX;
        final double fromCenterZ2 = fromCenterZ * fromCenterZ;
        final double distanceRemaining2 = distanceRemaining * distanceRemaining;
        final double adjustedScale2 = adjustedScale * adjustedScale;

        return (fromCenterX2 + fromCenterZ2 - distanceRemaining2) > adjustedScale2;
    }

    /**
     * Returns whether a test should be run to determine whether water is
     * found and stop generating.
     */
    private boolean shouldTestForWater(int highestY) {
        for (CaveBlocks filler : settings.decorators.caveBlocks) {
            if (filler.getFillBlock().equals(BLK_WATER) &&
                highestY <= filler.getMaxHeight() + 10) { // A little wiggle room.
                return true;
            }
        }
        return false;
    }

    /** Determines whether any water exists in the current section. */
    private boolean testForWater(ChunkPrimer primer, TunnelSectionInfo info) {
        return info.test(pos ->
            primer.getBlockState(pos.getX(), pos.getY(), pos.getZ()).equals(BLK_WATER)
        );
    }

    /** Replaces all blocks inside of this section. */
    private void replaceSection(Random rand, ChunkPrimer primer, TunnelSectionInfo info, int chunkX, int chunkZ) {
        info.run(pos -> {
            final int x = pos.getX();
            final int y = pos.getY();
            final int z = pos.getZ();
            final boolean isTopBlock = isTopBlock(primer, x, y, z, chunkX, chunkZ);
            replaceBlock(rand, primer, x, y, z, chunkX, chunkZ, isTopBlock);
        });
    }

    /** Decoirates all blocks inside of this section. */
    private void decorateSection(Random rand, ChunkPrimer primer, TunnelSectionInfo info, boolean useMut, int chunkX, int chunkZ) {
        info.run(pos -> {
            final int x = pos.getX();
            final int y = pos.getY();
            final int z = pos.getZ();
            decorateBlock(rand, primer, info, useMut, x, y, z, chunkX, chunkZ);
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
    private boolean replaceBlock(Random rand, ChunkPrimer primer, int x, int y, int z, int chunkX, int chunkZ, boolean foundTop) {
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
            for (CaveBlocks block : settings.decorators.caveBlocks) {
                if (block.canGenerate(x, y, z, chunkX, chunkZ)) {
                    if (rand.nextFloat() * 100 <= block.getChance()) {
                        primer.setBlockState(x, y, z, block.getFillBlock());
                        return true;
                    }
                }
            }
            primer.setBlockState(x, y, z, BLK_AIR);
            return true;
        }
        return false;
    }

    /** Conditionally replaces the current block with blocks from this generator's WallDecorators. */
    private boolean decorateBlock(
        Random rand,
        ChunkPrimer primer,
        TunnelSectionInfo info,
        boolean useMut,
        int x,
        int y,
        int z,
        int chunkX,
        int chunkZ
    ) {
        if (decorateVertical(rand, primer, x, y, z, chunkX, chunkZ, true)) {
            return true;
        } else if (decorateVertical(rand, primer, x, y, z, chunkX, chunkZ, false)) {
            return true;
        }
        return false; // decorateHorizontal(rand, primer, info, useMut, x, y, z, chunkX, chunkZ);
    }

    private boolean decorateVertical(Random rand, ChunkPrimer primer, int x, int y, int z, int chunkX, int chunkZ, boolean up) {
        // Up vs. down things.
        DecoratorSettings cfg = settings.decorators;
        int offset = up ? y + 1 : y - 1;
        WallDecorators[] decorators = up ? cfg.ceilingDecorators : cfg.floorDecorators;
        // The candidate blockstate to be tested / replaced.
        IBlockState candidate = primer.getBlockState(x, offset, z);
        // Ignore air blocks.
        if (candidate.getMaterial().equals(Material.AIR)) {
            return false;
        }
        for (WallDecorators decorator : decorators) {
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

    private boolean decorateHorizontal(
        Random rand,
        ChunkPrimer primer,
        TunnelSectionInfo info,
        boolean useMut,
        int x,
        int y,
        int z,
        int chunkX,
        int chunkZ
    ) {
        // Avoid repeated calculations.
        List<WallDecorators> testedDecorators = pretestDecorators(rand, x, y, z, chunkX, chunkZ);
        // We'll need to reiterate through those decorators below.
        for (BlockPos pos : nsew(x, y, z)) {
            IBlockState candidate = safeGetBlock(primer, info, useMut, pos.getX(), pos.getY(), pos.getZ());
            // Ignore air blocks.
            if (candidate.getMaterial().equals(Material.AIR)) {
                return false;
            }
            for (WallDecorators decorator : testedDecorators) {
                if (decorator.matchesBlock(candidate)) {
                    // Place block -> return success if original was replaced.
                    if (decorator.decidePlace(primer, x, y, z, pos.getX(), pos.getY(), pos.getZ())) {
                        return true;
                    } // else continue iterating through decorators.
                }
            }
        }
        // Everything failed.
        return false;
    }

    private List<WallDecorators> pretestDecorators(Random rand, int x, int y, int z, int chunkX, int chunkZ) {
        List<WallDecorators> testedDecorators = new ArrayList<>();
        for (WallDecorators decorator : settings.decorators.wallDecorators) {
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

    /**
     * Attempts to retrieve a block from the input ChunkPrimer. Returns air or stone if the
     * coordinates would be outside of the current chunk.
     */
    private IBlockState safeGetBlock(ChunkPrimer primer, TunnelSectionInfo info, boolean useMut, int x, int y, int z) {
        // If the coordinates are outside of the current chunk,
        // there is no way to accurately determine which block
        // will exist at this coordinate. That would need to be
        // handled on a later event, causing a substantial loss
        // in performance.
        if (areCoordsInChunk(x, z)) {
            return primer.getBlockState(x, y, z);
        } else if (useMut) {
            if (info.testCoords(x, y, z, mut)){
                return BLK_AIR;
            }
            return BLK_STONE;
        } else if (info.testCoords(x, y, z)) {
            return BLK_AIR;
        }
        return BLK_STONE;
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