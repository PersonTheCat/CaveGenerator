package personthecat.cavegenerator.world;

import lombok.Builder;
import personthecat.cavegenerator.world.config.BurrowConfig;
import personthecat.cavegenerator.world.config.CavernConfig;
import personthecat.cavegenerator.world.config.ClusterConfig;
import personthecat.cavegenerator.world.config.LayerConfig;
import personthecat.cavegenerator.world.feature.PillarGenerator;
import personthecat.cavegenerator.world.feature.StalactiteGenerator;
import personthecat.cavegenerator.world.feature.StructureGenerator;
import personthecat.cavegenerator.world.feature.WorldContext;
import personthecat.cavegenerator.world.generator.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Builder
public class GeneratorController {

    private final List<TunnelGenerator> tunnels;
    private final List<RavineGenerator> ravines;
    private final List<CavernGenerator> caverns;
    private final List<BurrowGenerator> burrows;
    private final List<LayerGenerator> layers;
    private final List<TunnelConnector<CavernGenerator>> cavernTunnels;
    private final List<TunnelConnector<BurrowGenerator>> burrowTunnels;
    private final ClusterGenerator globalClusters;
    private final ClusterGenerator layeredClusters;
    private final List<StalactiteGenerator> stalactites;
    private final List<PillarGenerator> pillars;
    private final List<StructureGenerator> structures;

    public void earlyGenerate(final PrimerContext ctx) {
        globalClusters.generate(ctx);
        layers.forEach(l -> l.generate(ctx));
        layeredClusters.generate(ctx);
        caverns.forEach(c -> c.generate(ctx));
        burrows.forEach(b -> b.generate(ctx));
    }

    public void mapGenerate(final PrimerContext ctx) {
        tunnels.forEach(t -> t.generate(ctx));
        ravines.forEach(r -> r.generate(ctx));
        cavernTunnels.forEach(t -> t.generate(ctx));
        burrowTunnels.forEach(t -> t.generate(ctx));
    }

    public void featureGenerate(final WorldContext ctx) {
        stalactites.forEach(s -> s.generate(ctx));
        pillars.forEach(p -> p.generate(ctx));
        structures.forEach(s -> s.generate(ctx));
    }

    public static class GeneratorControllerBuilder {
        public GeneratorControllerBuilder sortClusters(final List<ClusterConfig> clusters, final Random rand, final long seed) {
            final List<ClusterConfig> global = new ArrayList<>();
            final List<ClusterConfig> layered = new ArrayList<>();
            clusters.forEach(c -> (c.matchers.isEmpty() ? global: layered).add(c));
            return this.globalClusters(new ClusterGenerator(global, rand, seed))
                .layeredClusters(new ClusterGenerator(layered, rand, seed));
        }

        public GeneratorControllerBuilder sortCaverns(final List<CavernConfig> caverns, final Random rand, final long seed) {
            final List<CavernGenerator> generators = new ArrayList<>();
            final List<TunnelConnector<CavernGenerator>> connectors = new ArrayList<>();
            for (final CavernConfig cavern : caverns) {
                final CavernGenerator generator = new CavernGenerator(cavern, rand, seed);
                generators.add(generator);
                if (cavern.branches != null) {
                    connectors.add(new TunnelConnector<>(cavern.branches, generator, rand, seed));
                }
            }
            return this.caverns(generators).cavernTunnels(connectors);
        }

        public GeneratorControllerBuilder sortBurrows(final List<BurrowConfig> burrows, final Random rand, final long seed) {
            final List<BurrowGenerator> generators = new ArrayList<>();
            final List<TunnelConnector<BurrowGenerator>> connectors = new ArrayList<>();
            for (final BurrowConfig burrow : burrows) {
                final BurrowGenerator generator = new BurrowGenerator(burrow, rand, seed);
                generators.add(generator);
                if (burrow.branches != null) {
                    connectors.add(new TunnelConnector<>(burrow.branches, generator, rand, seed));
                }
            }
            return this.burrows(generators).burrowTunnels(connectors);
        }
    }
}
