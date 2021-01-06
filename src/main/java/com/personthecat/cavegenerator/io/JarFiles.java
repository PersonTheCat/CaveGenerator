package com.personthecat.cavegenerator.io;

import com.personthecat.cavegenerator.CaveInit;
import com.personthecat.cavegenerator.Main;
import com.personthecat.cavegenerator.world.feature.StructureSpawner;
import net.minecraftforge.fml.common.Loader;

import java.io.*;

import static com.personthecat.cavegenerator.io.SafeFileIO.*;

public class JarFiles {

    /** A setting indicating the location where example presets will be kept. */
    private static final String FOLDER = "cavegenerator/example_presets";
    private static final File EXAMPLE_DIR = new File(Loader.instance().getConfigDir(), FOLDER);

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
    public static void copyPresetFiles() {
        // Verify the folder's integrity before proceeding.
        ensureDirExists(EXAMPLE_DIR)
            .expect("Error: Unable to create the example preset directory.");
        ensureDirExists(CaveInit.IMPORT_DIR)
            .expect("Error: Unable to create the import directory.");

        for (String fileName : PRESETS) {
            String fromLocation = "assets/cavegenerator/presets/" + fileName + ".cave";
            String toLocation = EXAMPLE_DIR.getPath() + "/" + fileName + ".cave";
            copyFile(fromLocation, toLocation);
        }
        // Checks whether the preset folder exists.
        if (!safeFileExists(CaveInit.PRESET_DIR, "Error: Unable to read from preset directory.")) {
            // The directory doesn't exist. Create it.
            safeMkdirs(CaveInit.PRESET_DIR)
                .expect("Error: Unable to create preset directory.");
            // Copy only the vanilla preset. The others should be modifiable.
            // To-do: There was talk about changing this.
            String fromLocation = "assets/cavegenerator/presets/vanilla.cave";
            String toLocation = CaveInit.PRESET_DIR.getPath() + "/vanilla.cave";
            copyFile(fromLocation, toLocation);
        }
        // Also copy any import presets.
        for (String i : IMPORTS) {
            String fromLocation = "assets/cavegenerator/imports/" + i + ".cave";
            String toLocation = CaveInit.IMPORT_DIR.getPath() + "/" + i + ".cave";
            // Only copy each file if it doesn't already exist.
            if (!safeFileExists(new File(toLocation), "Unable to check " + toLocation)) {
                copyFile(fromLocation, toLocation);
            }
        }
    }

    /** Copies the example structures from the jar to the disk. */
    public static void copyExampleStructures() {
        if (!safeFileExists(StructureSpawner.DIR, "Error: Unable to read from structure directory.")) {
            // The directory doesn't exist. Create it.
            safeMkdirs(StructureSpawner.DIR)
                .expect("Error: Unable to create structure directory.");

            for (String structure : STRUCTURES) {
                String fromLocation = "assets/cavegenerator/structures/" + structure + ".nbt";
                String toLocation = StructureSpawner.DIR.getPath() + "/" + structure + ".nbt";
                copyFile(fromLocation, toLocation);
            }
        }
    }

    /**
     * Copies any file from the jar to the disk.
     * To-do: Find a way to handle this using Result.
     */
    private static void copyFile(String fromLocation, String toLocation) {
        try {
            InputStream toCopy = Main.class.getClassLoader().getResourceAsStream(fromLocation);
            FileOutputStream output = new FileOutputStream(toLocation);
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