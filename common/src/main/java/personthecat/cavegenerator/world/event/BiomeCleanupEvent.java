package personthecat.cavegenerator.world.event;

import lombok.extern.log4j.Log4j2;
import net.minecraft.core.Registry;
import personthecat.cavegenerator.CaveRegistries;

@Log4j2
public class BiomeCleanupEvent {

    public static void onBiomeCleanup(final CaveModificationContext ctx) {
        CaveRegistries.DISABLED_FEATURES.getAsserted(Registry.CONFIGURED_CARVER_REGISTRY).forEach(id -> {
            if (ctx.removeCarver(id)) log.debug("Removed carver {} from {}.", id, ctx.getBiomeName());
        });
        CaveRegistries.DISABLED_FEATURES.getAsserted(Registry.CONFIGURED_FEATURE_REGISTRY).forEach(id -> {
            if (ctx.removeFeature(id)) log.debug("Removed feature {} from {}.", id, ctx.getBiomeName());
        });
        CaveRegistries.DISABLED_FEATURES.getAsserted(Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY).forEach(id -> {
            if (ctx.removeStructure(id)) log.debug("Removed structure {} from {}.", id, ctx.getBiomeName());
        });
    }
}
