package com.personthecat.cavegenerator.world.generator;

import com.personthecat.cavegenerator.data.RavineSettings;
import com.personthecat.cavegenerator.model.PrimerData;
import fastnoise.FastNoise;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.ChunkPrimer;

import java.util.Random;

public class RavineGenerator extends SphereGenerator {

    /** From vanilla: avoids unnecessary allocations. */
    private final float[] mut = new float[256];
    private final RavineSettings cfg;
    private final FastNoise wallNoise;

    public RavineGenerator(RavineSettings cfg, World world) {
        super(cfg.conditions, cfg.decorators, world);
        this.cfg = cfg;
        this.wallNoise = cfg.walls.getGenerator(world);
    }

    /** Spawns all of the ravines for this preset. */
    @Override
    protected void generateChecked(PrimerContext ctx) {
        if (ctx.rand.nextInt(cfg.chance) == 0) {
            startRavine(ctx.world, ctx.rand.nextLong(), ctx.destChunkX, ctx.destChunkZ, ctx.chunkX, ctx.chunkZ, ctx.primer);
        }
    }

    /** Starts a ravine between the input chunk coordinates. */
    private void startRavine(World world, long seed, int destX, int destZ, int x, int z, ChunkPrimer primer) {
        final Random rand = new Random(seed);
        final int distance = cfg.distance;
        final PrimerData data = new PrimerData(primer, x, z);
        final TunnelPathInfo path = new TunnelPathInfo(cfg, rand, destX, destZ);

        addRavine(world, rand.nextLong(), data, path, distance);
    }

    /**
     * Variant of addTunnel() and {~~@link net.minecraft.world.gen.MapGenRavine#addTunnel}
     * which randomly alters the horizontal radius based on `mut`, a buffer of random
     * values between 1-4, stored above. The difference in scale typically observed in
     * ravines is the result of arguments input to this function.
     */
    private void addRavine(World world, long seed, PrimerData data, TunnelPathInfo path, int distance) {
        // Master RNG for this tunnel.
        final Random mast = new Random(seed);
        // Avoid issues with inconsistent Random calls.
        final Random dec = new Random(seed);
        distance = getDistance(mast, distance);
        // Unique wall mutations for this chasm.
        final float[] mut = getMutations(mast);

        for (int currentPos = 0; currentPos < distance; currentPos++) {
            // Determine the radius by `scale`.
            final double radiusXZ = 1.5D + (MathHelper.sin(currentPos * (float) Math.PI / distance) * path.getScale());
            final double radiusY = radiusXZ * path.getStretch();
            path.update(mast, true, cfg.noiseYFactor, 0.05F);

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
                    generateSphere(dec, data, new TunnelSectionInfo(data, path, radiusXZ, radiusY).calculateMutated(mut));
                }
            }
        }
    }

    /** Used to produce the variations in horizontal scale seen in ravines. */
    private float[] getMutations(Random rand) {
        if (cfg.useWallNoise) {
            return getMutationsNoise();
        }
        return getMutationsVanilla(rand);
    }

    /** The effectively vanilla implementation of getMutations(). */
    private float[] getMutationsVanilla(Random rand) {
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
    private float[] getMutationsNoise() {
        for (int i = 0; i < mut.length; i++) {
            mut[i] = wallNoise.GetAdjustedNoise(0, i);
        }
        return mut;
    }
}
