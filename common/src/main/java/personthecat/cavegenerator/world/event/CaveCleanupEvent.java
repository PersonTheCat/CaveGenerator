package personthecat.cavegenerator.world.event;

import lombok.extern.log4j.Log4j2;
import net.minecraft.core.Registry;
import net.minecraft.world.level.levelgen.GenerationStep;
import personthecat.catlib.event.world.FeatureModificationContext;
import personthecat.cavegenerator.CaveRegistries;
import personthecat.cavegenerator.config.Cfg;
import personthecat.cavegenerator.world.hook.FallbackFeatureHook;
import personthecat.cavegenerator.world.hook.FallbackCarverHook;

@Log4j2
public class CaveCleanupEvent {

    public static void onBiomeCleanup(final FeatureModificationContext ctx) {
        CaveRegistries.DISABLED_FEATURES.getAsserted(Registry.CONFIGURED_CARVER_REGISTRY).forEach(id -> {
            if (ctx.removeCarver(id)) log.debug("Removed carver {} from {}.", id, ctx.getName());
        });
        CaveRegistries.DISABLED_FEATURES.getAsserted(Registry.CONFIGURED_FEATURE_REGISTRY).forEach(id -> {
            if (ctx.removeFeature(id)) log.debug("Removed feature {} from {}.", id, ctx.getName());
        });
        CaveRegistries.DISABLED_FEATURES.getAsserted(Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY).forEach(id -> {
            if (ctx.removeStructure(id)) log.debug("Removed structure {} from {}.", id, ctx.getName());
        });
        if (Cfg.fallbackCarvers()) {
            ctx.addCarver(GenerationStep.Carving.AIR, FallbackCarverHook.HOOK);
        }
        if (Cfg.fallbackFeatures()) {
            ctx.addFeature(GenerationStep.Decoration.SURFACE_STRUCTURES, FallbackFeatureHook.HOOK);
        }
    }
}
