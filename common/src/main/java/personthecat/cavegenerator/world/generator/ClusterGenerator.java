package personthecat.cavegenerator.world.generator;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.tuple.Pair;
import personthecat.catlib.data.MultiValueIdentityMap;
import personthecat.catlib.data.MultiValueMap;
import personthecat.catlib.util.RandomChunkSelector;
import personthecat.cavegenerator.util.XoRoShiRo;
import personthecat.cavegenerator.world.config.ClusterConfig;
import personthecat.cavegenerator.world.config.ConditionConfig;

import java.util.List;
import java.util.Map;
import java.util.Random;

public class ClusterGenerator extends ListGenerator<ClusterConfig> {

    private final MultiValueMap<ConditionConfig, ClusterInfo> clusterMap = new MultiValueIdentityMap<>();
    private final RandomChunkSelector selector;

    public ClusterGenerator(List<ClusterConfig> cfg, final Random rand, final long seed) {
        super(cfg, c -> c.conditions, rand, seed);
        this.selector = new RandomChunkSelector(seed);
    }

    @Override
    public void generate(PrimerContext ctx) {
        if (!features.isEmpty()) {
            generateChecked(ctx);
        }
    }

    @Override
    protected void generateChecked(final PrimerContext ctx) {
        // Always reset the seed for clusters.
        ctx.localRand.setSeed(this.worldSeed);
        this.clusterMap.clear();
        this.locateFinalClusters(ctx);
        this.generateClusters(ctx);
    }

    private void locateFinalClusters(final PrimerContext ctx) {
        forEachFeature((cfg, conditions) -> {
            final int cRadiusX = (cfg.radiusX.max / 16) + 1;
            final int cRadiusZ = (cfg.radiusZ.max / 16) + 1;
            final double threshold = cfg.selectionThreshold;
            final int clusterSeed = ctx.localRand.nextInt();

            // Locate any possible origins for this cluster based on its radii.
            for (int cX = ctx.chunkX - cRadiusX; cX <= ctx.chunkX + cRadiusX; cX++) {
                for (int cZ = ctx.chunkZ - cRadiusZ; cZ <= ctx.chunkZ + cRadiusZ; cZ++) {

                    // Get absolute coordinates, generate in the center.
                    final int x = (cX * 16) + 8, z = (cZ * 16) + 8;
                    final Biome b = ctx.provider.getBiome(new BlockPos(x, 0, z));
                    if (conditions.biomes.test(b)) {
                        for (Pair<BlockState, Integer> pair : cfg.states) {
                            final BlockState state = pair.getLeft();
                            final int id = pair.getRight();

                            if (selector.testCoordinates(id, cX, cZ, threshold)) {
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

    private void generateClusters(PrimerContext ctx) {
        for (int x = 0; x < 16; x++) {
            final int aX = ctx.actualX + x;
            for (int z = 0; z < 16; z++) {
                final int aZ = ctx.actualZ + z;
                this.spawnColumn(ctx, x, z, aX, aZ);
            }
        }
    }

    private void spawnColumn(PrimerContext ctx, int x, int z, int aX, int aZ) {
        for (final Map.Entry<ConditionConfig, List<ClusterInfo>> entry : clusterMap.entrySet()) {
            final ConditionConfig conditions = entry.getKey();

            for (int y : conditions.getColumn(aX, aZ)) {
                if (conditions.noise.getBoolean(aX, y, aZ)) {
                    spawnCluster(ctx, entry.getValue(), x, y, z, aX, aZ);
                }
            }
        }
    }

    private static void spawnCluster(PrimerContext ctx, List<ClusterInfo> clusters, int x, int y, int z, int aX, int aZ) {
        final BlockState state = ctx.get(x, y, z);
        for (ClusterInfo info : clusters) {
            if (info.cluster.canSpawn(state)) {
                final BlockPos origin = info.center;
                final double distX = aX - origin.getX();
                final double distY = y - origin.getY();
                final double distZ = aZ - origin.getZ();
                final double distX2 = distX * distX;
                final double distY2 = distY * distY;
                final double distZ2 = distZ * distZ;

                // Ensure that we're within the sphere. Note: wall blocks could be && sum >= 0.9
                if (distX2 / info.radX2 + distY2 / info.radY2 + distZ2 / info.radZ2 <= 1) {
                    final double chance = info.cluster.integrity;
                    if (chance == 1.0 || ctx.localRand.nextFloat() <= chance) {
                        ctx.set(x, y, z, info.state);
                        return; // Already placed. Don't continue.
                    }
                }
            }
        }
    }

    /** Generated info related to how the current cluster will be spawned in the world. */
    private static class ClusterInfo {

        /** A reference to the original cluster to be spawned. */
        final ClusterConfig cluster;
        final BlockState state;
        final int id;

        /** The generated center coordinates of this cluster. */
        final BlockPos center;

        /** Storing the original vertical radius to avoid unnecessary calculations. */
        final int radY;

        /** Squared radii. */
        final int radX2;
        final int radY2;
        final int radZ2;

        ClusterInfo(ClusterConfig cluster, BlockState state, int id, BlockPos center, int radX, int radY, int radZ) {
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
