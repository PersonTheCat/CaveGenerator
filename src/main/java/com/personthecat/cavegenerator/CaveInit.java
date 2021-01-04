package com.personthecat.cavegenerator;

import com.personthecat.cavegenerator.config.PresetReader;
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
import static com.personthecat.cavegenerator.io.SafeFileIO.*;

public class CaveInit {

    /** A setting indicating the location where presets will be kept. */
    private static final String PRESETS = "cavegenerator/presets";

    /** A setting indicating the location where variable imports will be kept. */
    private static final String IMPORTS = "cavegenerator/imports";

    /** A message to display when the preset directory is somehow unavailable. */
    private static final String NO_ACCESS = "Currently unable to access preset directory.";

    /** A message to display when failing to run mkdirs. */
    private static final String CANT_CREATE = "Unable to create directory";

    /** A list of valid extensions to compare against presets. */
    private static final List<String> EXTENSIONS = Arrays.asList("hjson", "json", "cave");

    private static final File CONFIG_DIR = Loader.instance().getConfigDir();
    public static final File PRESET_DIR = new File(CONFIG_DIR, PRESETS);
    private static final File IMPORT_DIR = new File(CONFIG_DIR, IMPORTS);

    /** Initializes the supplied map with presets from the directory. */
    public static void initPresets(final Map<String, GeneratorSettings> presets) {
        // Verify the folders' integrity before proceeding.
        ensureDirExists(PRESET_DIR).expect(CANT_CREATE);
        ensureDirExists(IMPORT_DIR).expect(CANT_CREATE);
        // Go ahead and clear this to allow presets to be reloaded.
        presets.clear();
        // Handle all files in the preset directory.
        presets.putAll(PresetReader.getPresets(PRESET_DIR, IMPORT_DIR));
        // Inform the user of which presets were loaded.
        printLoadedPresets(presets);
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
        File presetFile = new File(PRESET_DIR, preset + "." + extension);
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
        return find(generators.values(), gen -> gen.canGenerate(dimension) && gen.settings.caverns.enabled)
            .isPresent();
    }

    /** Returns whether the input settings are valid for the current dimension. */
    static boolean validPreset(final GeneratorSettings cfg, int dimension) {
        return cfg.conditions.enabled && (cfg.conditions.dimensions.length == 0 ||
            (ArrayUtils.contains(cfg.conditions.dimensions, dimension) != cfg.conditions.dimensionBlacklist));
    }

    /** Returns whether any generator is enabled for the current dimension and has world decorators. */
    public static boolean anyHasWorldDecorator(final Map<String, CaveGenerator> generators, int dimension) {
        return find(generators.values(), gen -> gen.canGenerate(dimension) && gen.hasWorldDecorators())
            .isPresent();
    }
}