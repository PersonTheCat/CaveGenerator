package com.personthecat.cavegenerator.world.generator;

import com.personthecat.cavegenerator.data.ClusterSettings;
import com.personthecat.cavegenerator.model.Conditions;
import com.personthecat.cavegenerator.util.IdentityMultiValueMap;
import com.personthecat.cavegenerator.world.RandomChunkSelector;
import lombok.AllArgsConstructor;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraft.world.chunk.ChunkPrimer;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Map;
import java.util.Random;

public class ClusterGenerator extends ListGenerator<ClusterSettings> {

    private final IdentityMultiValueMap<Conditions, ClusterInfo> clusterMap = new IdentityMultiValueMap<>();
    private final RandomChunkSelector selector;

    public ClusterGenerator(List<ClusterSettings> cfg, World world) {
        super(cfg, c -> c.conditions, world);
        this.selector = new RandomChunkSelector(world.getSeed());
    }

    @Override
    protected void doGenerate(World world, Random rand, int destChunkX, int destChunkZ, int chunkX, int chunkZ, ChunkPrimer primer) {
        if (!features.isEmpty()) {
            // Always reset the seed for clusters.
            rand.setSeed(world.getSeed());
            clusterMap.clear();
            locateFinalClusters(world, rand, chunkX, chunkZ);
            generateClusters(primer, chunkX, chunkZ);
        }
    }

    private void locateFinalClusters(World world, Random rand, int chunkX, int chunkZ) {
        forEachFeature((cfg, conditions) -> {
            final int cRadiusX = (cfg.radiusX.max / 16) + 1;
            final int cRadiusZ = (cfg.radiusZ.max / 16) + 1;
            final double threshold = cfg.selectionThreshold;
            final int clusterSeed = rand.nextInt();

            // Locate any possible origins for this cluster based on its radii.
            for (int cX = chunkX - cRadiusX; cX <= chunkX + cRadiusX; cX++) {
                for (int cZ = chunkZ - cRadiusZ; cZ <= chunkZ + cRadiusZ; cZ++) {

                    // Get absolute coordinates, generate in the center.
                    final int x = (cX * 16) + 8, z = (cZ * 16) + 8;
                    if (conditions.biomes.test(getPredictBiome(world, x, z))) {
                        for (Pair<IBlockState, Integer> pair : cfg.states) {
                            final IBlockState state = pair.getLeft();
                            final int id = pair.getRight();

                            if (selector.getBooleanForCoordinates(id, cX, cZ, threshold)) {
                                // Get an RNG unique to this chunk.
                                final Random localRand = new Random(cX ^ cZ ^ clusterSeed);
                                final int y = cfg.centerHeight.rand(localRand);
                                // Finalize all values.
                                final BlockPos origin = new BlockPos(x, y, z);
                                final int radX = cfg.radiusX.rand(localRand) - (cfg.radiusX.diff() / 2);
                                final int radY = cfg.radiusY.rand(localRand) - (cfg.radiusY.diff() / 2);
                                final int radZ = cfg.radiusZ.rand(localRand) - (cfg.radiusZ.diff() / 2);
                                final int radiusX2 = radX * radX;
                                final int radiusY2 = radY * radY;
                                final int radiusZ2 = radZ * radZ;
                                // Add the new information to be returned.
                                clusterMap.add(conditions, new ClusterInfo(cfg, state, id, origin, radY, radiusX2, radiusY2, radiusZ2));
                            }
                        }
                    }
                }
            }
        });
    }

    private static Biome getPredictBiome(World world, int x, int z) {
        final BlockPos pos = new BlockPos(x + 2, 0, z + 2);
        final BiomeProvider provider = world.getBiomeProvider();
        // Unlike the original, this does not contain a try-catch.
        // May have to add that...
        if (world.isBlockLoaded(pos)) {
            return world.getChunk(pos).getBiome(pos, provider);
        }
        return provider.getBiomesForGeneration(null, x / 4, z / 4, 1, 1)[0];
    }

    private void generateClusters(ChunkPrimer primer, int chunkX, int chunkZ) {
        for (int x = 0; x < 16; x++) {
            final int actualX = x + (chunkX * 16);
            for (int z = 0; z < 16; z++) {
                final int actualZ = z + (chunkZ * 16);
                spawnCluster(primer, x, z, actualX, actualZ);
            }
        }
    }

    private void spawnCluster(ChunkPrimer primer, int x, int z, int actualX, int actualZ) {
        for (Map.Entry<Conditions, List<ClusterInfo>> entry : clusterMap.entrySet()) {
            final Conditions conditions = entry.getKey();
            for (int y : conditions.getColumn(actualX, actualZ)) {
                if (conditions.noise.GetBoolean(actualX, actualZ)) {
                    for (ClusterInfo info : entry.getValue()) {
                        final BlockPos origin = info.center;
                        final double distX = actualX - origin.getX();
                        final double distY = y - origin.getY();
                        final double distZ = actualZ - origin.getZ();
                        final double distX2 = distX * distX;
                        final double distY2 = distY * distY;
                        final double distZ2 = distZ * distZ;

                        // Ensure that we're within the sphere.
                        if (distX2 / info.radiusX2 + distY2 / info.radiusY2 + distZ2 / info.radiusZ2 <= 1) {
                            if (info.cluster.canSpawn(info.state)) {
                                primer.setBlockState(x, y, z, info.state);
                                return; // Already placed. Don't continue.
                            }
                        }
                    }
                }
            }
        }
    }

    /** Generated info related to how the current cluster will be spawned in the world. */
    @AllArgsConstructor
    public static class ClusterInfo {

        /** A reference to the original cluster to be spawned. */
        ClusterSettings cluster;
        IBlockState state;
        int id;

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
