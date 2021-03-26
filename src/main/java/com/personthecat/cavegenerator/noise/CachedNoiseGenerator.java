package com.personthecat.cavegenerator.noise;

import fastnoise.FastNoise;

public class CachedNoiseGenerator extends FastNoise {

    private final FastNoise wrapped;
    private final CachedNoiseHelper.Cache cache;

    // WIP. This should probably use a builder instead of copying from wrapped.
    public CachedNoiseGenerator(FastNoise wrapped) {
        this.wrapped = wrapped;
        this.cache = CachedNoiseHelper.getOrCreate(wrapped.hashCode());
        this.m_seed = wrapped.m_seed;
        this.m_frequency = wrapped.m_frequency;
        this.m_interp = wrapped.m_interp;
        this.m_noiseType = wrapped.m_noiseType;
        this.m_octaves = wrapped.m_octaves;
        this.m_lacunarity = wrapped.m_lacunarity;
        this.m_gain = wrapped.m_gain;
        this.m_fractalType = wrapped.m_fractalType;
        this.m_fractalBounding = wrapped.m_fractalBounding;
        this.m_cellularDistanceFunction = wrapped.m_cellularDistanceFunction;
        this.m_cellularReturnType = wrapped.m_cellularReturnType;
        this.m_cellularNoiseLookup = wrapped.m_cellularNoiseLookup;
        this.m_cellularJitterX = wrapped.m_cellularJitterX;
        this.m_cellularJitterY = wrapped.m_cellularJitterY;
        this.m_cellularJitterZ = wrapped.m_cellularJitterZ;
        this.m_cellular3Edge = wrapped.m_cellular3Edge;
        this.m_gradientPerturb = wrapped.m_gradientPerturb;
        this.m_gradientPerturbAmp = wrapped.m_gradientPerturbAmp;
        this.m_gradientPerturbFrequency = wrapped.m_gradientPerturbFrequency;
        this.m_stretch = wrapped.m_stretch;
        this.m_offset = wrapped.m_offset;
        this.m_invert = wrapped.m_invert;
        this.m_multiple = wrapped.m_multiple;
        this.m_addend = wrapped.m_addend;
        this.m_booleanMinThreshold = wrapped.m_booleanMinThreshold;
        this.m_booleanMaxThreshold = wrapped.m_booleanMaxThreshold;
    }

    @Override
    public float GetNoise(float x, float y) {
        final int relX = (int) x & 15;
        final int relY = (int) y & 15;

        final float reused = cache.getNoise(relX, relY);
        if (reused != 0) {
            return reused;
        }
        final float noise = wrapped.GetNoise(x, y);
        cache.writeNoise(relX, relY, noise);
        return noise;
    }

    @Override
    public float GetNoise(float x, float y, float z) {
        final int relX = (int) x & 15;
        final int relY = (int) y & 255;
        final int relZ = (int) z & 15;

        final float reused = cache.getNoise(relX, relY, relZ);
        if (reused != 0) {
            return reused;
        }
        final float noise = wrapped.GetNoise(x, y, z);
        cache.writeNoise(relX, relY, relZ, noise);
        return noise;
    }
}
