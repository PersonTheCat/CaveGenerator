package com.personthecat.cavegenerator.io;

import com.personthecat.cavegenerator.CaveInit;
import com.personthecat.cavegenerator.Main;
import com.personthecat.cavegenerator.config.PresetExpander;
import com.personthecat.cavegenerator.world.feature.StructureSpawner;
import lombok.extern.log4j.Log4j2;
import org.hjson.JsonObject;
import org.hjson.JsonValue;

import java.io.*;

import static com.personthecat.cavegenerator.io.SafeFileIO.copyStream;
import static com.personthecat.cavegenerator.io.SafeFileIO.ensureDirExists;
import static com.personthecat.cavegenerator.io.SafeFileIO.fileExists;
import static com.personthecat.cavegenerator.io.SafeFileIO.getRequiredResource;
import static com.personthecat.cavegenerator.io.SafeFileIO.mkdirs;
import static com.personthecat.cavegenerator.util.CommonMethods.runEx;

@Log4j2
public class JarFiles {

    /** The path where all of this mod's assets are stored. */
    private static final String ASSETS_PATH = "assets/" + Main.MODID;

    /** The name of the folder containing cat presets.  */
    private static final String CAT_FOLDER_NAME = "cat";

    /** The name of the tutorial file. */
    private static final String TUTORIAL_NAME = "TUTORIAL.cave";

    /** The name of the stripped tutorial file. */
    private static final String REFERENCE_NAME = "REFERENCE.cave";

    /** The actual folder containing the default cat imports. */
    private static final File CAT_DIR = new File(CaveInit.IMPORT_DIR, CAT_FOLDER_NAME);

    /** All of the <b>example</b> presets to be copied from the jar. */
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

    /** A couple of structure NBTs to be copied from the jar. */
    private static final String[] STRUCTURES = {
        "brown_mushroom_lg", "brown_mushroom_sm", "bush1_sm",
        "bush2_sm", "bush3_sm", "bush4_med", "hanging_spawner",
        "hanging_spawner_skeleton", "hanging_spawner_zombie",
        "red_mushroom"
    };

    /** Any preset that specifically belongs in /imports. */
    private static final String[] IMPORTS = {
        "blocks", "conditions", "defaults", "types"
    };

    /** Copies the example presets from the jar to the disk. */
    public static void copyFiles() {
        // Verify the folders' integrity before proceeding.
        ensureDirExists(CaveInit.EXAMPLE_DIR)
            .expect("Error: Unable to create the example preset directory");
        ensureDirExists(CaveInit.IMPORT_DIR)
            .expect("Error: Unable to create the import directory");

        copyExamples();
        copyDefaults();
        copyImports();
        copyCatImports();
        copyStructures();
        copyTutorial();
    }

    public static JsonObject getDefaults() {
        final String fromLocation = ASSETS_PATH + "/imports/" + PresetExpander.DEFAULTS;
        final InputStream is = getRequiredResource(fromLocation);
        final Reader rx = new InputStreamReader(is);
        final JsonObject json;
        try {
            json = JsonValue.readHjson(rx).asObject();
        } catch (IOException e) {
            throw runEx("Reading internal defaults.cave");
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
        for (String fileName : EXAMPLES) {
            final String fromLocation = ASSETS_PATH + "/presets/" + fileName + ".cave";
            final String toLocation = CaveInit.EXAMPLE_DIR + "/" + fileName + ".cave";
            if (!fileExists(new File(toLocation), "Security error on " + toLocation)) {
                copyFile(fromLocation, toLocation);
            }
        }
    }

    private static void copyDefaults() {
        if (!fileExists(CaveInit.PRESET_DIR, "Error: Unable to read from preset directory")) {
            // The directory doesn't exist. Create it.
            mkdirs(CaveInit.PRESET_DIR).expect("Error: Unable to create preset directory");
            for (String s : DEFAULTS) {
                copyDefault(s + ".cave");
            }
        }
    }

    private static void copyDefault(String name) {
        final String fromLocation = ASSETS_PATH + "/presets/" + name;
        final String toLocation = CaveInit.PRESET_DIR + "/" + name;
        copyFile(fromLocation, toLocation);
    }

    private static void copyImports() {
        ensureDirExists(CaveInit.IMPORT_DIR).expect("Error creating import directory");
        for (String i : IMPORTS) {
            final String fromLocation = ASSETS_PATH + "/imports/" + i + ".cave";
            final String toLocation = CaveInit.IMPORT_DIR + "/" + i + ".cave";
            // Only copy each file if it doesn't already exist.
            if (!fileExists(new File(toLocation), "Unable to check " + toLocation)) {
                copyFile(fromLocation, toLocation);
            }
        }
    }

    private static void copyCatImports() {
        ensureDirExists(new File(CaveInit.IMPORT_DIR, CAT_FOLDER_NAME)).expect("Error creating cat directory");
        for (String i : CAT_IMPORTS) {
            final String fromLocation = ASSETS_PATH + "/imports/cat/" + i + ".cave";
            final String toLocation = CAT_DIR + "/" + i + ".cave";
            if (!fileExists(new File(toLocation), "Unable to check + " + toLocation)) {
                copyFile(fromLocation, toLocation);
            }
        }
    }

    /** Copies the example structures from the jar to the disk. */
    private static void copyStructures() {
        if (!fileExists(StructureSpawner.DIR, "Error: Unable to read from structure directory")) {
            // The directory doesn't exist. Create it.
            mkdirs(StructureSpawner.DIR)
                .expect("Error: Unable to create structure directory.");

            for (String structure : STRUCTURES) {
                final String fromLocation = ASSETS_PATH + "/structures/" + structure + ".nbt";
                final String toLocation = StructureSpawner.DIR + "/" + structure + ".nbt";
                copyFile(fromLocation, toLocation);
            }
        }
    }

    /** Copies the tutorial file into this mod's root directory. */
    private static void copyTutorial() {
        // Copy the regular tutorial file.
        final String fromLocation = ASSETS_PATH + "/" + TUTORIAL_NAME;
        final String toLocation = CaveInit.CG_DIR + "/" + TUTORIAL_NAME;
        copyFile(fromLocation, toLocation);

        // Copy the condensed version.
        // Todo: Generate this file.
        final String fromLocation2 = ASSETS_PATH + "/" + REFERENCE_NAME;
        final String toLocation2 = CaveInit.CG_DIR + "/" + REFERENCE_NAME;
        copyFile(fromLocation2, toLocation2);
    }

    /**
     * Copies any file from the jar to the disk.
     * To-do: Find a way to handle this using Result.
     */
    private static void copyFile(String fromLocation, String toLocation) {
        try {
            final InputStream toCopy = getRequiredResource(fromLocation);
            final FileOutputStream output = new FileOutputStream(toLocation);
            copyStream(toCopy, output, 1024) // This error should be handled now.
                .expect("Error copying file data. Perhaps the jar is corrupt.");
            output.close();
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Unable to open FileOutputStream", e);
        } catch (IOException e) {
            throw new RuntimeException("Error closing FileOutputStream", e);
        }
    }
}