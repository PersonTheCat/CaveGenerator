package com.personthecat.cavegenerator.util;

import fastnoise.FastNoise;
import fastnoise.FastNoise.*;

/**
 * Variant of NoiseSettings2D.
 * May be deleted as soon as some additional parameters are
 * merged directly into FastNoise.
 */
public class NoiseSettings3D {
    public final float scale; // Calculated from scale.
    public final float frequency;
    public final float scaleY;
    public final float lacunarity;
    public final float gain;
    public final float perturbAmp;
    public final float perturbFreq;
    public final float jitter;
    public final int octaves;
    public final boolean perturb;
    public final boolean invert;
    public final NoiseType noiseType;
    public final Interp interp;
    public final FractalType fractalType;
    public final CellularDistanceFunction distanceFunction;
    public final CellularReturnType returnType;
    public final NoiseType cellularLookup;

    /** Where @param scale is a value between 0.0 and 1.0. */
    public NoiseSettings3D(
        float frequency,
        float scale,
        float scaleY,
        float lacunarity,
        float gain,
        float perturbAmp,
        float perturbFreq,
        float jitter,
        int octaves,
        boolean perturb,
        boolean invert,
        Interp interp,
        NoiseType noiseType,
        FractalType fractalType,
        CellularDistanceFunction distanceFunction,
        CellularReturnType returnType,
        NoiseType cellularLookup
    ) {
        this.scale = scale;
        this.frequency = frequency;
        this.scaleY = scaleY;
        this.lacunarity = lacunarity;
        this.gain = gain;
        this.perturbAmp = perturbAmp;
        this.perturbFreq = perturbFreq;
        this.jitter = jitter;
        this.octaves = octaves;
        this.perturb = perturb;
        this.invert = invert;
        this.interp = interp;
        this.noiseType = noiseType;
        this.fractalType = fractalType;
        this.distanceFunction = distanceFunction;
        this.returnType = returnType;
        this.cellularLookup = cellularLookup;
    }

    /** Variant of the primary constructor with a default value for noiseType, etc. */
    public NoiseSettings3D(float frequency, float scale, float scaleY, int octaves) {
        this(frequency, scale, scaleY, 1.0f, 0.5f, 1.0f, 0.1f, 0.45f, octaves, false, false, Interp.Hermite, NoiseType.SimplexFractal,
            FractalType.FBM, CellularDistanceFunction.Euclidean, CellularReturnType.Distance2, NoiseType.Simplex);
    }

    public FastNoise getGenerator(int seed) {
        return new FastNoise(seed)
            .SetNoiseType(noiseType)
            .SetFrequency(frequency)
            .SetFractalType(fractalType)
            .SetFractalOctaves(octaves)
            .SetInvert(invert)
            .SetFractalGain(gain)
            .SetCellularReturnType(returnType)
            .SetGradientPerturbAmp(perturbAmp)
            .SetGradientPerturbFrequency(perturbFreq)
            .SetGradientPerturb(perturb)
            .SetFractalLacunarity(lacunarity)
            .SetCellularNoiseLookup(cellularLookup)
            .SetCellularDistanceFunction(distanceFunction)
            .SetInterp(interp)
            .SetCellularJitter(jitter)
            .SetScale(scale)
            .SetScaleY(scaleY);
    }
}