package com.personthecat.cavegenerator.world;

import com.personthecat.cavegenerator.config.CavePreset;
import com.personthecat.cavegenerator.data.ClusterSettings;
import com.personthecat.cavegenerator.world.feature.FeatureInfo;
import com.personthecat.cavegenerator.world.feature.PillarGenerator;
import com.personthecat.cavegenerator.world.feature.StalactiteGenerator;
import com.personthecat.cavegenerator.world.feature.StructureGenerator;
import com.personthecat.cavegenerator.world.generator.*;
import lombok.Builder;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkPrimer;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;

@Builder
public class GeneratorController {

    private final List<TunnelGenerator> tunnels;
    private final List<RavineGenerator> ravines;
    @Nullable private final CavernGenerator caverns;
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
            .caverns(preset.caverns.map(c -> new CavernGenerator(c, world)).orElse(null))
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
    public void earlyGenerate(World world, int x, int z, ChunkPrimer primer) {
        globalClusters.generate(world, world.rand, x, z, x, z, primer);
        for (LayerGenerator layer : layers) {
            layer.generate(world, world.rand, x, z, x, z, primer);
        }
        layeredClusters.generate(world, world.rand, x, z, x, z, primer);
        if (caverns != null) {
            caverns.generate(world, world.rand, x, z, x, z, primer);
        }
    }

    /** Generate all of the early, MapGenBase-style features that make up the bulk of the caves. */
    public void mapGenerate(World world, Random rand, int destX, int destZ, int x, int z, ChunkPrimer primer) {
        for (TunnelGenerator tunnel : tunnels) {
            tunnel.generate(world, rand, destX, destZ, x, z, primer);
        }
        for (RavineGenerator ravine : ravines) {
            ravine.generate(world, rand, destX, destZ, x, z, primer);
        }
    }

    /** Spawn all of the superficial decorations that take place later in the chunk generation cycle. */
    public void featureGenerate(FeatureInfo info) {
        pillars.forEach(p -> p.generate(info));
        stalactites.forEach(s -> s.generate(info));
        structures.forEach(s -> s.generate(info));
    }

}
