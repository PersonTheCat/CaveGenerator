package com.personthecat.cavegenerator.world;

import com.personthecat.cavegenerator.config.CavePreset;
import com.personthecat.cavegenerator.data.BurrowSettings;
import com.personthecat.cavegenerator.data.CavernSettings;
import com.personthecat.cavegenerator.data.ClusterSettings;
import com.personthecat.cavegenerator.world.feature.*;
import com.personthecat.cavegenerator.world.generator.*;
import lombok.Builder;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

import static com.personthecat.cavegenerator.util.CommonMethods.map;

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

    public static GeneratorController from(CavePreset preset, World world) {
        final GeneratorControllerBuilder builder = builder()
            .tunnels(map(preset.tunnels, t -> new TunnelGenerator(t, world)))
            .ravines(map(preset.ravines, r -> new RavineGenerator(r, world)))
            .layers(map(preset.layers, l -> new LayerGenerator(l, world)))
            .stalactites(map(preset.stalactites, s -> new StalactiteGenerator(s, world)))
            .pillars(map(preset.pillars, p -> new PillarGenerator(p, world)))
            .structures(map(preset.structures, s -> new StructureGenerator(s, world)));
        sortClusters(preset.clusters, world, builder);
        mapCaverns(preset.caverns, world, builder);
        mapBurrows(preset.burrows, world, builder);
        return builder.build();
    }

    private static void sortClusters(List<ClusterSettings> clusters, World world, GeneratorControllerBuilder builder) {
        final List<ClusterSettings> global = new ArrayList<>();
        final List<ClusterSettings> layered = new ArrayList<>();
        clusters.forEach(c -> (c.matchers.isEmpty() ? global : layered).add(c));
        builder.globalClusters(new ClusterGenerator(global, world))
            .layeredClusters(new ClusterGenerator(layered, world));
    }

    private static void mapCaverns(List<CavernSettings> caverns, World world, GeneratorControllerBuilder builder) {
        final List<CavernGenerator> generators = new ArrayList<>();
        final List<TunnelConnector<CavernGenerator>> connectors = new ArrayList<>();
        for (CavernSettings cavern : caverns) {
            final CavernGenerator generator = new CavernGenerator(cavern, world);
            generators.add(generator);
            cavern.branches.ifPresent(b -> connectors.add(new TunnelConnector<>(b, generator, world)));
        }
        builder.caverns(generators);
        builder.cavernTunnels(connectors);
    }

    private static void mapBurrows(List<BurrowSettings> burrows, World world, GeneratorControllerBuilder builder) {
        final List<BurrowGenerator> generators = new ArrayList<>();
        final List<TunnelConnector<BurrowGenerator>> connectors = new ArrayList<>();
        for (BurrowSettings burrow : burrows) {
            final BurrowGenerator generator = new BurrowGenerator(burrow, world);
            generators.add(generator);
            burrow.branches.ifPresent(b -> connectors.add(new TunnelConnector<>(b, generator, world)));
        }
        builder.burrows(generators);
        builder.burrowTunnels(connectors);
    }

    /** Generate noise-based features in the world before anything else. */
    public void earlyGenerate(PrimerContext ctx) {
        globalClusters.generate(ctx);
        layers.forEach(l -> l.generate(ctx));
        layeredClusters.generate(ctx);
        caverns.forEach(c -> c.generate(ctx));
        burrows.forEach(t -> t.generate(ctx));
    }

    /** Generate all of the early, MapGenBase-style features that make up the bulk of the caves. */
    public void mapGenerate(PrimerContext ctx) {
        tunnels.forEach(t -> t.generate(ctx));
        ravines.forEach(r -> r.generate(ctx));
        cavernTunnels.forEach(t -> t.generate(ctx));
        burrowTunnels.forEach(t -> t.generate(ctx));
    }

    /** Spawn all of the superficial decorations that take place later in the chunk generation cycle. */
    public void featureGenerate(WorldContext ctx) {
        pillars.forEach(p -> p.generate(ctx));
        stalactites.forEach(s -> s.generate(ctx));
        structures.forEach(s -> s.generate(ctx));
    }

}
