package com.personthecat.cavegenerator.model.generator;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Value;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import org.hjson.JsonObject;

import static com.personthecat.cavegenerator.util.HjsonTools.*;

/** Data used for spawning giant clusters of stone through ChunkPrimer. */
@Value
@Builder(toBuilder = true)
@AllArgsConstructor
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

    /** From Json */
    public static Cluster from(IBlockState state, JsonObject cluster) {
        final ClusterBuilder builder = Cluster.builder()
            .state(state)
            .ID(Block.getStateId(state));

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