package personthecat.cavegenerator.presets.data;

import com.mojang.serialization.Codec;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldNameConstants;
import personthecat.cavegenerator.world.GeneratorController;
import personthecat.cavegenerator.world.feature.PillarGenerator;
import personthecat.cavegenerator.world.feature.StalactiteGenerator;
import personthecat.cavegenerator.world.feature.StructureGenerator;
import personthecat.cavegenerator.world.generator.LayerGenerator;
import personthecat.cavegenerator.world.generator.RavineGenerator;
import personthecat.cavegenerator.world.generator.TunnelGenerator;

import java.util.Collections;
import java.util.List;
import java.util.Random;

import static personthecat.catlib.util.Shorthand.map;
import static personthecat.catlib.serialization.CodecUtils.codecOf;
import static personthecat.catlib.serialization.CodecUtils.easyList;
import static personthecat.catlib.serialization.FieldDescriptor.defaultGet;

@FieldNameConstants
@AllArgsConstructor
public class CaveSettings implements ConfigProvider<CaveSettings, GeneratorController> {
    public final List<BurrowSettings> burrows;
    public final List<CavernSettings> caverns;
    public final List<ClusterSettings> clusters;
    public final List<LayerSettings> layers;
    public final List<PillarSettings> pillars;
    public final List<RavineSettings> ravines;
    public final List<StalactiteSettings> stalactites;
    public final List<StructureSettings> structures;
    public final List<TunnelSettings> tunnels;

    public static final CaveSettings EMPTY =
        new CaveSettings(Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
            Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
            Collections.emptyList(), Collections.emptyList(), Collections.emptyList());

    public static final Codec<CaveSettings> CODEC = codecOf(
        defaultGet(easyList(BurrowSettings.CODEC), Fields.burrows, Collections::emptyList, s -> s.burrows),
        defaultGet(easyList(CavernSettings.CODEC), Fields.caverns, Collections::emptyList, s -> s.caverns),
        defaultGet(easyList(ClusterSettings.CODEC), Fields.clusters, Collections::emptyList, s -> s.clusters),
        defaultGet(easyList(LayerSettings.CODEC), Fields.layers, Collections::emptyList, s -> s.layers),
        defaultGet(easyList(PillarSettings.CODEC), Fields.pillars, Collections::emptyList, s -> s.pillars),
        defaultGet(easyList(RavineSettings.CODEC), Fields.ravines, Collections::emptyList, s -> s.ravines),
        defaultGet(easyList(StalactiteSettings.CODEC), Fields.stalactites, Collections::emptyList, s -> s.stalactites),
        defaultGet(easyList(StructureSettings.CODEC), Fields.structures, Collections::emptyList, s -> s.structures),
        defaultGet(easyList(TunnelSettings.CODEC), Fields.tunnels, Collections::emptyList, s -> s.tunnels),
        CaveSettings::new
    );

    @Override
    public Codec<CaveSettings> codec() {
        return CODEC;
    }

    @Override
    public CaveSettings withOverrides(final OverrideSettings o) {
        return new CaveSettings(
            map(this.burrows, b -> b.withOverrides(o)),
            map(this.caverns, c -> c.withOverrides(o)),
            map(this.clusters, c -> c.withOverrides(o)),
            map(this.layers, l -> l.withOverrides(o)),
            map(this.pillars, p -> p.withOverrides(o)),
            map(this.ravines, r -> r.withOverrides(o)),
            map(this.stalactites, s -> s.withOverrides(o)),
            map(this.structures, s -> s.withOverrides(o)),
            map(this.tunnels, s -> s.withOverrides(o))
        );
    }

    @Override
    public GeneratorController compile(final Random rand, final long seed) {
        return GeneratorController.builder()
            .sortBurrows(map(this.burrows, b -> b.compile(rand, seed)), rand, seed)
            .sortCaverns(map(this.caverns, c -> c.compile(rand, seed)), rand, seed)
            .sortClusters(map(this.clusters, c -> c.compile(rand, seed)), rand, seed)
            .layers(map(this.layers, l -> new LayerGenerator(l.compile(rand, seed), rand, seed)))
            .pillars(map(this.pillars, p -> new PillarGenerator(p.compile(rand, seed), rand, seed)))
            .ravines(map(this.ravines, r -> new RavineGenerator(r.compile(rand, seed), rand, seed)))
            .stalactites(map(this.stalactites, s -> new StalactiteGenerator(s.compile(rand, seed), rand, seed)))
            .structures(map(this.structures, s -> new StructureGenerator(s.compile(rand, seed), rand, seed)))
            .tunnels(map(this.tunnels, t -> new TunnelGenerator(t.compile(rand, seed), rand, seed)))
            .build();
    }
}
