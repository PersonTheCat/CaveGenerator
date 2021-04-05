package com.personthecat.cavegenerator.io;

import com.personthecat.cavegenerator.CaveInit;
import com.personthecat.cavegenerator.Main;
import com.personthecat.cavegenerator.world.feature.StructureSpawner;
import net.minecraftforge.fml.common.Loader;

import java.io.*;

import static com.personthecat.cavegenerator.io.SafeFileIO.*;

public class JarFiles {

    /** A setting indicating the parent folder of all presets. */
    private static final File ROOT_DIR = new File(Loader.instance().getConfigDir(), Main.MODID);

    /** A setting indicating the location where example presets will be kept. */
    private static final String EXAMPLE_PATH = ROOT_DIR + "/example_presets";

    /** The actual folder containing the example presets. */
    private static final File EXAMPLE_DIR = new File(EXAMPLE_PATH);

    /** The path where all of this mod's assets are stored. */
    private static final String ASSETS_PATH = "assets/" + Main.MODID;

    /** The name of the vanilla preset file. */
    private static final String VANILLA_NAME = "vanilla.cave";

    /** The name of the tutorial file. */
    private static final String TUTORIAL_NAME = "TUTORIAL.cave";

    /** The name of the stripped tutorial file. */
    private static final String TUTORIAL_STRIPPED_NAME = "TUTORIAL_STRIPPED.cave";

    /** All of the <b>example</b> presets to be copied from the jar. */
    private static final String[] PRESETS = {
        "flooded_vanilla", "large_caves", "spirals",
        "tunnels", "caverns", "stone_layers", "stalactites",
        "ravines", "stone_clusters", "large_stalactites",
        "vanilla", "underground_forest", "euclids_tunnels",
        "lower_caves"
    };

    /** A couple of structure NBTs to be copied from the jar. */
    private static final String[] STRUCTURES = {
        "hanging_spawner", "red_mushroom"
    };

    /** Any preset that specifically belongs in /imports. */
    private static final String[] IMPORTS = {
        "defaults"
    };

    /** Copies the example presets from the jar to the disk. */
    public static void copyFiles() {
        // Verify the folders' integrity before proceeding.
        ensureDirExists(EXAMPLE_DIR)
            .expect("Error: Unable to create the example preset directory.");
        ensureDirExists(CaveInit.IMPORT_DIR)
            .expect("Error: Unable to create the import directory.");

        copyExamples();
        copyVanilla();
        copyImports();
        copyStructures();
        copyTutorial();
    }

    private static void copyExamples() {
        for (String fileName : PRESETS) {
            final String fromLocation = ASSETS_PATH + "/presets/" + fileName + ".cave";
            final String toLocation = EXAMPLE_DIR + "/" + fileName + ".cave";
            copyFile(fromLocation, toLocation);
        }
    }

    private static void copyVanilla() {
        if (!fileExists(CaveInit.PRESET_DIR, "Error: Unable to read from preset directory.")) {
            // The directory doesn't exist. Create it.
            mkdirs(CaveInit.PRESET_DIR)
                .expect("Error: Unable to create preset directory.");
            // Copy only the vanilla preset. The others should be modifiable.
            // To-do: There was talk about changing this.
            final String fromLocation = ASSETS_PATH + "/presets/" + VANILLA_NAME;
            final String toLocation = CaveInit.PRESET_DIR + "/" + VANILLA_NAME;
            copyFile(fromLocation, toLocation);
        }
    }

    private static void copyImports() {
        for (String i : IMPORTS) {
            final String fromLocation = ASSETS_PATH + "/imports/" + i + ".cave";
            final String toLocation = CaveInit.IMPORT_DIR + "/" + i + ".cave";
            // Only copy each file if it doesn't already exist.
            if (!fileExists(new File(toLocation), "Unable to check " + toLocation)) {
                copyFile(fromLocation, toLocation);
            }
        }
    }

    /** Copies the example structures from the jar to the disk. */
    private static void copyStructures() {
        if (!fileExists(StructureSpawner.DIR, "Error: Unable to read from structure directory.")) {
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
        final String toLocation = ROOT_DIR + "/" + TUTORIAL_NAME;
        copyFile(fromLocation, toLocation);

        // Copy the condensed version.
        // Todo: Generate this file.
        final String fromLocation2 = ASSETS_PATH + "/" + TUTORIAL_STRIPPED_NAME;
        final String toLocation2 = ROOT_DIR + "/" + TUTORIAL_STRIPPED_NAME;
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
            throw new RuntimeException("Unable to open FileOutputStream.", e);
        } catch (IOException e) {
            throw new RuntimeException("Error closing FileOutputStream.", e);
        }
    }
}