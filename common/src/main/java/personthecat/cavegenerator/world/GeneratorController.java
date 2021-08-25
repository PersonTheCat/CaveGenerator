package personthecat.cavegenerator.world;

import lombok.Builder;
import personthecat.cavegenerator.presets.CavePreset;
import personthecat.cavegenerator.world.feature.WorldContext;
import personthecat.cavegenerator.world.generator.*;

import java.util.List;
import java.util.Random;

import static personthecat.catlib.util.Shorthand.map;

@Builder
public class GeneratorController {

    private final List<CavernGenerator> caverns;
    private final List<TunnelGenerator> tunnels;
    private final List<RavineGenerator> ravines;
    private final List<BurrowGenerator> burrows;
    private final List<LayerGenerator> layers;

    public static GeneratorController from(final CavePreset preset, final Random rand, final long seed) {
        return GeneratorController.builder()
            .caverns(map(preset.caverns, c -> new CavernGenerator(c, rand, seed)))
            .tunnels(map(preset.tunnels, t -> new TunnelGenerator(t, rand, seed)))
            .ravines(map(preset.ravines, r -> new RavineGenerator(r, rand, seed)))
            .burrows(map(preset.burrows, b -> new BurrowGenerator(b, rand, seed)))
            .layers(map(preset.layers, l -> new LayerGenerator(l, rand, seed)))
            .build();
    }

    public void earlyGenerate(final PrimerContext ctx) {
        layers.forEach(l -> l.generate(ctx));
        caverns.forEach(c -> c.generate(ctx));
        burrows.forEach(b -> b.generate(ctx));
    }

    public void mapGenerate(final PrimerContext ctx) {
        tunnels.forEach(t -> t.generate(ctx));
        ravines.forEach(r -> r.generate(ctx));
    }

    public void featureGenerate(final WorldContext ctx) {

    }
}
