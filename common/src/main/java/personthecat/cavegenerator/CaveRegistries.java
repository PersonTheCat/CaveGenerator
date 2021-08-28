package personthecat.cavegenerator;

import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import personthecat.catlib.data.SafeRegistry;
import personthecat.cavegenerator.model.SeedStorage;
import personthecat.cavegenerator.presets.CavePreset;
import personthecat.cavegenerator.init.CaveInit;
import personthecat.cavegenerator.world.GeneratorController;
import personthecat.cavegenerator.world.feature.StructureSpawner;

// Todo: map generators to dimension keys?
public class CaveRegistries {

    public static final SafeRegistry<String, CavePreset> PRESETS =
        SafeRegistry.of(CaveInit::initPresets)
            .respondsWith(key -> "There is no preset named: " + key)
            .canBeReset(true);

    public static final SafeRegistry<String, GeneratorController> GENERATORS =
        SafeRegistry.of(CaveInit::initControllers)
            .respondsWith(key -> "There is no generator named: " + key)
            .canBeReset(true);

    public static final SafeRegistry<String, StructureTemplate> STRUCTURES =
        SafeRegistry.of(StructureSpawner::initStructures)
            .respondsWith(key -> "Structure file or resource not found: " + key)
            .canBeReset(true);

    public static final SeedStorage CURRENT_SEED = new SeedStorage();

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
