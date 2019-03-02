package com.personthecat.cavegenerator.world;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import org.hjson.JsonObject;

import static com.personthecat.cavegenerator.util.HjsonTools.*;

/** Data used for spawning giant clusters of stone through ChunkPrimer. */
public class StoneCluster {
    /** Mandatory fields that must be initialized by the constructor. */
    private final IBlockState state;
    /** A value from 0.0 to 92.0 which determines this cluster's frequency. */
    private final double selectionThreshold;
    /** The original value used for indicating spawn rates. */
    private final double frequency;
    private final int radiusX, radiusY, radiusZ;
    private final int radiusVariance;
    private final int startHeight;
    private final int heightVariance;

    /** A field indicating the seed for this stone cluster. */
    private final int ID;

    /** Where frequency is a value between 0.0 and 1.0. */
    public StoneCluster(
        IBlockState state,
        double frequency,
        int radiusX,
        int radiusY,
        int radiusZ,
        int radiusVariance,
        int startHeight,
        int heightVariance
    ) {
        this.state = state;
        this.frequency = frequency;
        this.selectionThreshold = (1.0 - frequency) * 92.0;
        this.radiusX = radiusX;
        this.radiusY = radiusY;
        this.radiusZ = radiusZ;
        this.radiusVariance = radiusVariance;
        this.startHeight = startHeight;
        this.heightVariance = heightVariance;
        this.ID = Block.getStateId(state);
    }

    /** From Json */
    public StoneCluster(IBlockState state, JsonObject cluster) {
        this(
            state,
            getFloatOr(cluster, "frequency", 0.15f),
            getIntOr(cluster,"radiusX", 16),
            getIntOr(cluster, "radiusY", 12),
            getIntOr(cluster, "radiusZ", 16),
            getIntOr(cluster, "radiusVariance", 6),
            getIntOr(cluster, "startHeight", 32),
            getIntOr(cluster, "heightVariance", 16)
        );
    }

    public IBlockState getState() {
        return state;
    }

    public double getSelectionThreshold() {
        return selectionThreshold;
    }

    public double getFrequency() {
        return frequency;
    }

    public int getRadiusX() {
        return radiusX;
    }

    public int getRadiusY() {
        return radiusY;
    }

    public int getRadiusZ() {
        return radiusZ;
    }

    public int getRadiusVariance() {
        return radiusVariance;
    }

    public int getStartHeight() {
        return startHeight;
    }

    public int getHeightVariance() {
        return heightVariance;
    }

    public int getID() {
        return ID;
    }

    /** Generated info related to how the current cluster will be spawned in the world. */
    public static class ClusterInfo {
        private final BlockPos center;

        /** Storing the original vertical radius to avoid unnecessary calculations. */
        private final int radiusY;

        /** Squared radii. */
        private final int radiusX2;
        private final int radiusY2;
        private final int radiusZ2;

        /** A reference to the original cluster to be spawned. */
        private final StoneCluster cluster;

        public ClusterInfo(StoneCluster cluster, BlockPos center, int radiusY, int radiusX2, int radiusY2, int radiusZ2) {
            this.cluster = cluster;
            this.center = center;
            this.radiusY = radiusY;
            this.radiusX2 = radiusX2;
            this.radiusY2 = radiusY2;
            this.radiusZ2 = radiusZ2;
        }

        public BlockPos getCenter() {
            return center;
        }

        public int getRadiusY() {
            return radiusY;
        }

        public int getRadiusX2() {
            return radiusX2;
        }

        public int getRadiusY2() {
            return radiusY2;
        }

        public int getRadiusZ2() {
            return radiusZ2;
        }

        public StoneCluster getCluster() {
            return cluster;
        }
    }
}