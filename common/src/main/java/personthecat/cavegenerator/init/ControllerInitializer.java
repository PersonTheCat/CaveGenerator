package personthecat.cavegenerator.init;

import personthecat.cavegenerator.CaveRegistries;
import personthecat.cavegenerator.model.SeedStorage;
import personthecat.cavegenerator.presets.CavePreset;
import personthecat.cavegenerator.world.GeneratorController;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

public class ControllerInitializer {

    /**
     * Converts every cave preset model into a generator controller using the current seed info.
     *
     * @return A map of preset name -> generator controller.
     */
    public static Map<String, GeneratorController> initControllers() {
        if (CaveRegistries.PRESETS.isEmpty()) {
            return Collections.emptyMap();
        }
        final SeedStorage.Info seedInfo = CaveRegistries.CURRENT_SEED.get();
        final Map<String, GeneratorController> controllers = new TreeMap<>();
        for (final Map.Entry<String, CavePreset> entry : CaveRegistries.PRESETS.entrySet()) {
            controllers.put(entry.getKey(), entry.getValue().setupController(seedInfo.rand, seedInfo.seed));
        }
        return controllers;
    }
}
