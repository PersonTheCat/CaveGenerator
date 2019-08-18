package com.personthecat.cavegenerator;

import com.personthecat.cavegenerator.config.ConfigFile;
import com.personthecat.cavegenerator.config.PresetReader;
import com.personthecat.cavegenerator.config.PresetTester;
import com.personthecat.cavegenerator.util.Result;
import com.personthecat.cavegenerator.world.CaveGenerator;
import com.personthecat.cavegenerator.world.GeneratorSettings;
import net.minecraftforge.fml.common.Loader;
import org.apache.commons.lang3.ArrayUtils;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import static com.personthecat.cavegenerator.util.CommonMethods.*;
import static com.personthecat.cavegenerator.util.SafeFileIO.*;

public class CaveInit {
    /** A setting indicating the location where presets will be kept. */
    private static final String FOLDER = "cavegenerator/presets";
    public static final File DIR = new File(Loader.instance().getConfigDir(), FOLDER);
    public static final List<String> EXTENSIONS = Arrays.asList("hjson", "json", "cave");
    /** A message to display when the preset directory is somehow unavailable. */
    private static final String NO_ACCESS = "Currently unable to access preset directory.";

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
                if (validExtension(file)) {
                    String filename = file.getName();
                    GeneratorSettings preset = PresetReader.getPreset(file);
                    PresetTester tester = new PresetTester(preset, filename, ConfigFile.strictPresets);
                    tester.run();
                    presets.put(filename, preset);
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

    /** Attempts to locate a preset using each of the possible extensions. */
    public static Optional<File> locatePreset(String preset) {
        for (String ext : EXTENSIONS) {
            Optional<File> found = tryExtension(preset, ext);
            if (found.isPresent()) {
                return found;
            }
        }
        return empty();
    }

    /** Attempts to locate a preset using a specific extension. */
    private static Optional<File> tryExtension(String preset, String extension) {
        File presetFile = new File(CaveInit.DIR, preset + "." + extension);
        if (safeFileExists(presetFile, NO_ACCESS)) {
            return full(presetFile);
        }
        return empty();
    }

    public static boolean validExtension(File file) {
        return EXTENSIONS.contains(extension(file));
    }

    /** Prints which presets are currently loaded and whether they are enabled. */
    private static void printLoadedPresets(final Map<String, GeneratorSettings> presets) {
        for (Entry<String, GeneratorSettings> entry : presets.entrySet()) {
            final String enabled = entry.getValue().conditions.enabled ? "enabled" : "disabled";
            info("Successfully loaded {}. It is {}.", entry.getKey(), enabled);
        }
    }

    /** Returns whether any generator is enabled for the current dimension. */
    public static boolean anyGeneratorEnabled(final Map<String, CaveGenerator> generators, int dimension) {
        return find(generators.values(), gen -> gen.canGenerate(dimension))
            .isPresent();
    }

    /** Returns whether any generator in the current dimension has caverns enabled. */
    public static boolean anyCavernsEnabled(final Map<String, CaveGenerator> generators, int dimension) {
        return find(generators.values(), gen -> gen.canGenerate(dimension))
            .map(gen -> gen.settings.caverns.enabled)
            .orElse(false);
    }

    /** Returns whether the input settings are valid for the current dimension. */
    public static boolean validPreset(final GeneratorSettings cfg, int dimension) {
        return cfg.conditions.enabled && (cfg.conditions.dimensions.length == 0 ||
            (ArrayUtils.contains(cfg.conditions.dimensions, dimension) != cfg.conditions.dimensionBlacklist));
    }

    /** Returns whether any generator is enabled for the current dimension and has world decorators. */
    public static boolean anyHasWorldDecorator(final Map<String, CaveGenerator> generators, int dimension) {
        return find(generators.values(), gen -> gen.canGenerate(dimension) && gen.hasWorldDecorators())
            .isPresent();
    }
}