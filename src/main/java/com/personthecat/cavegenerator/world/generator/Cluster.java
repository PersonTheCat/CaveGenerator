package com.personthecat.cavegenerator.world.generator;

import com.personthecat.cavegenerator.model.NoiseSettings3D;
import com.personthecat.cavegenerator.util.LazyFunction;
import fastnoise.FastNoise;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Value;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import org.hjson.JsonObject;

import java.util.Optional;
import java.util.Random;

import static com.personthecat.cavegenerator.util.HjsonTools.*;
import static com.personthecat.cavegenerator.util.CommonMethods.*;

/** Data used for spawning giant clusters of stone through ChunkPrimer. */
@Value
@Builder(toBuilder = true)
@AllArgsConstructor
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class Cluster {

    /** The block state to be placed by the generator. */
    IBlockState state;

    /** A value from 0.0 to 92.0 which determines this cluster's frequency. */
    @Default double selectionThreshold = 78.2;

    /** The original value used for indicating spawn rates. */
    @Default double chance = 0.15;

    /** Radius on the x-axis. */
    @Default int radiusX = 16;

    /** Radius on the y-axis. */
    @Default int radiusY = 12;

    /** Radius on the z-axis. */
    @Default int radiusZ = 16;

    /** How much to vary radius of this cluster. */
    @Default int radiusVariance = 6;

    /** Starting y-coordinate for this cluster. */
    @Default int startHeight = 32;

    /** How much to vary the y-coordinate of this cluster. */
    @Default int heightVariance = 16;

    /** A field indicating the seed for this cluster. */
    @Default int ID = 0;

    /** Noise settings to use in placement. */
    @Default Optional<NoiseSettings3D> noiseSettings = empty();

    /** Returns a noise generator for this cluster. */
    LazyFunction<Long, Optional<FastNoise>> noise = new LazyFunction<>(this::getNoise);

    /** Default values for cluster noise. */
    public static final NoiseSettings3D DEFAULT_NOISE =
        NoiseSettings3D.builder()
            .frequency(0.0143f)
            .scale(0.2f)
            .scaleY(0.5f)
            .octaves(1)
            .build();

    /** From Json */
    public static Cluster from(IBlockState state, JsonObject cluster) {
        final ClusterBuilder builder = Cluster.builder()
            .state(state)
            .ID(Block.getStateId(state));
        final Optional<NoiseSettings3D> noise = getObject(cluster, "noise3D")
            .map(o -> toNoiseSettings(o, DEFAULT_NOISE));
        builder.noiseSettings(noise);
        getFloat(cluster, "chance").ifPresent(chance -> {
            builder.chance(chance);
            builder.selectionThreshold((1.0 - chance) * 92.0);
        });
        getInt(cluster, "radiusX").ifPresent(builder::radiusX);
        getInt(cluster, "radiusY").ifPresent(builder::radiusY);
        getInt(cluster, "radiusZ").ifPresent(builder::radiusZ);
        getInt(cluster, "radiusVariance").ifPresent(builder::radiusVariance);
        getInt(cluster, "startHeight").ifPresent(builder::startHeight);
        getInt(cluster, "heightVariance").ifPresent(builder::heightVariance);
        return builder.build();
    }

    /** Generates a new seeded generator based on `seed` and `ID`. */
    private Optional<FastNoise> getNoise(long seed) {
        return noiseSettings.map(settings -> {
            final Random rand = new Random(seed);
            final FastNoise simple = new FastNoise(rand.nextInt());
            final int hash = Float.floatToIntBits(simple.GetNoise(ID));
            return settings.getGenerator(hash);
        });
    }

    /** Sets up the noise generator for this cluster. */
    public void initNoise(long seed) {
        noise.apply(seed);
    }

    /** Returns whether this cluster is valid at these coordinates. */
    public boolean canSpawn(float x, float y, float z) {
        return noise.apply(null) // Already loaded.
            .map(noise -> noise.GetBoolean(x, y, z))
            .orElse(true);
    }

    /** Generated info related to how the current cluster will be spawned in the world. */
    @Value
    @AllArgsConstructor
    public static class ClusterInfo {

        /** A reference to the original cluster to be spawned. */
        Cluster cluster;

        /** The generated center coordinates of this cluster. */
        BlockPos center;

        /** Storing the original vertical radius to avoid unnecessary calculations. */
        int radiusY;

        /** Squared radii. */
        int radiusX2;
        int radiusY2;
        int radiusZ2;
    }
}