package personthecat.cavegenerator.presets.init;

import lombok.extern.log4j.Log4j2;
import personthecat.catlib.util.HjsonUtils;
import personthecat.cavegenerator.presets.CavePreset;
import personthecat.cavegenerator.config.Cfg;
import personthecat.cavegenerator.presets.init.PresetReader;
import personthecat.cavegenerator.exception.ModSetupException;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import static java.util.Optional.empty;
import static personthecat.catlib.util.PathUtils.extension;
import static personthecat.catlib.util.Shorthand.full;
import static personthecat.catlib.io.FileIO.fileExists;
import static personthecat.catlib.io.FileIO.mkdirs;
import static personthecat.cavegenerator.io.ModFolders.GENERATED_DIR;
import static personthecat.cavegenerator.io.ModFolders.IMPORT_DIR;
import static personthecat.cavegenerator.io.ModFolders.PRESET_DIR;

@Log4j2
public class CaveInit {

    /** A message to display when failing to run mkdirs. */
    private static final String CANT_CREATE = "Unable to create directory";

    /** A list of valid extensions to compare against presets. */
    private static final List<String> EXTENSIONS = Arrays.asList("hjson", "json", "cave");

    /** Initializes the supplied map with presets from the directory. */
    public static void initPresets(final Map<String, CavePreset> presets) {
        // Verify the folders' integrity before proceeding.
        if (!(mkdirs(PRESET_DIR) && mkdirs(IMPORT_DIR))) {
            throw new ModSetupException(CANT_CREATE);
        }
        // Go ahead and final File dir = new File(CaveInit.CG_DIR, args[1]);lear this to allow presets to be reloaded.
        presets.clear();
        // Handle all files in the preset directory.
        presets.putAll(PresetReader.getPresets(PRESET_DIR, IMPORT_DIR));

        // If necessary, automatically write the expanded values.
        if (Cfg.AUTO_GENERATE.getAsBoolean()) {
            if (!mkdirs(GENERATED_DIR)) {
                throw new ModSetupException(CANT_CREATE);
            }
            presets.forEach((name, preset) -> {
                final File file = new File(GENERATED_DIR, name + ".cave");
                HjsonUtils.writeJson(preset.raw, file).unwrap();
            });
        }
        // Inform the user of which presets were loaded.
        printLoadedPresets(presets);
    }

    /** Attempts to locate a preset using each of the possible extensions. */
    public static Optional<File> locatePreset(final String preset) {
        return locatePreset(PRESET_DIR, preset);
    }

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
        return empty();
    }

    /** Attempts to locate a preset using a specific extension. */
    private static Optional<File> tryExtension(final File directory, final String preset, final String extension) {
        final File presetFile = new File(directory, preset + "." + extension);
        if (fileExists(presetFile)) {
            return full(presetFile);
        }
        return empty();
    }

    public static boolean validExtension(final File file) {
        return EXTENSIONS.contains(extension(file));
    }

    /** Prints which presets are currently loaded and whether they are enabled. */
    private static void printLoadedPresets(final Map<String, CavePreset> presets) {
        for (Entry<String, CavePreset> entry : presets.entrySet()) {
            final String enabled = entry.getValue().enabled ? "enabled" : "disabled";
            log.info("{} is {}.", entry.getKey(), enabled);
        }
    }
}