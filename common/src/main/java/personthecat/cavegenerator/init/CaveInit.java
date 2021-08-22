package personthecat.cavegenerator.init;

import lombok.extern.log4j.Log4j2;
import personthecat.catlib.io.FileIO;
import personthecat.catlib.util.HjsonUtils;
import personthecat.cavegenerator.CaveRegistries;
import personthecat.cavegenerator.model.SeedStorage;
import personthecat.cavegenerator.presets.CavePreset;
import personthecat.cavegenerator.config.Cfg;
import personthecat.cavegenerator.world.GeneratorController;

import java.io.File;
import java.util.*;

import static java.util.Optional.empty;
import static personthecat.catlib.util.PathUtils.extension;
import static personthecat.catlib.util.Shorthand.full;
import static personthecat.catlib.io.FileIO.fileExists;
import static personthecat.catlib.io.FileIO.mkdirsOrThrow;
import static personthecat.cavegenerator.io.ModFolders.GENERATED_DIR;
import static personthecat.cavegenerator.io.ModFolders.IMPORT_DIR;
import static personthecat.cavegenerator.io.ModFolders.PRESET_DIR;

@Log4j2
public class CaveInit {

    /** A list of valid extensions to compare against presets. */
    private static final List<String> EXTENSIONS = Arrays.asList("hjson", "json", "cave");

    /**
     * Loads all of the enabled presets located inside of <code>cavegenerator/presets</code>.
     *
     * @return A map of preset name -> preset model.
     */
    public static Map<String, CavePreset> initPresets() {
        // Verify the folders' integrity before proceeding.
        mkdirsOrThrow(PRESET_DIR, IMPORT_DIR);
        // Handle all files in the preset directory.
        final Map<String, CavePreset> presets = PresetReader.loadPresets(PRESET_DIR, IMPORT_DIR);

        // If necessary, automatically write the expanded values.
        if (Cfg.AUTO_GENERATE.getAsBoolean()) {
            mkdirsOrThrow(GENERATED_DIR);
            presets.forEach((name, preset) -> {
                final File file = new File(GENERATED_DIR, name + ".cave");
                HjsonUtils.writeJson(preset.raw, file).unwrap();
            });
        }
        // Inform the user of which presets were loaded.
        printLoadedPresets(presets);
        return presets;
    }

    /**
     * Prints which presets are currently loaded and whether they are enabled.
     *
     * @param presets A map of preset name -> preset model.
     */
    private static void printLoadedPresets(final Map<String, CavePreset> presets) {
        for (final Map.Entry<String, CavePreset> entry : presets.entrySet()) {
            final String enabled = entry.getValue().enabled ? "enabled" : "disabled";
            log.info("{} is {}.", entry.getKey(), enabled);
        }
    }

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
            controllers.put(entry.getKey(), GeneratorController.from(entry.getValue(), seedInfo.rand, seedInfo.seed));
        }
        return controllers;
    }

    /**
     * Attempts to locate a preset using each of the possible extensions. This will search
     * recursively inside of <code>cavegenerator/presets</code>.
     *
     * @param preset The name of the preset, with or without an extension.
     * @return The requested preset, or else {@link Optional#empty}.
     */
    public static Optional<File> locatePreset(final String preset) {
        return locatePreset(PRESET_DIR, preset);
    }

    /**
     * Attempts to locate a preset using each of the possible extensions when provided a
     * specific root directory to search inside of.
     *
     * @param directory The root directory inside of which the preset is expected to exist.
     * @param preset The name of the preset, with or without an extension.
     * @return The requested preset, or else {@link Optional#empty}.
     */
    public static Optional<File> locatePreset(final File directory, final String preset) {
        final File presetFile = new File(directory,  preset);
        if (fileExists(presetFile)) {
            return full(presetFile);
        }
        for (final String ext : EXTENSIONS) {
            final Optional<File> found = tryExtension(directory, preset, ext);
            if (found.isPresent()) {
                return found;
            }
        }
        for (final File dir : FileIO.listFiles(directory, File::isDirectory)) {
            final Optional<File> found = locatePreset(dir, preset);
            if (found.isPresent()) {
                return found;
            }
        }
        return empty();
    }

    /**
     * Checks to see whether a file exists using the provided extension. If so, returns it.
     *
     * @param directory The directory which may or may not contain the file.
     * @param preset The name of the file, <b>without</b> any extension.
     * @param extension The extension being appended to the filename.
     * @return The expected file, or else {@link Optional#empty}
     */
    private static Optional<File> tryExtension(final File directory, final String preset, final String extension) {
        final File presetFile = new File(directory, preset + "." + extension);
        if (fileExists(presetFile)) {
            return full(presetFile);
        }
        return empty();
    }

    /**
     * Determines whether the provided file is in one of the accepted formats.
     *
     * @param file A file which may or may not be a preset.
     * @return <code>true</code>, if the file can be accepted.
     */
    public static boolean validExtension(final File file) {
        return EXTENSIONS.contains(extension(file));
    }
}