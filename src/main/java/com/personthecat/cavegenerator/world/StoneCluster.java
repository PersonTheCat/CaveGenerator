package com.personthecat.cavegenerator.world;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;

/** Data used for spawning giant clusters of stone through ChunkPrimer. */
public class StoneCluster {
    /** Mandatory fields that must be initialized by the constructor. */
    private final IBlockState state;
    private final double selectionThreshold;
    private final int radius;
    private final int radiusVariance;
    private final int startingHeight;
    private final int heightVariance;

    /** A late-init field indicating the seed for this stone cluster. */
    private int ID = -1;

    public StoneCluster(
        IBlockState state,
        double selectionThreshold,
        int radius,
        int radiusVariance,
        int startingHeight,
        int heightVariance
    ) {
        this.state = state;
        this.selectionThreshold = selectionThreshold;
        this.radius = radius;
        this.radiusVariance = radiusVariance;
        this.startingHeight = startingHeight;
        this.heightVariance = heightVariance;
    }

    public IBlockState getState() {
        return state;
    }

    public double getSelectionThreshold() {
        return selectionThreshold;
    }

    public int getRadius() {
        return radius;
    }

    public int getRadiusVariance() {
        return radiusVariance;
    }

    public int getStartingHeight() {
        return startingHeight;
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