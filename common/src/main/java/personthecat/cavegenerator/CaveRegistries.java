package personthecat.cavegenerator;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import personthecat.catlib.data.SafeRegistry;
import personthecat.cavegenerator.init.ControllerInitializer;
import personthecat.cavegenerator.init.DisabledFeatureSupport;
import personthecat.cavegenerator.init.PresetLoadingContext;
import personthecat.cavegenerator.model.CaveCommandSource;
import personthecat.cavegenerator.model.SeedStorage;
import personthecat.cavegenerator.presets.CavePreset;
import personthecat.cavegenerator.world.GeneratorController;
import personthecat.cavegenerator.world.feature.StructureSpawner;

import java.util.List;

public class CaveRegistries {

    public static final SafeRegistry<String, CavePreset> PRESETS =
        SafeRegistry.of(PresetLoadingContext::loadPresets)
            .respondsWith(key -> "There is no preset named: " + key)
            .canBeReset(true);

    public static final SafeRegistry<String, GeneratorController> GENERATORS =
        SafeRegistry.of(ControllerInitializer::initControllers)
            .respondsWith(key -> "There is no generator named: " + key)
            .canBeReset(true);

    public static final SafeRegistry<String, StructureTemplate> STRUCTURES =
        SafeRegistry.of(StructureSpawner::initStructures)
            .respondsWith(key -> "Structure file or resource not found: " + key)
            .canBeReset(true);

    public static final SafeRegistry<ResourceKey<?>, List<ResourceLocation>> DISABLED_FEATURES =
        SafeRegistry.of(DisabledFeatureSupport::setupDisabledFeatures)
            .respondsWith(key -> "Unsupported registry key: " + key)
            .canBeReset(true);

    public static final SeedStorage CURRENT_SEED = new SeedStorage();

    public static final CaveCommandSource COMMAND_SOURCE = new CaveCommandSource();

    public static void loadAll() {
        SafeRegistry.loadAll(PRESETS, GENERATORS, STRUCTURES);
    }

    public static void reloadAll() {
        SafeRegistry.reloadAll(PRESETS, GENERATORS, STRUCTURES);
    }

    public static void resetAll() {
        SafeRegistry.resetAll(PRESETS, GENERATORS, STRUCTURES);
    }
}
