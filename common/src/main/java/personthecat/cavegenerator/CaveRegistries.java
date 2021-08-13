package personthecat.cavegenerator;

import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import personthecat.catlib.data.SafeRegistry;
import personthecat.cavegenerator.world.feature.StructureSpawner;

public class CaveRegistries {
    public static SafeRegistry<String, StructureTemplate> STRUCTURES =
        SafeRegistry.of(StructureSpawner.loadStructureFiles())
            .respondsWith(key -> "Structure file or resource not found: " + key)
            .canBeReset(true);
}
