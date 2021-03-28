package com.personthecat.cavegenerator.world;

import com.personthecat.cavegenerator.config.CavePreset;
import com.personthecat.cavegenerator.data.ClusterSettings;
import com.personthecat.cavegenerator.world.feature.PillarGenerator;
import com.personthecat.cavegenerator.world.feature.StalactiteGenerator;
import com.personthecat.cavegenerator.world.feature.StructureGenerator;
import com.personthecat.cavegenerator.world.feature.WorldContext;
import com.personthecat.cavegenerator.world.generator.*;
import lombok.Builder;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Builder
public class GeneratorController {
    private final List<TunnelGenerator> tunnels;
    private final List<RavineGenerator> ravines;
    private final List<CavernGenerator> caverns;
    private final List<LayerGenerator> layers;
    private final ClusterGenerator globalClusters;
    private final ClusterGenerator layeredClusters;
    private final List<StalactiteGenerator> stalactites;
    private final List<PillarGenerator> pillars;
    private final List<StructureGenerator> structures;

    public static GeneratorController from(CavePreset preset, World world) {
        final GeneratorControllerBuilder builder = builder()
            .tunnels(map(preset.tunnels, t -> new TunnelGenerator(t, world)))
            .ravines(map(preset.ravines, r -> new RavineGenerator(r, world)))
            .caverns(map(preset.caverns, r -> new CavernGenerator(r, world)))
            .layers(map(preset.layers, l -> new LayerGenerator(l, world)))
            .stalactites(map(preset.stalactites, s -> new StalactiteGenerator(s, world)))
            .pillars(map(preset.pillars, p -> new PillarGenerator(p, world)))
            .structures(map(preset.structures, s -> new StructureGenerator(s, world)));
        sortClusters(preset.clusters, world, builder);
        return builder.build();
    }

    private static <T, U> List<U> map(List<T> list, Function<T, U> mapper) {
        return list.stream().map(mapper).collect(Collectors.toList());
    }

    private static void sortClusters(List<ClusterSettings> clusters, World world, GeneratorControllerBuilder builder) {
        final List<ClusterSettings> global = new ArrayList<>();
        final List<ClusterSettings> layered = new ArrayList<>();
        clusters.forEach(c -> (c.matchers.isEmpty() ? global : layered).add(c));
        builder.globalClusters(new ClusterGenerator(global, world))
            .layeredClusters(new ClusterGenerator(layered, world));
    }

    /** Generate noise-based features in the world before anything else.. */
    public void earlyGenerate(PrimerContext ctx) {
        globalClusters.generate(ctx);
        layers.forEach(l -> l.generate(ctx));
        layeredClusters.generate(ctx);
        caverns.forEach(c -> c.generate(ctx));
    }

    /** Generate all of the early, MapGenBase-style features that make up the bulk of the caves. */
    public void mapGenerate(PrimerContext ctx) {
        tunnels.forEach(t -> t.generate(ctx));
        ravines.forEach(r -> r.generate(ctx));
    }

    /** Spawn all of the superficial decorations that take place later in the chunk generation cycle. */
    public void featureGenerate(WorldContext ctx) {
        pillars.forEach(p -> p.generate(ctx));
        stalactites.forEach(s -> s.generate(ctx));
        structures.forEach(s -> s.generate(ctx));
    }

}
