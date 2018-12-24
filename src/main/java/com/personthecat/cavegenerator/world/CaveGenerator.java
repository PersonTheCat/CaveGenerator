package com.personthecat.cavegenerator.world;

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
import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

import static com.personthecat.cavegenerator.util.CommonMethods.*;

public class CaveGenerator {
    /** A few convenient values. */
    private static final float PI_TIMES_2 = (float) (Math.PI * 2);
    private static final float PI_OVER_2 = (float) (Math.PI / 2);
    private static final IBlockState BLK_AIR = Blocks.AIR.getDefaultState();
    private static final IBlockState BLK_WATER = Blocks.WATER.getDefaultState();

    /** Mandatory fields that must be initialized by the constructor */
    private final World world;
    // The elusive public final field. It's just too convenient
    // in this context.
    public final GeneratorSettings settings;

    /** Information regarding the stone clusters to be spawned. */
    private final List<ClusterInfo> finalClusters = new ArrayList<>();
    /** A series of random floats between 1-4 for distorting ravine walls. */
    private final float[] mut = new float[1024];

    public CaveGenerator(World world, GeneratorSettings settings) {
        this.world = world;
        this.settings = settings;
    }

    /** Returns whether the generator is enabled globally. */
    public boolean enabled() {
        return settings.enabled;
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

    private boolean validDimension(int dim) {
        return settings.conditions.dimensions.length == 0 ||
            ArrayUtils.contains(settings.conditions.dimensions, dim);
    }

    private boolean validBiome(Biome biome) {
        return settings.conditions.biomes.length == 0 ||
            ArrayUtils.contains(settings.conditions.biomes, biome);
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

    /**
     * Starts a tunnel system between the input chunk coordinates.
     */
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

            for (int j = 0; j< branches; j++) {
                float angleXZ = settings.tunnels.startingAngleXZ;
                float angleY = settings.tunnels.startingAngleY;
                float scale = settings.tunnels.startingScale;

                angleXZ += settings.tunnels.startingAngleXZRandFactor * (rand.nextFloat() * PI_TIMES_2);
                angleY += settings.tunnels.startingAngleYRandFactor * (rand.nextFloat() - 0.50f);
                scale += settings.tunnels.startingScaleRandFactor * (rand.nextFloat() * 2.00f + rand.nextFloat());

                // Per-vanilla: this randomly increases the size.
                if (rand.nextInt(10) == 0) {
                    // Exact equation might be silly. Maintains seed fidelity.
                    scale *= rand.nextFloat() * rand.nextFloat() * 3.00f + 1.00f;
                }

                final int distance = settings.tunnels.startingDistance;
                final float scaleY = settings.tunnels.startingScaleY;

                addTunnel(rand.nextLong(), originalX, originalZ, primer, x, y, z, scale, angleXZ, angleY, 0, distance, scaleY);
            }
        }
    }

    /**
     * Starts a room between the input chunk coordinates.
     */
    public void startRoom(Random rand, int chunkX, int chunkZ, int originalX, int originalZ, ChunkPrimer primer) {

    }

    /**
     * Starts a ravine between the input chunk coordinates.
     */
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

        float angleXZ = settings.ravines.startingAngleXZ;
        float angleY = settings.ravines.startingAngleY;
        float scale = settings.ravines.startingScale;

        // Randomly orient the angle.
        angleXZ += rand.nextFloat() * PI_TIMES_2;
        angleY += settings.ravines.startingAngleYRandFactor * (rand.nextFloat() - 0.50f);
        // Randomly adjust the scale.
        scale += settings.ravines.startingScaleRandFactor * (rand.nextFloat() * 2.00f + rand.nextFloat());

        final int distance = settings.ravines.startingDistance;
        final float scaleY = settings.ravines.startingScaleY;

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
        float twistXZ = settings.tunnels.startingTwistXZ;
        float twistY = settings.tunnels.startingTwistY;
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
            twistY = adjustTwist(twistY, rand,
                settings.tunnels.twistYExponent, settings.tunnels.twistYFactor, settings.tunnels.twistYRandFactor);
            // Positive is counterclockwise, negative is clockwise.
            twistXZ = adjustTwist(twistXZ, rand,
                settings.tunnels.twistXZExponent, settings.tunnels.twistXZFactor, settings.tunnels.twistXZRandFactor);
            // Adjust the scale each iteration. This doesn't? happen
            // in vanilla, so a separate Random object is used in
            // order to avoid breaking seeds, as much as possible.
            scale = adjustScale(scale, rand2,
                settings.tunnels.scaleExponent, settings.tunnels.scaleFactor, settings.tunnels.scaleRandFactor);
            scaleY = adjustScale(scaleY, rand2,
                settings.tunnels.scaleYExponent, settings.tunnels.scaleYFactor, settings.tunnels.scaleYRandFactor);

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
                continue;
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
                    replaceSection(primer, originalX, originalZ, sectionInfo);
                    // We need to generate twice; once to create walls,
                    // and once again to decorate those walls.
                    if (hasLocalWallDecorators()) {
                        // Decorate the sphere.
                        // decorateSection(primer, originalX, originalZ, sectionInfo);
                    }
                }
            }
        }
    }

//    /** Makes sure the resulting value stays within chunk bounds. */
//    private static int applyLimitXZ(int xz) {
//        return xz < 0 ? 0 : xz > 16 ? 16 : xz;
//    }
//
//    /** Makes sure the resulting value stays between y = 1 & y = 248 */
//    private static int applyLimitY(int y) {
//        return y < 1 ? 1 : y > 248 ? 248 : y;
//    }
//
//    private void replaceSection(ChunkPrimer primer, double radiusXZ, double radiusY, int chunkX, int chunkZ, int x1, int x2, double posX, int y1, int y2, double posY, int z1, int z2, double posZ)
//    {
//        tunnelSection(chunkX, chunkZ, radiusXZ, radiusY, x1, x2, posX, y1, y2, posY, z1, z2, posZ, pos ->
//        {
//            int x = pos.getX(), y = pos.getY(), z = pos.getZ();
//
//            boolean isTopBlock = isTopBlock(primer, x, y, z, chunkX, chunkZ);
//
//            replaceBlock(primer, x, y, z, chunkX, chunkZ, isTopBlock);
//        });
//    }
//
//    private void tunnelSection(int chunkX, int chunkZ, double radiusXZ, double radiusY, int x1, int x2, double posX, int y1, int y2, double posY, int z1, int z2, double posZ, Consumer<BlockPos> function)
//    {
//        for (int x = x1; x < x2; x++)
//        {
//            double finalX = ((x + chunkX * 16) + 0.5 - posX) / radiusXZ;
//
//            for (int z = z1; z < z2; z++)
//            {
//                double finalZ = ((z + chunkZ * 16) + 0.5 - posZ) / radiusXZ;
//
//                if (((finalX * finalX) + (finalZ * finalZ)) < 1.0)
//                {
//                    for (int y = y2; y > y1; y--)
//                    {
//                        double finalY = ((y - 1) + 0.5 - posY) / radiusY;
//
//                        if ((finalY > -0.7) && (((finalX * finalX) + (finalY * finalY) + (finalZ * finalZ)) < 1.0D))
//                        {
//                            function.accept(new BlockPos(x, y, z));
//                        }
//                    }
//                }
//            }
//        }
//    }

    /**
     * Variant of addTunnel() which extracts the features dedicated to generating
     * single, symmetrical spheres, known internally as "rooms." This may be
     * slightly more redundant, but it should increase the algorithm's readability.
     */
    private void addRoom(
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
    private float adjustTwist(float original, Random rand, float exponent, float factor, float randFactor) {
        original = (float) Math.pow(original, exponent);
        original *= factor;
        original += randFactor * (rand.nextFloat() - rand.nextFloat()) * rand.nextFloat();
        return original;
    }

    /** Updates the value of `original` based on the input settings. */
    private float adjustScale(float original, Random rand, float exponent, float factor, float randFactor) {
        original = (float) Math.pow(original, exponent);
        original *= factor;
        original += randFactor * (rand.nextFloat() - 0.5F);
        if (original < 0) original = 0;
        return original;
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
        return info.test((pos) ->
            primer.getBlockState(pos.getX(), pos.getY(), pos.getZ()).equals(BLK_WATER)
        );
    }

    /** Replaces all blocks inside of this section. */
    private void replaceSection(ChunkPrimer primer, int chunkX, int chunkZ, TunnelSectionInfo info) {
        info.run((pos) -> {
            final int x = pos.getX();
            final int y = pos.getY();
            final int z = pos.getZ();
            final boolean isTopBlock = isTopBlock(primer, x, y, z, chunkX, chunkZ);
            replaceBlock(primer, x, y, z, chunkX, chunkZ, isTopBlock);
        });
    }

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
     * PersonTheCat. Allows alternatives to air to be randomly placed.
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
    private boolean replaceBlock(ChunkPrimer primer, int x, int y, int z, int chunkX, int chunkZ, boolean foundTop) {
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
            // To-do: Handle CaveBlocks.
            primer.setBlockState(x, y, z, BLK_AIR);
        }

        return false;
    }

    private boolean canReplaceBlock(IBlockState state) {
        if (state.getMaterial().equals(Material.WATER)){
            return false;
        }
        return find(settings.replaceable, (blk) -> blk.equals(state))
            .isPresent();
    }
}