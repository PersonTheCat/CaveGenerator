package personthecat.cavegenerator.world;

import lombok.Builder;
import personthecat.cavegenerator.presets.CavePreset;
import personthecat.cavegenerator.presets.data.BurrowSettings;
import personthecat.cavegenerator.presets.data.CavernSettings;
import personthecat.cavegenerator.presets.data.ClusterSettings;
import personthecat.cavegenerator.world.feature.PillarGenerator;
import personthecat.cavegenerator.world.feature.StalactiteGenerator;
import personthecat.cavegenerator.world.feature.StructureGenerator;
import personthecat.cavegenerator.world.feature.WorldContext;
import personthecat.cavegenerator.world.generator.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static personthecat.catlib.util.Shorthand.map;

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

    public static GeneratorController from(final CavePreset preset, final Random rand, final long seed) {
        return GeneratorController.builder()
            .tunnels(map(preset.tunnels, t -> new TunnelGenerator(t, rand, seed)))
            .ravines(map(preset.ravines, r -> new RavineGenerator(r, rand, seed)))
            .layers(map(preset.layers, l -> new LayerGenerator(l, rand, seed)))
            .stalactites(map(preset.stalactites, s -> new StalactiteGenerator(s, rand, seed)))
            .pillars(map(preset.pillars, p -> new PillarGenerator(p, rand, seed)))
            .structures(map(preset.structures, s -> new StructureGenerator(s, rand, seed)))
            .sortClusters(preset.clusters, rand, seed)
            .sortCaverns(preset.caverns, rand, seed)
            .sortBurrows(preset.burrows, rand, seed)
            .build();
    }

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
        GeneratorControllerBuilder sortClusters(List<ClusterSettings> clusters, final Random rand, final long seed) {
            final List<ClusterSettings> global = new ArrayList<>();
            final List<ClusterSettings> layered = new ArrayList<>();
            clusters.forEach(c -> (c.matchers.isEmpty() ? global: layered).add(c));
            return this.globalClusters(new ClusterGenerator(global, rand, seed))
                .layeredClusters(new ClusterGenerator(layered, rand, seed));
        }

        GeneratorControllerBuilder sortCaverns(List<CavernSettings> caverns, final Random rand, final long seed) {
            final List<CavernGenerator> generators = new ArrayList<>();
            final List<TunnelConnector<CavernGenerator>> connectors = new ArrayList<>();
            for (final CavernSettings cavern : caverns) {
                final CavernGenerator generator = new CavernGenerator(cavern, rand, seed);
                generators.add(generator);
                cavern.branches.ifPresent(b -> connectors.add(new TunnelConnector<>(b, generator, rand, seed)));
            }
            return this.caverns(generators).cavernTunnels(connectors);
        }

        GeneratorControllerBuilder sortBurrows(List<BurrowSettings> burrows, final Random rand, final long seed) {
            final List<BurrowGenerator> generators = new ArrayList<>();
            final List<TunnelConnector<BurrowGenerator>> connectors = new ArrayList<>();
            for (final BurrowSettings burrow : burrows) {
                final BurrowGenerator generator = new BurrowGenerator(burrow, rand, seed);
                generators.add(generator);
                burrow.branches.ifPresent(b -> connectors.add(new TunnelConnector<>(b, generator, rand, seed)));
            }
            return this.burrows(generators).burrowTunnels(connectors);
        }
    }
}
