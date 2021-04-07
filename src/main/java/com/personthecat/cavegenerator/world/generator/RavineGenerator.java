package com.personthecat.cavegenerator.world.generator;

import com.personthecat.cavegenerator.data.RavineSettings;
import com.personthecat.cavegenerator.model.PrimerData;
import fastnoise.FastNoise;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkPrimer;

import java.util.Random;

public class RavineGenerator extends MapGenerator {

    /** From vanilla: avoids unnecessary allocations. */
    private final float[] mut = new float[256];
    private final RavineSettings cfg;
    private final FastNoise wallNoise;
    private final double cutoff;

    public RavineGenerator(RavineSettings cfg, World world) {
        super(cfg.conditions, cfg.decorators, world, true);
        this.cfg = cfg;
        this.wallNoise = cfg.walls.getGenerator(world);
        this.cutoff = 1.0 + cfg.cutoffStrength;
    }

    @Override
    protected void mapGenerate(MapGenerationContext ctx) {
        if (ctx.rand.nextInt(cfg.chance) == 0) {
            this.startRavine(ctx.rand.nextLong(), ctx.destChunkX, ctx.destChunkZ, ctx.chunkX, ctx.chunkZ, ctx.primer);
        }
    }

    @Override
    protected void fillSphere(SphereData sphere, double cX, double cY, double cZ, int absX, int absZ,
              double rXZ, double rY, int miX, int maX, int miY, int maY, int miZ, int maZ) {
        for (int x = miX; x < maX; x++) {
            final double distX = ((x + absX) + 0.5 - cX) / rXZ;
            final double distX2 = distX * distX;
            for (int z = miZ; z < maZ; z++) {
                final double distZ = ((z + absZ) + 0.5 - cZ) / rXZ;
                final double distZ2 = distZ * distZ;
                if (distX2 + distZ2 >= 1.0) {
                    continue;
                }
                for (int y = maY; y > miY; y--) {
                    final double distY = ((y - 1) + 0.5 - cY) / rY;
                    final double distY2 = distY * distY;
                    if ((distX2 + distZ2) * mut[y - 1] + distY2 / this.cutoff < 1.0) {
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
        final int d = (int) (roY - rY);
        final int miOY = Math.max(1, miY - d);
        final int maOY = Math.min(248, maY + d);

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
                this.coverOuter(sphere, sumRoXZ, roY2, x, z, cY, miOY, miY);
                for (int y = maY; y > miY; y--) {
                    final double distY = ((y - 1) + 0.5 - cY);
                    final double distY2 = distY * distY;
                    if (sumRXZ * mut[y - 1] + distY2 / rY2 / this.cutoff < 1.0) {
                        sphere.inner.add(x, y, z);
                    } else if (sumRoXZ * mut[y - 1] + distY2 / roY2 / this.cutoff < 1.0) {
                        sphere.shell.add(x, y, z);
                    }
                }
                this.coverOuter(sphere, sumRoXZ, roY2, x, z, cY, maY, maOY);
            }
        }
    }

    private void coverOuter(SphereData sphere, double sumRoXZ, double roY2, int x, int z, double cY, int min, int max) {
        if (this.cutoff > 1.0) {
            for (int y = max; y > min; y--) {
                final double distY = ((y - 1) + 0.5 - cY);
                final double distY2 = distY * distY;
                if (sumRoXZ * mut[y - 1] + distY2 / roY2 / this.cutoff < 1.0) {
                    sphere.shell.add(x, y, z);
                }
            }
        }
    }

    /** Starts a ravine between the input chunk coordinates. */
    private void startRavine(long seed, int destX, int destZ, int x, int z, ChunkPrimer primer) {
        final Random rand = new Random(seed);
        final int distance = cfg.distance;
        final PrimerData data = new PrimerData(primer, x, z);
        final TunnelPathInfo path = new TunnelPathInfo(cfg, rand, destX, destZ);

        // Todo: verify that we need to check this outside of the current chunk.
        if (conditions.getColumn((int) path.getX(), (int) path.getZ()).contains((int) path.getY())) {
            if (conditions.noise.GetBoolean(path.getX(), path.getY(), path.getZ())) {
                this.addRavine(rand.nextLong(), data, path, distance);
            }
        }
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
        distance = this.getDistance(mast, distance);
        // Unique wall mutations for this chasm.
        this.fillMutations(mast);

        for (int currentPos = 0; currentPos < distance; currentPos++) {
            // Determine the radius by `scale`.
            final double rXZ = 1.5D + (MathHelper.sin(currentPos * (float) Math.PI / distance) * path.getScale());
            final double rY = rXZ * path.getStretch();
            final double d = this.decorators.shell.cfg.sphereRadius;
            final double roXZ = rXZ + d;
            final double roY = rY + d;

            path.update(mast, true, cfg.noiseYFactor, 0.05F);

            if (mast.nextInt(cfg.resolution) == 0) {
                continue;
            }
            // Make sure we haven't travelled too far?
            if (path.travelledTooFar(data, currentPos, distance)) {
                return;
            }
            if (!path.touchesChunk(data, roXZ * 2.0)) {
                continue;
            }
            if (getNearestBorder((int) path.getX(), (int) path.getZ()) < roXZ + 9) {
                continue;
            }
            if (!conditions.height.contains((int) path.getY())) {
                continue;
            }
            this.generateSphere(data, dec, path.getX(), path.getY(), path.getZ(), rXZ, rY, roXZ, roY);
        }
    }

    /** Used to produce the variations in horizontal scale seen in ravines. */
    private void fillMutations(Random rand) {
        if (cfg.useWallNoise) {
            this.fillMutationsWithNoise();
        } else {
            this.fillMutationsVanilla(rand);
        }
    }

    /** The effectively vanilla implementation of getMutations(). */
    private void fillMutationsVanilla(Random rand) {
        float val = 1.0f;
        for (int i = 0; i < mut.length; i++) {
            if (i == 0 || rand.nextInt(3) == 0) {
                val = rand.nextFloat() * rand.nextFloat() + 1.0f;
            }
            mut[i] = val * val;
        }
    }

    /** Variant of getMutations() which produces aberrations using a noise generator. */
    private void fillMutationsWithNoise() {
        for (int i = 0; i < mut.length; i++) {
            mut[i] = wallNoise.GetAdjustedNoise(0, i);
        }
    }
}
