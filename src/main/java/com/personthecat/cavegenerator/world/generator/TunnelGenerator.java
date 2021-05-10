package com.personthecat.cavegenerator.world.generator;

import com.personthecat.cavegenerator.data.RoomSettings;
import com.personthecat.cavegenerator.data.TunnelSettings;
import com.personthecat.cavegenerator.model.PrimerData;
import com.personthecat.cavegenerator.util.XoRoShiRo;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkPrimer;

import javax.annotation.Nullable;
import java.util.Random;

public class TunnelGenerator extends MapGenerator {

    private static final float PI_OVER_2 = (float) (Math.PI / 2);

    protected final TunnelSettings cfg;
    @Nullable private final RoomSettings rooms;
    @Nullable private final TunnelGenerator branches;

    public TunnelGenerator(TunnelSettings cfg, World world) {
        super(cfg.conditions, cfg.decorators, world, cfg.waterChecks);
        this.cfg = cfg;
        this.rooms = cfg.rooms.orElse(null);
        this.branches = cfg.branches.map(b -> new TunnelGenerator(b, world)).orElse(null);
    }

    @Override
    protected void mapGenerate(MapGenerationContext ctx) {
        this.createSystem(ctx.world, ctx.rand.nextLong(), ctx.destChunkX, ctx.destChunkZ, ctx.chunkX, ctx.chunkZ, ctx.primer);
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
                final double sumRoXZ = distX2 / roXZ2 + distZ2 / roXZ2;
                if (sumRoXZ >= 1.0) {
                    continue;
                }
                final double sumRXZ = distX2 / rXZ2 + distZ2 / rXZ2;
                for (int y = maY; y > miY; y--) {
                    final double distY = ((y - 1) + 0.5 - cY);
                    final double distY2 = distY * distY;
                    if ((distY / rY > -0.7) && (sumRXZ + distY2 / rY2 < 1.0)) {
                        sphere.inner.add(x, y, z);
                    } else if (sumRoXZ + distY2 / roY2 < 1.0) {
                        sphere.shell.add(x, y, z);
                    }
                }
            }
        }
    }

    /** Starts a tunnel system between the input chunk coordinates. */
    protected void createSystem(World world, long seed, int destX, int destZ, int x, int z, ChunkPrimer primer) {
        final Random rand = new XoRoShiRo(seed);
        final int frequency = this.getTunnelCount(rand);
        for (int i = 0; i < frequency; i++) {
            final int distance = this.cfg.distance;
            final PrimerData data = new PrimerData(primer, x, z);

            for (int j = 0; j < this.getBranchCount(rand); j++) {
                final TunnelPathInfo path = new TunnelPathInfo(this.cfg, rand, destX, destZ);
                if (this.conditions.getColumn((int) path.getX(), (int) path.getZ()).contains((int) path.getY())) {
                    if (this.conditions.noise.GetBoolean(path.getX(), path.getY(), path.getZ())) {
                        if (this.rooms != null && rand.nextInt(this.rooms.chance) == 0) {
                            this.addRoom(world, rand, data, this.rooms.scale, this.rooms.stretch, path.getX(), path.getY(), path.getZ());
                            // From vanilla: alters the scale each time a room spawns. Remove this?
                            path.multiplyScale(rand.nextFloat() * rand.nextFloat() * 3.00F + 1.00F);
                        }
                        final long tunnelSeed = this.cfg.seed.orElseGet(rand::nextLong);
                        this.addTunnel(world, tunnelSeed, data, path,0, distance);
                    }
                }
            }
        }
    }

    /** Determines the number of cave systems to try and spawn. */
    protected int getTunnelCount(Random rand) {
        final int frequency = rand.nextInt(rand.nextInt(rand.nextInt(cfg.count) + 1) + 1);
        // The order is important for seeds
        if (rand.nextInt(this.cfg.chance) != 0) {
            // Usually set frequency to 0, causing the systems to be
            // isolated from one another.
            return 0;
        }
        return frequency;
    }

    protected int getBranchCount(Random rand) {
        if (rand.nextInt(this.cfg.systemChance) == 0) {
            return 1 + rand.nextInt(this.cfg.systemDensity);
        }
        return 1;
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
    protected void addTunnel(World world, long seed, PrimerData data, TunnelPathInfo path, int position, int distance) {
        // Main RNG for this tunnel.
        final Random rand = new XoRoShiRo(seed);
        distance = this.getDistance(rand, distance);
        // Determine where to place branches, if applicable.
        final int randomBranchIndex = rand.nextInt(distance / 2) + distance / 4;
        final boolean randomNoiseCorrection = rand.nextInt(6) == 0;

        for (int currentPos = position; currentPos < distance; currentPos++) {
            // Determine the radius by `scale`.
            final double rXZ = 1.5D + (MathHelper.sin(currentPos * (float) Math.PI / distance) * path.getScale());
            final double rY = rXZ * path.getStretch();
            final double d = this.decorators.shell.cfg.radius;
            final double roXZ = rXZ + d;
            final double roY = rY + d;

            path.update(rand, this.cfg.noiseYReduction, randomNoiseCorrection ? 0.92F : 0.70F, 0.1F);

            if (path.getScale() > 1.00F && distance > 0 && currentPos == randomBranchIndex && currentPos != position) {
                this.addBranches(world, rand, seed, data, path, currentPos, distance);
                return;
            }
            // Effectively sets the tunnel resolution by randomly skipping
            // tunnel segments, increasing performance.
            if (rand.nextInt(this.cfg.resolution) == 0) {
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
            if (!this.conditions.height.contains((int) path.getY())) {
                continue;
            }
            this.generateSphere(data, world, new XoRoShiRo(decSeed), path.getX(), path.getY(), path.getZ(), rXZ, rY, roXZ, roY);
        }
    }

    /**
     * Variant of addTunnel() which extracts the features dedicated to generating
     * single, symmetrical spheres, known internally as "rooms." This may be
     * slightly more redundant, but it should increase the algorithm's readability.
     */
    private void addRoom(World world, Random main, PrimerData data, float scale, float stretch, double x, double y, double z) {
        // Construct these initial values using `rand`, consistent
        // with the vanilla setup.
        final long seed = main.nextLong();
        scale = main.nextFloat() * scale + 1;
        // Construct a local Random object for use within this function,
        // also matching the vanilla setup.
        final Random local = new XoRoShiRo(seed);
        final int distance = getDistance(local, 0);
        final int position = distance / 2;
        // Determine the radius by `scale`.
        final double rXZ = 1.5D + (MathHelper.sin(position * (float) Math.PI / distance) * scale);
        final double rY = rXZ * stretch;
        final double d = this.decorators.shell.cfg.radius;
        this.generateSphere(data, world, local, x, y, z, rXZ, rY, rXZ + d, rY + d);
    }

    private void addBranches(World world, Random rand, long seed, PrimerData data, TunnelPathInfo path, int currentPos, int distance) {
        if (!this.cfg.hasBranches) return;
        final float yaw1 = path.getYaw() - PI_OVER_2;
        final float yaw2 = path.getYaw() + PI_OVER_2;
        final float pitch = path.getPitch() / 3.0F;
        final TunnelPathInfo reset1, reset2;

        if (this.cfg.resizeBranches) { // In vanilla, tunnels are resized when branching.
            reset1 = path.reset(yaw1, pitch, rand.nextFloat() * 0.5F + 0.5F, 1.00F);
            reset2 = path.reset(yaw2, pitch, rand.nextFloat() * 0.5F + 0.5F, 1.00F);
        } else { // Continue with the same size (not vanilla).
            reset1 = path.reset(yaw1, pitch, path.getScale(), path.getStretch());
            reset2 = path.reset(yaw2, pitch, path.getScale(), path.getStretch());
        }
        if (this.branches != null) {
            final long seedA = this.branches.cfg.seed.orElse(this.cfg.reseedBranches ? rand.nextLong() : seed);
            this.branches.addTunnel(world, seedA, data, reset1, currentPos, distance);
            final long seedB = this.branches.cfg.seed.orElse(this.cfg.reseedBranches ? rand.nextLong() : seed);
            this.branches.addTunnel(world, seedB, data, reset2, currentPos, distance);
        } else {
            final long seedA = this.cfg.seed.orElse(this.cfg.reseedBranches ? rand.nextLong() : seed);
            this.addTunnel(world, seedA, data, reset1, currentPos, distance);
            final long seedB = this.cfg.seed.orElse(this.cfg.reseedBranches ? rand.nextLong() : seed);
            this.addTunnel(world, seedB, data, reset2, currentPos, distance);
        }
    }
}
