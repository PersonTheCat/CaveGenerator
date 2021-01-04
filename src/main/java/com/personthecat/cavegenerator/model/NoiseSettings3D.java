package com.personthecat.cavegenerator.model;

import fastnoise.FastNoise;
import fastnoise.FastNoise.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.experimental.FieldDefaults;

import java.util.Optional;
import java.util.Random;

import static com.personthecat.cavegenerator.util.CommonMethods.*;

/**
 * This is a variant of {@link NoiseSettings2D} which contains additional fields
 * specifically relevant to 3-dimensional noise generation. I am looking into how
 * I can remove one or both of them, as they are both almost completely redundant.
 */
@Builder(toBuilder = true)
@FieldDefaults(level = AccessLevel.PUBLIC, makeFinal = true)
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class NoiseSettings3D {

    /** A seed used for <b>producing</b> new seeds from a given value. */
    @Default Optional<Integer> seed = empty();

    /** The target waveform frequency produced by the generator. */
    @Default float frequency = 1.0f;

    /** Converts a range of 0-1 into a threshold for accepting output values. */
    @Default float scale = 0.5f;

    /** Scales the noise produced * x. */
    @Default float scaleY = 1.0f;

    /** The scale of gaps produced in fractal patterns. */
    @Default float lacunarity = 1.0f;

    /** The octave gain for fractal noise types. */
    @Default float gain = 0.5f;

    /** The maximum amount to warp coordinates when perturb is enabled. */
    @Default float perturbAmp = 1.0f;

    /** The frequency used in warping input coordinates. */
    @Default float perturbFreq = 1.0f;

    /** The maximum amount a cellular point can move off grid. (x-axis) */
    @Default float jitterX = 0.45f;

    /** The maximum amount a cellular point can move off grid. (y-axis) */
    @Default float jitterY = 0.45f;

    /** The maximum amount a cellular point can move off grid. (z-axis) */
    @Default float jitterZ = 0.45f;

    /** The number of generation passes, i.e. the resolution. */
    @Default int octaves = 3;

    /** The vertical offset applied to the noise generator. */
    @Default int offset = 0;

    /** Whether to apply a gradient perturb function. */
    @Default boolean perturb = false;

    /** Whether to invert the acceptable values generated. */
    @Default boolean invert = false;

    /** The type of interpolation to use. */
    @Default Interp interp = Interp.Hermite;

    /** The type of noise generator to run. */
    @Default NoiseType noiseType = NoiseType.SimplexFractal;

    /** Determines how the noise will be fractalized, if applicable. */
    @Default FractalType fractalType = FractalType.FBM;

    /** The type of distance function used with cellular noise types. */
    @Default CellularDistanceFunction distanceFunction = CellularDistanceFunction.Euclidean;

    /** The return type from cellular noise types. */
    @Default CellularReturnType returnType = CellularReturnType.Distance2;

    /** The noise type used when returnType is set to {@code NoiseLookup}. */
    @Default NoiseType cellularLookup = NoiseType.Simplex;

    /** Converts these settings into a regular {@link FastNoise} object. */
    public FastNoise getGenerator(long seed) {
        return new FastNoise(getSeed(seed))
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
            .SetCellularJitterX(jitterX)
            .SetCellularJitterY(jitterY)
            .SetCellularJitterZ(jitterZ)
            .SetScale(scale)
            .SetScaleY(scaleY)
            .SetOffset(offset);
    }

    /** Generates a new seed from the input `base` value. */
    private int getSeed(long base) {
        final Random rand = new Random(base);
        final int next = rand.nextInt();
        return seed.map(num -> {
            final FastNoise simple = new FastNoise(next);
            return Float.floatToIntBits(simple.GetNoise(num));
        }).orElse(next);
    }
}