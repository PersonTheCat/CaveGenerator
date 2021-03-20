package com.personthecat.cavegenerator.data;

import com.personthecat.cavegenerator.util.HjsonMapper;
import fastnoise.FastNoise;
import fastnoise.FastNoise.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import net.minecraft.world.World;
import org.hjson.JsonObject;

import java.util.Optional;
import java.util.Random;

import static com.personthecat.cavegenerator.util.CommonMethods.empty;
import static com.personthecat.cavegenerator.util.CommonMethods.full;

/**
 * This is a variant of {@link NoiseRegionSettings} which contains additional fields
 * specifically relevant to 3-dimensional noise generation. I am looking into how
 * I can remove one or both of them, as they are both almost completely redundant.
 */
@FieldNameConstants
@Builder(toBuilder = true)
@FieldDefaults(level = AccessLevel.PUBLIC, makeFinal = true)
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class NoiseSettings {

    /** A seed used for <b>producing</b> new seeds from a given value. */
    @Default Optional<Integer> seed = empty();

    /** The target waveform frequency produced by the generator. */
    @Default float frequency = 1.0f;

    /** Converts a range of 0-1 into a threshold for accepting output values. */
    @Default float scale = 0.5f;

    /** Scales the noise produced * x. */
    @Default float skew = 1.0f;

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
    @Default NoiseType type = NoiseType.SimplexFractal;

    /** Determines how the noise will be fractalized, if applicable. */
    @Default FractalType fractal = FractalType.FBM;

    /** The type of distance function used with cellular noise types. */
    @Default CellularDistanceFunction distFunc = CellularDistanceFunction.Euclidean;

    /** The return type from cellular noise types. */
    @Default CellularReturnType returnType = CellularReturnType.Distance2;

    /** The noise type used when returnType is set to {@code NoiseLookup}. */
    @Default NoiseType cellularLookup = NoiseType.Simplex;

    public static NoiseSettings from(JsonObject json, NoiseSettings defaults) {
        return copyInto(json, defaults.toBuilder());
    }

    public static NoiseSettings from(JsonObject json) {
        return copyInto(json, builder());
    }

    private static NoiseSettings copyInto(JsonObject json, NoiseSettingsBuilder builder) {
        return new HjsonMapper(json)
            .mapInt(Fields.seed, i -> builder.seed(full(i)))
            .mapFloat(Fields.frequency, builder::frequency)
            .mapFloat(Fields.scale, builder::scale)
            .mapFloat(Fields.skew, builder::skew)
            .mapFloat(Fields.lacunarity, builder::lacunarity)
            .mapFloat(Fields.gain, builder::gain)
            .mapFloat(Fields.perturbAmp, builder::perturbAmp)
            .mapFloat(Fields.perturbFreq, builder::perturbFreq)
            .mapFloat("jitter", i -> builder.jitterX(i).jitterY(i).jitterZ(i))
            .mapFloat(Fields.jitterX, builder::jitterX)
            .mapFloat(Fields.jitterY, builder::jitterY)
            .mapFloat(Fields.jitterZ, builder::jitterZ)
            .mapInt(Fields.octaves, builder::octaves)
            .mapInt(Fields.offset, builder::offset)
            .mapBool(Fields.perturb, builder::perturb)
            .mapBool(Fields.invert, builder::invert)
            .mapInterp(Fields.interp, builder::interp)
            .mapNoiseType(Fields.type, builder::type)
            .mapFractalType(Fields.fractal, builder::fractal)
            .mapDistFunc(Fields.distFunc, builder::distFunc)
            .mapReturnType(Fields.returnType, builder::returnType)
            .mapNoiseType(Fields.cellularLookup, builder::cellularLookup)
            .release(builder::build);
    }

    /** Converts these settings into a regular {@link FastNoise} object. */
    public FastNoise getGenerator(World world) {
        return new FastNoise(getSeed(world))
            .SetNoiseType(type)
            .SetFrequency(frequency)
            .SetFractalType(fractal)
            .SetFractalOctaves(octaves)
            .SetInvert(invert)
            .SetFractalGain(gain)
            .SetCellularReturnType(returnType)
            .SetGradientPerturbAmp(perturbAmp)
            .SetGradientPerturbFrequency(perturbFreq)
            .SetGradientPerturb(perturb)
            .SetFractalLacunarity(lacunarity)
            .SetCellularNoiseLookup(cellularLookup)
            .SetCellularDistanceFunction(distFunc)
            .SetInterp(interp)
            .SetCellularJitterX(jitterX)
            .SetCellularJitterY(jitterY)
            .SetCellularJitterZ(jitterZ)
            .SetScale(scale)
            .SetScaleY(skew)
            .SetOffset(offset);
    }

    /** Generates a new seed from the input `base` value. */
    private int getSeed(World world) {
        return seed.map(num -> {
            final Random rand = new Random(world.getSeed());
            final FastNoise simple = new FastNoise(rand.nextInt());
            return Float.floatToIntBits(simple.GetNoise(num));
        }).orElseGet(world.rand::nextInt);
    }
}