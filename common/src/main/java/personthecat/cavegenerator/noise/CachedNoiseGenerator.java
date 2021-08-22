package personthecat.cavegenerator.noise;

import personthecat.fastnoise.FastNoise;
import personthecat.fastnoise.data.NoiseDescriptor;

public class CachedNoiseGenerator extends FastNoise {

    private final FastNoise reference;
    private final CachedNoiseHelper.Cache cache;

    public CachedNoiseGenerator(final NoiseDescriptor cfg, final FastNoise reference) {
        super(cfg);
        this.reference = reference;
        this.cache = CachedNoiseHelper.getOrCreate(cfg.hashCode());
    }

    @Override
    public float getSingle(final int seed, final float x) {
        return reference.getSingle(seed, x);
    }

    @Override
    public float getSingle(final int seed, final float x, final float y) {
        final int relX = (int) x & 15;
        final int relY = (int) y & 15;

        final float reused = cache.getNoise(relX, relY);
        if (reused != 0) {
            return reused;
        }
        final float noise = reference.getSingle(seed, x, y);
        cache.writeNoise(relX, relY, noise);
        return noise;
    }

    @Override
    public float getSingle(final int seed, final float x, final float y, final float z) {
        final int relX = (int) x & 15;
        final int relY = (int) y & 255;
        final int relZ = (int) z & 15;

        final float reused = cache.getNoise(relX, relY, relZ);
        if (reused != 0) {
            return reused;
        }
        final float noise = reference.getSingle(seed, x, y, z);
        cache.writeNoise(relX, relY, relZ, noise);
        return noise;
    }
}
