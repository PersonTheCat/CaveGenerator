package personthecat.cavegenerator;

import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import personthecat.catlib.data.SafeRegistry;
import personthecat.cavegenerator.presets.CavePreset;
import personthecat.cavegenerator.world.GeneratorController;
import personthecat.cavegenerator.world.feature.StructureSpawner;

public class CaveRegistries {

    // Todo: map these to dimension keys?
    public static SafeRegistry<String, GeneratorController> GENERATORS =
        SafeRegistry.of(new java.util.HashMap<String, GeneratorController>())
            .respondsWith(key -> "There is no generator named: " + key)
            .canBeReset(true);

    public static SafeRegistry<String, CavePreset> PRESETS =
        SafeRegistry.of(new java.util.HashMap<String, CavePreset>())
            .respondsWith(key -> "There is no preset named: " + key)
            .canBeReset(true);

    public static SafeRegistry<String, StructureTemplate> STRUCTURES =
        SafeRegistry.of(StructureSpawner::loadStructureFiles)
            .respondsWith(key -> "Structure file or resource not found: " + key)
            .canBeReset(true);
}
