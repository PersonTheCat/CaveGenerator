package com.personthecat.cavegenerator.world.generator;

import com.personthecat.cavegenerator.data.ClusterSettings;
import com.personthecat.cavegenerator.model.Conditions;
import com.personthecat.cavegenerator.util.MultiValueIdentityMap;
import com.personthecat.cavegenerator.util.XoRoShiRo;
import com.personthecat.cavegenerator.world.RandomChunkSelector;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.ChunkPrimer;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Map;
import java.util.Random;

public class ClusterGenerator extends ListGenerator<ClusterSettings> {

    private final MultiValueIdentityMap<Conditions, ClusterInfo> clusterMap = new MultiValueIdentityMap<>();
    private final RandomChunkSelector selector;

    public ClusterGenerator(List<ClusterSettings> cfg, World world) {
        super(cfg, c -> c.conditions, world);
        this.selector = new RandomChunkSelector(world.getSeed());
    }

    @Override
    public void generate(PrimerContext ctx) {
        if (!features.isEmpty()) {
            generateChecked(ctx);
        }
    }

    @Override
    protected void generateChecked(PrimerContext ctx) {
        // Always reset the seed for clusters.
        ctx.world.rand.setSeed(ctx.world.getSeed());
        this.clusterMap.clear();
        this.locateFinalClusters(ctx.world, ctx.world.rand, ctx.chunkX, ctx.chunkZ);
        this.generateClusters(ctx.primer, ctx.localRand, ctx.chunkX, ctx.chunkZ);
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
                    final Biome b = world.getBiomeProvider().getBiome(new BlockPos(x, 0, z));
                    if (conditions.biomes.test(b)) {
                        for (Pair<IBlockState, Integer> pair : cfg.states) {
                            final IBlockState state = pair.getLeft();
                            final int id = pair.getRight();

                            if (selector.getBooleanForCoordinates(id, cX, cZ, threshold)) {
                                // Get an RNG unique to this chunk.
                                final Random localRand = new XoRoShiRo(cX ^ cZ ^ clusterSeed);
                                final int y = cfg.centerHeight.rand(localRand);
                                // Finalize all values.
                                final BlockPos origin = new BlockPos(x, y, z);
                                final int radX = cfg.radiusX.rand(localRand) - (cfg.radiusX.diff() / 2);
                                final int radY = cfg.radiusY.rand(localRand) - (cfg.radiusY.diff() / 2);
                                final int radZ = cfg.radiusZ.rand(localRand) - (cfg.radiusZ.diff() / 2);
                                // Add the new information to be returned.
                                this.clusterMap.add(conditions, new ClusterInfo(cfg, state, id, origin, radX, radY, radZ));
                            }
                        }
                    }
                }
            }
        });
    }

    private void generateClusters(ChunkPrimer primer, Random rand, int chunkX, int chunkZ) {
        for (int x = 0; x < 16; x++) {
            final int actualX = x + (chunkX * 16);
            for (int z = 0; z < 16; z++) {
                final int actualZ = z + (chunkZ * 16);
                this.spawnColumn(primer, rand, x, z, actualX, actualZ);
            }
        }
    }

    private void spawnColumn(ChunkPrimer primer, Random rand, int x, int z, int actualX, int actualZ) {
        for (Map.Entry<Conditions, List<ClusterInfo>> entry : clusterMap.entrySet()) {
            final Conditions conditions = entry.getKey();

            for (int y : conditions.getColumn(actualX, actualZ)) {
                if (conditions.noise.GetBoolean(actualX, y, actualZ)) {
                    spawnCluster(entry.getValue(), primer, rand, x, y, z, actualX, actualZ);
                }
            }
        }
    }

    private static void spawnCluster(List<ClusterInfo> clusters, ChunkPrimer primer, Random rand, int x, int y, int z, int actualX, int actualZ) {
        final IBlockState state = primer.getBlockState(x, y, z);
        for (ClusterInfo info : clusters) {
            if (info.cluster.canSpawn(state)) {
                final BlockPos origin = info.center;
                final double distX = actualX - origin.getX();
                final double distY = y - origin.getY();
                final double distZ = actualZ - origin.getZ();
                final double distX2 = distX * distX;
                final double distY2 = distY * distY;
                final double distZ2 = distZ * distZ;

                // Ensure that we're within the sphere. Note: wall blocks could be && sum >= 0.9
                if (distX2 / info.radX2 + distY2 / info.radY2 + distZ2 / info.radZ2 <= 1) {
                    final double chance = info.cluster.integrity;
                    if (chance == 1.0 || rand.nextFloat() <= chance) {
                        primer.setBlockState(x, y, z, info.state);
                        return; // Already placed. Don't continue.
                    }
                }
            }
        }
    }

    /** Generated info related to how the current cluster will be spawned in the world. */
    private static class ClusterInfo {

        /** A reference to the original cluster to be spawned. */
        final ClusterSettings cluster;
        final IBlockState state;
        final int id;

        /** The generated center coordinates of this cluster. */
        final BlockPos center;

        /** Storing the original vertical radius to avoid unnecessary calculations. */
        final int radY;

        /** Squared radii. */
        final int radX2;
        final int radY2;
        final int radZ2;

        ClusterInfo(ClusterSettings cluster, IBlockState state, int id, BlockPos center, int radX, int radY, int radZ) {
            this.cluster = cluster;
            this.state = state;
            this.id = id;
            this.center = center;
            this.radY = radY;
            this.radY2 = radY * radY;
            this.radX2 = radX * radX;
            this.radZ2 = radZ * radZ;
        }
    }
}
