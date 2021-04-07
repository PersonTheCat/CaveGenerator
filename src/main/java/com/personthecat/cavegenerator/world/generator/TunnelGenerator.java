package com.personthecat.cavegenerator.world.generator;

import com.personthecat.cavegenerator.data.RoomSettings;
import com.personthecat.cavegenerator.data.TunnelSettings;
import com.personthecat.cavegenerator.model.PrimerData;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkPrimer;

import javax.annotation.Nullable;
import java.util.Random;

public class TunnelGenerator extends MapGenerator {

    private static final float PI_OVER_2 = (float) (Math.PI / 2);

    private final TunnelSettings cfg;
    @Nullable private final RoomSettings rooms;
    @Nullable private final TunnelGenerator branches;

    public TunnelGenerator(TunnelSettings cfg, World world) {
        super(cfg.conditions, cfg.decorators, world, true);
        this.cfg = cfg;
        this.rooms = cfg.rooms.orElse(null);
        this.branches = cfg.branches.map(b -> new TunnelGenerator(b, world)).orElse(null);
    }

    @Override
    protected void mapGenerate(MapGenerationContext ctx) {
        createSystem(ctx.world, ctx.rand.nextLong(), ctx.destChunkX, ctx.destChunkZ, ctx.chunkX, ctx.chunkZ, ctx.primer);
    }

    @Override
    protected void fillSphere(SphereData sphere, double cX, double cY, double cZ, int absX, int absZ,
            double radXZ, double radY, int miX, int maX, int miY, int maY, int miZ, int maZ) {
        for (int x = miX; x < maX; x++) {
            final double distX = ((x + absX) + 0.5 - cX) / radXZ;
            final double distX2 = distX * distX;
            for (int z = miZ; z < maZ; z++) {
                final double distZ = ((z + absZ) + 0.5 - cZ) / radXZ;
                final double distZ2 = distZ * distZ;
                if ((distX2 + distZ2) >= 1.0) {
                    continue;
                }
                for (int y = maY; y > miY; y--) {
                    final double distY = ((y - 1) + 0.5 - cY) / radY;
                    final double distY2 = distY * distY;
                    if ((distY > -0.7) && ((distX2 + distY2 + distZ2) < 1.0)) {
                        sphere.inner.add(x, y, z);
                    }
                }
            }
        }
    }

    @Override
    protected void fillDouble(SphereData sphere, double cX, double cY, double cZ, int absX, int absZ, double rXZ,
              double rY, double roXZ, double roY, int miX, int maX, int miY, int maY, int miZ, int maZ) {
        final double rXZ2 = rXZ * rXZ;
        final double rY2 = rY * rY;
        final double roXZ2 = roXZ * roXZ;
        final double roY2 = roY * roY;
        for (int x = miX; x < maX; x++) {
            final double distX = ((x + absX) + 0.5 - cX);
            final double distX2 = distX * distX;
            for (int z = miZ; z < maZ; z++) {
                final double distZ = ((z + absZ) + 0.5 - cZ);
                final double distZ2 = distZ * distZ;
                if ((distX2 / roXZ2 + distZ2 / roXZ2) >= 1.0) {
                    continue;
                }
                for (int y = maY; y > miY; y--) {
                    final double distY = ((y - 1) + 0.5 - cY);
                    final double distY2 = distY * distY;
                    if ((distY / rY > -0.7) && ((distX2 / rXZ2 + distY2 / rY2 + distZ2 / rXZ2) < 1.0)) {
                        sphere.inner.add(x, y, z);
                    } else if (distX2 / roXZ2 + distY2 / roY2 + distZ2 / roXZ2 < 1.0) {
                        sphere.shell.add(x, y, z);
                    }
                }
            }
        }
    }

    /** Starts a tunnel system between the input chunk coordinates. */
    private void createSystem(World world, long seed, int destX, int destZ, int x, int z, ChunkPrimer primer) {
        final Random rand = new Random(seed);
        final int frequency = this.getTunnelFrequency(rand);
        for (int i = 0; i < frequency; i++) {
            int branches = 1;
            if (rand.nextInt(cfg.systemChance) == 0) {
                branches += rand.nextInt(cfg.systemDensity);
            }
            final int distance = cfg.distance;
            final PrimerData data = new PrimerData(primer, x, z);

            for (int j = 0; j < branches; j++) {
                final TunnelPathInfo path = new TunnelPathInfo(cfg, rand, destX, destZ);
                if (conditions.getColumn((int) path.getX(), (int) path.getZ()).contains((int) path.getY())) {
                    if (conditions.noise.GetBoolean(path.getX(), path.getY(), path.getZ())) {
                        if (rooms != null && rand.nextInt(rooms.chance) == 0) {
                            this.addRoom(rand, data, rooms.scale, rooms.stretch, path.getX(), path.getY(), path.getZ());
                            // From vanilla: alters the scale each time a room spawns. Remove this?
                            path.multiplyScale(rand.nextFloat() * rand.nextFloat() * 3.00F + 1.00F);
                        }
                        this.addTunnel(world, rand.nextLong(), data, path,0, distance);
                    }
                }
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
     * Mod of {~~@link net.minecraft.world.gen.MapGenCaves#addTunnel} by PersonTheCat. This is the basic function
     * responsible for spawning a chained sequence of angled spheres in the world. Supports object-specific
     * replacement of most variables, as well as a few new variables for controlling shapes, adding noise-based
     * alternatives to air, and wall decorations.
     *
     * @param world     The current world being generated inside of.
     * @param seed      The world's seed. Use to create a local Random object for
     *                  regen parity.
     * @param data      Data containing the current chunk primer and coordinates.
     * @param path      Data containing information about the path of tunnel segments to be created.
     * @param position  A measure of progress until `distance`.
     * @param distance  The length of the tunnel. 0 -> # ( 132 to 176 ).
     */
    private void addTunnel(World world, long seed, PrimerData data, TunnelPathInfo path, int position, int distance) {
        // Main RNG for this tunnel.
        final Random rand = new Random(seed);
        distance = this.getDistance(rand, distance);
        // Determine where to place branches, if applicable.
        final int randomBranchIndex = rand.nextInt(distance / 2) + distance / 4;
        final boolean randomNoiseCorrection = rand.nextInt(6) == 0;

        for (int currentPos = position; currentPos < distance; currentPos++) {
            // Determine the radius by `scale`.
            final double rXZ = 1.5D + (MathHelper.sin(currentPos * (float) Math.PI / distance) * path.getScale());
            final double rY = rXZ * path.getStretch();
            final double d = this.decorators.shell.cfg.sphereRadius;
            final double roXZ = rXZ + d;
            final double roY = rY + d;

            path.update(rand, cfg.noiseYReduction, randomNoiseCorrection ? 0.92F : 0.70F, 0.1F);

            if (path.getScale() > 1.00F && distance > 0 && currentPos == randomBranchIndex) {
                this.addBranches(world, rand, data, path, currentPos, distance);
                return;
            }
            // Effectively sets the tunnel resolution by randomly skipping
            // tunnel segments, increasing performance.
            if (rand.nextInt(cfg.resolution) == 0) {
                continue;
            }
            // Make sure we haven't travelled too far?
            if (path.travelledTooFar(data, currentPos, distance)) {
                return;
            }
            // Avoid issues with inconsistent Random calls.
            final int decSeed = rand.nextInt(); // Todo: profile this
            if (!path.touchesChunk(data, roXZ * 2.0)) {
                continue;
            }
            if (this.getNearestBorder((int) path.getX(), (int) path.getZ()) < roXZ + 9) {
                continue;
            }
            if (!conditions.height.contains((int) path.getY())) {
                continue;
            }
            this.generateSphere(data, new Random(decSeed), path.getX(), path.getY(), path.getZ(), rXZ, rY, roXZ, roY);
        }
    }

    /**
     * Variant of addTunnel() which extracts the features dedicated to generating
     * single, symmetrical spheres, known internally as "rooms." This may be
     * slightly more redundant, but it should increase the algorithm's readability.
     */
    private void addRoom(Random main, PrimerData data, float scale, float stretch, double x, double y, double z) {
        // Construct these initial values using `rand`, consistent
        // with the vanilla setup.
        final long seed = main.nextLong();
        scale = main.nextFloat() * scale + 1;
        // Construct a local Random object for use within this function,
        // also matching the vanilla setup.
        final Random local = new Random(seed);
        final int distance = getDistance(local, 0);
        final int position = distance / 2;
        // Determine the radius by `scale`.
        final double rXZ = 1.5D + (MathHelper.sin(position * (float) Math.PI / distance) * scale);
        final double rY = rXZ * stretch;
        final double d = this.decorators.shell.cfg.sphereRadius;
        this.generateSphere(data, local, x, y, z, rXZ, rY, rXZ + d, rY + d);
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
            reset1 = path.reset(yaw1, pitch, path.getScale(), path.getStretch());
            reset2 = path.reset(yaw2, pitch, path.getScale(), path.getStretch());
        }
        // Todo: add control over using new seeds.
        if (branches != null) {
            branches.addTunnel(world, rand.nextLong(), data, reset1, currentPos, distance);
            branches.addTunnel(world, rand.nextLong(), data, reset2, currentPos, distance);
        } else {
            this.addTunnel(world, rand.nextLong(), data, reset1, currentPos, distance);
            this.addTunnel(world, rand.nextLong(), data, reset2, currentPos, distance);
        }
    }
}
