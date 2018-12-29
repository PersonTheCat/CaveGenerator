package com.personthecat.cavegenerator;

import com.personthecat.cavegenerator.config.PresetReader;
import com.personthecat.cavegenerator.util.Result;
import com.personthecat.cavegenerator.world.CaveGenerator;
import com.personthecat.cavegenerator.world.GeneratorSettings;
import net.minecraft.world.World;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import static com.personthecat.cavegenerator.util.CommonMethods.*;
import static com.personthecat.cavegenerator.util.SafeFileIO.*;

public class CaveInit {
    /** A setting indicating the location where presets will be kept. */
    private static final String FOLDER = "cavegenerator/presets";
    public static final File DIR = new File(Loader.instance().getConfigDir(), FOLDER);
    private static final List<String> EXTENSIONS = Arrays.asList("hjson", "json", "cave");

    /** Initializes the supplied map with presets from the directory. */
    public static Result<RuntimeException> initPresets(final Map<String, GeneratorSettings> presets) {
        // Verify the folder's integrity before proceeding.
        ensureDirExists(DIR)
            .expect("Error: Unable to create the preset directory. It is most likely in use.");
        // Go ahead and clear this to allow presets to be reloaded.
        presets.clear();
        // Initialize a result to be returned.
        Result<RuntimeException> result = Result.ok();
        // Handle all files in the preset directory.
        safeListFiles(DIR).ifPresent((files) -> { // Files found.
            for (File file : files) {
                if (EXTENSIONS.contains(extension(file))) {
                    presets.put(file.getName(), PresetReader.getPreset(file));
                }
            }
        });
        // Inform the user of which presets were loaded.
        printLoadedPresets(presets);
        return result; // To do: return an error if safeListFiles() returns empty.
    }

    /** Loads all presets, crashing if an error is present. */
    public static void forceInitPresets(final Map<String, GeneratorSettings> presets) {
        initPresets(presets).handleIfPresent((err) -> {
            throw runExF("Error loading presets: %s", err.getMessage());
        });
    }

    /** Prints which presets are currently loaded and whether they are enabled. */
    private static void printLoadedPresets(final Map<String, GeneratorSettings> presets) {
        for (Entry<String, GeneratorSettings> entry : presets.entrySet()) {
            final String enabled = entry.getValue().conditions.enabled ? "enabled" : "disabled";
            info("Successfully loaded {}. It is {}.", entry.getKey(), enabled);
        }
    }

    @SubscribeEvent
    @SuppressWarnings("unused")
    public static void onWorldEventLoad(WorldEvent.Load event) {
        // Obtain a reference to the current world from `event`.
        World world = event.getWorld();

        // Verify the integrity of `world` before proceeding.
        if (world == null) {
            throw runEx(
                "Received a null World object on WorldEvent.Load. " +
                "This was most likely caused by another mod running " +
                "with a dedicated WorldProvider service. Please let " +
                "PersonTheCat know, but it may not be fixable."
            );
        }

        // Clear existing generators. Allow them to be reset.
        Main.instance.generators.clear();
        // As always, there's no other way to access additional
        // information non-statically when using Forge's
        // SubscribeEvents. I would have preferred to avoid that.
        for (Entry<String, GeneratorSettings> entry : Main.instance.presets.entrySet()) {
            CaveGenerator generator = new CaveGenerator(world, entry.getValue());
            Main.instance.generators.put(entry.getKey(), generator);
        }
    }

    /** Returns whether any generator is enabled for the current dimension. */
    public static boolean anyGeneratorEnabled(final Map<String, CaveGenerator> generators, int dimension) {
        return find(generators.values(), (gen) -> gen.canGenerate(dimension))
            .isPresent();
    }

    /** Returns whether any generator has wall decorations, requiring special handling. */
    public static boolean correctionsNeeded(final Map<String, CaveGenerator> generators) {
        return find(generators.values(), CaveGenerator::hasLocalWallDecorators)
            .isPresent();
    }
}