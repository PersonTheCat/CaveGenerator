package personthecat.cavegenerator.world;

import lombok.Builder;
import personthecat.cavegenerator.presets.CavePreset;
import personthecat.cavegenerator.world.feature.WorldContext;
import personthecat.cavegenerator.world.generator.PrimerContext;

import java.util.Random;

@Builder
public class GeneratorController {

    public static GeneratorController from(final CavePreset preset, final Random rand, final long seed) {
        return GeneratorController.builder().build();
    }

    public void earlyGenerate(final PrimerContext ctx) {

    }

    public void mapGenerate(final PrimerContext ctx) {

    }

    public void featureGenerate(final WorldContext ctx) {

    }
}
