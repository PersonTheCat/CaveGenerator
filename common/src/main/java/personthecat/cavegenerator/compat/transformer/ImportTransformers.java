package personthecat.cavegenerator.compat.transformer;

import personthecat.catlib.util.JsonTransformer;
import personthecat.catlib.util.JsonTransformer.ObjectResolver;
import personthecat.cavegenerator.io.JarFiles;

public class ImportTransformers {

    // Deprecated fields in defaults.cave
    private static final String REPLACE_DIRT_STONE = "REPLACE_DIRT_STONE";
    private static final String VANILLA_ROOM = "VANILLA_ROOM";
    private static final String LAVA_CAVE_BLOCK = "LAVA_CAVE_BLOCK";
    private static final String VANILLA_TUNNELS = "VANILLA_TUNNELS";
    private static final String VANILLA_RAVINES = "VANILLA_RAVINES";

    public static final ObjectResolver DEFAULTS_TRANSFORMER =
        JsonTransformer.root()
            .remove(REPLACE_DIRT_STONE)
            .remove(VANILLA_ROOM)
            .remove(LAVA_CAVE_BLOCK)
            .remove(VANILLA_TUNNELS)
            .remove(VANILLA_RAVINES)
            .setDefaults(JarFiles.getDefaults());
}
