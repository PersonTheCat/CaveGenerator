package personthecat.cavegenerator.world;

import lombok.Builder;
import personthecat.cavegenerator.presets.CavePreset;
import personthecat.cavegenerator.world.feature.WorldContext;
import personthecat.cavegenerator.world.generator.CavernGenerator;
import personthecat.cavegenerator.world.generator.PrimerContext;
import personthecat.cavegenerator.world.generator.TunnelGenerator;

import java.util.List;
import java.util.Random;

import static personthecat.catlib.util.Shorthand.map;

@Builder
public class GeneratorController {

    private final List<CavernGenerator> caverns;
    private final List<TunnelGenerator> tunnels;

    public static GeneratorController from(final CavePreset preset, final Random rand, final long seed) {
        return GeneratorController.builder()
            .caverns(map(preset.caverns, c -> new CavernGenerator(c, rand, seed)))
            .tunnels(map(preset.tunnels, t -> new TunnelGenerator(t, rand, seed)))
            .build();
    }

    public void earlyGenerate(final PrimerContext ctx) {
        caverns.forEach(c -> c.generate(ctx));
    }

    public void mapGenerate(final PrimerContext ctx) {
        tunnels.forEach(t -> t.generate(ctx));
    }

    public void featureGenerate(final WorldContext ctx) {

    }
}
