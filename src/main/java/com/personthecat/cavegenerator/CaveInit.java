package com.personthecat.cavegenerator;

import com.personthecat.cavegenerator.config.CavePreset;
import com.personthecat.cavegenerator.config.PresetReader;
import lombok.extern.log4j.Log4j2;
import net.minecraftforge.fml.common.Loader;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import static com.personthecat.cavegenerator.util.CommonMethods.*;
import static com.personthecat.cavegenerator.io.SafeFileIO.*;

@Log4j2
public class CaveInit {

    /** A setting indicating the location where presets will be kept. */
    private static final String PRESETS = Main.MODID + "/presets";

    /** A setting indicating the location where variable imports will be kept. */
    private static final String IMPORTS = Main.MODID + "/imports";

    /** A setting indicating the location where generated presets will be kept. */
    private static final String GENERATED = Main.MODID + "/generated";

    /** A message to display when the preset directory is somehow unavailable. */
    private static final String NO_ACCESS = "Currently unable to access preset directory.";

    /** A message to display when failing to run mkdirs. */
    private static final String CANT_CREATE = "Unable to create directory";

    /** A list of valid extensions to compare against presets. */
    private static final List<String> EXTENSIONS = Arrays.asList("hjson", "json", "cave");

    private static final File CONFIG_DIR = Loader.instance().getConfigDir();

    public static final File PRESET_DIR = new File(CONFIG_DIR, PRESETS);
    public static final File IMPORT_DIR = new File(CONFIG_DIR, IMPORTS);
    public static final File GENERATED_DIR = new File(CONFIG_DIR, GENERATED);

    /** Initializes the supplied map with presets from the directory. */
    public static void initPresets(final Map<String, CavePreset> presets) {
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
        final File presetFile = new File(PRESET_DIR,  preset);
        if (safeFileExists(presetFile, "Error checking file: " + presetFile)) {
            return full(presetFile);
        }
        for (String ext : EXTENSIONS) {
            final Optional<File> found = tryExtension(preset, ext);
            if (found.isPresent()) {
                return found;
            }
        }
        return empty();
    }

    /** Attempts to locate a preset using a specific extension. */
    private static Optional<File> tryExtension(String preset, String extension) {
        final File presetFile = new File(PRESET_DIR, preset + "." + extension);
        if (safeFileExists(presetFile, NO_ACCESS)) {
            return full(presetFile);
        }
        return empty();
    }

    public static boolean validExtension(File file) {
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