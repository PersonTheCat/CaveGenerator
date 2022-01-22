package personthecat.cavegenerator.io;

import lombok.extern.log4j.Log4j2;
import org.hjson.JsonObject;
import org.hjson.JsonValue;
import personthecat.cavegenerator.presets.lang.CaveLangExtension;
import personthecat.cavegenerator.util.Reference;

import java.io.*;

import static personthecat.catlib.io.FileIO.copyStream;
import static personthecat.catlib.io.FileIO.fileExists;
import static personthecat.catlib.io.FileIO.getRequiredResource;
import static personthecat.catlib.io.FileIO.mkdirsOrThrow;
import static personthecat.cavegenerator.exception.CaveSyntaxException.caveSyntax;
import static personthecat.cavegenerator.io.ModFolders.CG_DIR;
import static personthecat.cavegenerator.io.ModFolders.EXAMPLE_DIR;
import static personthecat.cavegenerator.io.ModFolders.IMPORT_DIR;
import static personthecat.cavegenerator.io.ModFolders.PRESET_DIR;
import static personthecat.cavegenerator.io.ModFolders.STRUCTURE_DIR;

@Log4j2
public class JarFiles {

    private static final String DATA_PATH = "data/" + Reference.MOD_ID;
    private static final String CAT_FOLDER_NAME = "cat";
    private static final String TUTORIAL_NAME = "TUTORIAL.cave";
    private static final String REFERENCE_NAME = "REFERENCE.cave";
    private static final File CAT_DIR = new File(ModFolders.IMPORT_DIR, CAT_FOLDER_NAME);

    private static final String[] EXAMPLES = {
        "flooded_vanilla", "large_caves", "spirals",
        "tunnels", "caverns", "stone_layers", "stalactites",
        "ravines", "stone_clusters", "large_stalactites",
        "vanilla", "underground_forest", "euclids_tunnels",
        "lower_caves", "cluster_caverns"
    };

    private static final String[] CAT_IMPORTS = {
        "common", "crack", "desert", "generic",
        "jungle", "lava_aquifers", "maze",
        "mountain", "mushroom", "snow", "water"
    };

    private static final String[] DEFAULTS = {
        "cat", "cat_lite", "geodes", "ore_veins"
    };

    private static final String[] STRUCTURES = {
        "brown_mushroom_lg", "brown_mushroom_sm", "bush1_sm",
        "bush2_sm", "bush3_sm", "bush4_med", "hanging_spawner",
        "hanging_spawner_skeleton", "hanging_spawner_zombie",
        "red_mushroom"
    };

    private static final String[] IMPORTS = {
        "conditions", "defaults", "types"
    };

    public static void copyFiles() {
        mkdirsOrThrow(EXAMPLE_DIR, IMPORT_DIR);

        copyExamples();
        copyDefaults();
        copyImports();
        copyCatImports();
        copyStructures();
        copyTutorial();
    }

    public static boolean isSpecialFile(final String name) {
        return TUTORIAL_NAME.equalsIgnoreCase(name) || REFERENCE_NAME.equalsIgnoreCase(name);
    }

    public static JsonObject getDefaults() {
        final String fromLocation = DATA_PATH + "/imports/" + CaveLangExtension.DEFAULTS;
        final InputStream is = getRequiredResource(fromLocation);
        final Reader rx = new InputStreamReader(is);
        final JsonObject json;
        try {
            json = JsonValue.readHjson(rx).asObject();
        } catch (final IOException e) {
            throw caveSyntax("Reading internal defaults.cave");
        } finally {
            try {
                is.close();
                rx.close();
            } catch (IOException e) {
                log.warn("Unable to dispose resources.");
            }
        }
        return json;
    }

    private static void copyExamples() {
        for (final String filename : EXAMPLES) {
            final String fromLocation = DATA_PATH + "/presets/" + filename + ".cave";
            final String toLocation = EXAMPLE_DIR + "/" + filename + ".cave";
            if (!fileExists(new File(toLocation))) {
                copyFile(fromLocation, toLocation);
            }
        }
    }

    private static void copyDefaults() {
        if (!fileExists(PRESET_DIR)) {
            // The directory doesn't exist. Create it.
            mkdirsOrThrow(PRESET_DIR);
            for (final String s : DEFAULTS) {
                copyDefault(s + ".cave");
            }
        }
    }

    private static void copyDefault(String name) {
        final String fromLocation = DATA_PATH + "/presets/" + name;
        final String toLocation = PRESET_DIR + "/" + name;

        copyFile(fromLocation, toLocation);
    }

    private static void copyImports() {
        mkdirsOrThrow(IMPORT_DIR);
        for (final String i : IMPORTS) {
            final String fromLocation = DATA_PATH + "/imports/" + i + ".cave";
            final String toLocation = IMPORT_DIR + "/" + i + ".cave";
            // Only copy each file if it doesn't already exist.
            if (!fileExists(new File(toLocation))) {
                copyFile(fromLocation, toLocation);
            }
        }
    }

    private static void copyCatImports() {
        mkdirsOrThrow(new File(IMPORT_DIR, CAT_FOLDER_NAME));
        for (final String i : CAT_IMPORTS) {
            final String fromLocation = DATA_PATH + "/imports/cat/" + i + ".cave";
            final String toLocation = CAT_DIR + "/" + i + ".cave";
            if (!fileExists(new File(toLocation))) {
                copyFile(fromLocation, toLocation);
            }
        }
    }

    private static void copyStructures() {
        if (!fileExists(STRUCTURE_DIR)) {
            // The directory doesn't exist. Create it.
            mkdirsOrThrow(STRUCTURE_DIR);
            for (String structure : STRUCTURES) {
                final String fromLocation = DATA_PATH + "/structures/" + structure + ".nbt";
                final String toLocation = STRUCTURE_DIR + "/" + structure + ".nbt";
                copyFile(fromLocation, toLocation);
            }
        }
    }

    private static void copyTutorial() {
        // Copy the regular tutorial file.
        final String fromLocation = DATA_PATH + "/" + TUTORIAL_NAME;
        final String toLocation = CG_DIR + "/" + TUTORIAL_NAME;
        copyFile(fromLocation, toLocation);

        // Copy the condensed version. Todo: Generate this file.
        final String fromLocation2 = DATA_PATH + "/" + REFERENCE_NAME;
        final String toLocation2 = CG_DIR + "/" + REFERENCE_NAME;
        copyFile(fromLocation2, toLocation2);
    }

    private static void copyFile(String fromLocation, String toLocation) {
        try {
            final InputStream toCopy = getRequiredResource(fromLocation);
            final FileOutputStream output = new FileOutputStream(toLocation);
            copyStream(toCopy, output) // This error should be handled now.
                .expect("Error copying file data. Perhaps the jar is corrupt.");
            output.close();
        } catch (final FileNotFoundException e) {
            throw new RuntimeException("Unable to open FileOutputStream", e);
        } catch (final IOException e) {
            throw new RuntimeException("Error closing FileOutputStream", e);
        }
    }
}