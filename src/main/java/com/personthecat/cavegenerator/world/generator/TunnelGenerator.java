package com.personthecat.cavegenerator.world.generator;

import com.personthecat.cavegenerator.data.TunnelSettings;
import com.personthecat.cavegenerator.model.PrimerData;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.ChunkPrimer;

import javax.annotation.Nullable;
import java.util.Random;

public class TunnelGenerator extends SphereGenerator {

    private static final float PI_OVER_2 = (float) (Math.PI / 2);

    private final TunnelSettings cfg;
    @Nullable private final TunnelGenerator branches;

    public TunnelGenerator(TunnelSettings cfg, World world) {
        super(cfg.conditions, cfg.decorators, world);
        this.cfg = cfg;
        this.branches = cfg.branches.map(b -> new TunnelGenerator(b, world)).orElse(null);
    }

    @Override
    protected void generateChecked(World world, Random rand, int destChunkX, int destChunkZ, int chunkX, int chunkZ, ChunkPrimer primer) {
        createSystem(world, rand.nextLong(), destChunkX, destChunkZ, chunkX, chunkZ, primer);
    }

    /** Starts a tunnel system between the input chunk coordinates. */
    private void createSystem(World world, long seed, int destX, int destZ, int x, int z, ChunkPrimer primer) {
        final Random rand = new Random(seed);
        final int frequency = getTunnelFrequency(rand);
        for (int i = 0; i < frequency; i++) {
            // Determine the number of branches to spawn.
            int branches = 1;
            if (rand.nextInt(cfg.systemChance) == 0) {
                // Add a room at the center? of the system.
                branches += rand.nextInt(cfg.systemDensity);
            }

            for (int j = 0; j < branches; j++) {
                final int distance = cfg.distance;
                final PrimerData data = new PrimerData(primer, x, z);
                final TunnelPathInfo path = new TunnelPathInfo(cfg, rand, destX, destZ);

                // Per-vanilla: this randomly increases the size.
                if (rand.nextInt(cfg.rooms.chance) == 0) {
                    addRoom(rand, data, path.getX(), path.getY(), path.getZ());
                    // From vanilla: alters the scale each time a room spawns. Remove this?
                    path.multiplyScale(rand.nextFloat() * rand.nextFloat() * 3.00F + 1.00F);
                }
                addTunnel(world, rand.nextLong(), data, path,0, distance);
            }
        }
    }

    /** Determines the number of cave systems to try and spawn. */
    private int getTunnelFrequency(Random rand) {
        final int frequency = rand.nextInt(rand.nextInt(rand.nextInt(cfg.count) + 1) + 1);
        // The order is important for seeds
        if (rand.nextInt(cfg.chance) != 0) {
            // Usually set frequency to 0, causing the systems to be
            // isolated from one another.
            return 0;
        }
        return frequency;
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
    private void addTunnel(World world, long seed, PrimerData data, TunnelPathInfo path, int position, int distance) {
        // Master RNG for this tunnel.
        final Random mast = new Random(seed);
        // Avoid issues with inconsistent Random calls.
        final Random dec = new Random(seed);
        distance = getDistance(mast, distance);
        // Determine where to place branches, if applicable.
        final int randomBranchIndex = mast.nextInt(distance / 2) + distance / 4;
        final boolean randomNoiseCorrection = mast.nextInt(6) == 0;

        for (int currentPos = position; currentPos < distance; currentPos++) {
            // Determine the radius by `scale`.
            final double radiusXZ = 1.5D + (MathHelper.sin(currentPos * (float) Math.PI / distance) * path.getScale());
            final double radiusY = radiusXZ * path.getScaleY();
            path.update(mast, cfg.noiseYReduction, randomNoiseCorrection ? 0.92F : 0.70F, 0.1F);

            if (path.getScale() > 1.00F && distance > 0 && currentPos == randomBranchIndex) {
                addBranches(world, mast, data, path, currentPos, distance);
                return;
            }
            // Effectively sets the tunnel resolution by randomly skipping
            // tunnel segments, increasing performance.
            if (mast.nextInt(cfg.resolution) == 0) {
                continue;
            }
            // Make sure we haven't travelled too far?
            if (path.travelledTooFar(data, currentPos, distance)) {
                return;
            }
            if (path.touchesChunk(data, radiusXZ * 2.0)) {
                // Calculate all of the positions in the section.
                // We'll be using them multiple times.
                final int x = (int) path.getX(), y = (int) path.getY(), z = (int) path.getZ();
                final Biome biome = world.getBiome(new BlockPos(x, 0, z));
                if (conditions.checkSingle(biome, x, y, z)) {
                    generateSphere(dec, data, new TunnelSectionInfo(data, path, radiusXZ, radiusY).calculate());
                }
            }
        }
    }

    /**
     * Variant of addTunnel() which extracts the features dedicated to generating
     * single, symmetrical spheres, known internally as "rooms." This may be
     * slightly more redundant, but it should increase the algorithm's readability.
     */
    private void addRoom(Random main, PrimerData data, double x, double y, double z) {
        // Construct these initial values using `rand`, consistent
        // with the vanilla setup.
        final long seed = main.nextLong();
        final float scale = main.nextFloat() * cfg.rooms.scale + 1;
        final float scaleY = cfg.rooms.stretch;
        // Construct a local Random object for use within this function,
        // also matching the vanilla setup.
        final Random rand = new Random(seed);
        final int distance = getDistance(rand, 0);
        final int position = distance / 2;
        // Determine the radius by `scale`.
        final double radiusXZ = 1.5D + (MathHelper.sin(position * (float) Math.PI / distance) * scale);
        final double radiusY = radiusXZ * scaleY;
        // Coordinates are normally shifted based on yaw
        // and pitch. When the input angle would otherwise
        // just be 0.0, this is the only value that would
        // actually change.
        x += 1;
        // Calculate all of the positions in the section.
        // We'll be using them multiple times.
        generateSphere(main, data, new TunnelSectionInfo(data, x, y, z, radiusXZ, radiusY).calculate());
    }

    private void addBranches(World world, Random rand, PrimerData data, TunnelPathInfo path, int currentPos, int distance) {
        final float yaw1 = path.getYaw() - PI_OVER_2;
        final float yaw2 = path.getYaw() + PI_OVER_2;
        final float pitch = path.getPitch() / 3.0F;
        final TunnelPathInfo reset1, reset2;

        if (cfg.resizeBranches) { // In vanilla, tunnels are resized when branching.
            reset1 = path.reset(yaw1, pitch, rand.nextFloat() * 0.5F + 0.5F, 1.00F);
            reset2 = path.reset(yaw2, pitch, rand.nextFloat() * 0.5F + 0.5F, 1.00F);
        } else { // Continue with the same size (not vanilla).
            reset1 = path.reset(yaw1, pitch, path.getScale(), path.getScaleY());
            reset2 = path.reset(yaw2, pitch, path.getScale(), path.getScaleY());
        }
        if (branches != null) {
            branches.addTunnel(world, rand.nextLong(), data, reset1, currentPos, distance);
            branches.addTunnel(world, rand.nextLong(), data, reset2, currentPos, distance);
        } else {
            this.addTunnel(world, rand.nextLong(), data, reset1, currentPos, distance);
            this.addTunnel(world, rand.nextLong(), data, reset2, currentPos, distance);
        }
    }
}
