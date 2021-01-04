package com.personthecat.cavegenerator.model.generator;

import com.personthecat.cavegenerator.model.NoiseSettings2D;
import fastnoise.FastNoise;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Value;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import org.hjson.JsonObject;

import java.util.Objects;

import static com.personthecat.cavegenerator.util.HjsonTools.*;
import static com.personthecat.cavegenerator.util.CommonMethods.*;

/** Data used for spawning giant layers of stone through ChunkPrimer. */
@Value
@AllArgsConstructor
@Builder(toBuilder = true)
public class StoneLayer {

    /** Mandatory fields to be initialized by the constructor. */
    IBlockState state;

    /** The y-coordinate to build this layer around. */
    int maxHeight;

    /** The generator used to create this layer. */
    FastNoise noise;

    /** The raw settings containing noise parameters. */
    @Default NoiseSettings2D settings = DEFAULT_NOISE;

    /** The default noise values used by this object. */
    public static final NoiseSettings2D DEFAULT_NOISE = NoiseSettings2D.builder()
        .frequency(0.015f)
        .scale(0.5f)
        .min(-7)
        .max(7)
        .build();

    /** From Json. */
    public static StoneLayer from(JsonObject layer) {
        return StoneLayer.builder()
            .state(getGuaranteedState(layer, "StoneLayer"))
            .maxHeight(getInt(layer, "maxHeight")
                .orElseThrow(() -> runEx("At least one StoneLayer does not define maxHeight.")))
            .noiseSettings(getNoiseSettingsOr(layer, "noise2D", DEFAULT_NOISE))
            .build();
    }

    @SuppressWarnings("unused") // Used by #builder
    public static class StoneLayerBuilder {
        StoneLayerBuilder noiseSettings(NoiseSettings2D settings) {
            Objects.requireNonNull(state, "You must define state before noise.");
            this.noise = settings.getGenerator(Block.getStateId(state));
            this.settings$value = settings;
            return this;
        }
    }
}