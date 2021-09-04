package personthecat.cavegenerator.world.event;

import lombok.extern.log4j.Log4j2;
import net.minecraft.core.Registry;
import net.minecraft.world.level.levelgen.GenerationStep;
import personthecat.catlib.event.world.FeatureModificationContext;
import personthecat.cavegenerator.CaveRegistries;
import personthecat.cavegenerator.config.Cfg;
import personthecat.cavegenerator.world.feature.FallbackFeatureHook;
import personthecat.cavegenerator.world.generator.FallbackCarverHook;

@Log4j2
public class CaveCleanupEvent {

    public static void onBiomeCleanup(final FeatureModificationContext ctx) {
        log.info("running biome cleanup v5 on {}", ctx.getName());
        CaveRegistries.DISABLED_FEATURES.getAsserted(Registry.CONFIGURED_CARVER_REGISTRY).forEach(id -> {
            if (ctx.removeCarver(id)) log.debug("Removed carver {} from {}.", id, ctx.getName());
        });
        CaveRegistries.DISABLED_FEATURES.getAsserted(Registry.CONFIGURED_FEATURE_REGISTRY).forEach(id -> {
            if (ctx.removeFeature(id)) log.debug("Removed feature {} from {}.", id, ctx.getName());
        });
        CaveRegistries.DISABLED_FEATURES.getAsserted(Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY).forEach(id -> {
            if (ctx.removeStructure(id)) log.debug("Removed structure {} from {}.", id, ctx.getName());
        });
        if (Cfg.FALLBACK_CARVERS.getAsBoolean()) {
            ctx.addCarver(GenerationStep.Carving.AIR, FallbackCarverHook.HOOK);
        }
        if (Cfg.FALLBACK_FEATURES.getAsBoolean()) {
            ctx.addFeature(GenerationStep.Decoration.SURFACE_STRUCTURES, FallbackFeatureHook.HOOK);
        }
    }
}
