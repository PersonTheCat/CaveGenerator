package com.personthecat.cavegenerator.world.feature;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.structure.template.PlacementSettings;
import net.minecraft.world.gen.structure.template.Template;
import net.minecraft.world.gen.structure.template.TemplateManager;
import net.minecraftforge.fml.common.Loader;
import com.personthecat.cavegenerator.util.Result;
import static com.personthecat.cavegenerator.util.CommonMethods.*;
import static com.personthecat.cavegenerator.io.SafeFileIO.*;

public class StructureSpawner {
    /** A setting indicating the location where structures will be kept. */
    private static final String FOLDER = "cavegenerator/structures";
    public static final File DIR = new File(Loader.instance().getConfigDir(), FOLDER);

    /** Loads all structures into the container from the disk. */
    public static void loadAllStructures(final Map<String, Template> structures) {
        // Verify the folder's integrity before proceeding.
        ensureDirExists(DIR)
            .expect("Error: Unable to create the structure directory. It is most likely in use.");

        // Clear the structures. Allows them to be reloaded.
        structures.clear();

        // Safely view the files in this directory.
        safeListFiles(DIR).ifPresent((files) -> { // Files found.
            for (File file : files) {
                // Limit to nbt files only.
                if (!file.getName().endsWith(".nbt")) {
                    continue;
                }
                // Create a template.
                Template template = new Template();
                String name = file.getName();
                // Read into the template, handling exceptions.
                read(template, file)
                    .expectF("Error: unable to read structure file {}", name);
                // Warn users if the structure is too large.
                warnSizeLimitations(template, name);
                // Finally, place the structure into the map.
                structures.put(removeNbt(name), template);
            }
        });
        // To-do: Make sure to return an error when nothing is returned.
    }

    /** Attempts to read nbt data from the input `nbt` into `template`. */
    private static Result<IOException> read(Template template, File nbt) {
        try {
            template.read(CompressedStreamTools.readCompressed(new FileInputStream(nbt)));
        } catch (IOException e) {
            return Result.of(e);
        }
        return Result.ok();
    }

    /** Warns the user if their structure is too large. */
    private static void warnSizeLimitations(Template template, String name) {
        if (template.getSize().getX() > 15 || template.getSize().getZ() > 15) {
            warn("Large structures are not yet fully supported. Expect cascading generation lag caused by {}.", name);
        }
    }

    /**
     * Attempts to load a template from the map or TemplateManager.
     * throws a RuntimeException when no template is found.
     */
    public static Template getTemplate(Map<String, Template> structures, String fileOrResource, World world) {
        fileOrResource = removeNbt(fileOrResource);
        // Attempt to load the preset directly from the map.
        Optional<Template> fromMap = safeGet(structures, fileOrResource);
        if (fromMap.isPresent()) {
            return fromMap.get();
        }
        // Verify that we're on the server side, first.
        if (world.isRemote) {
            throw runEx("Build error: Somehow called StructureSpawner#getTemplate with an invalid World object.");
        }
        // It wasn't found. Try getting it from TemplateManager.
        WorldServer worldServer = (WorldServer) world;
        MinecraftServer mcServer = world.getMinecraftServer();
        TemplateManager manager = worldServer.getStructureTemplateManager();
        ResourceLocation location = new ResourceLocation(fileOrResource);
        // This isn't safe. Scary.
        Template template = manager.get(mcServer, location);
        // He remembered to check.
        if (template == null) {
            throw templateNotFound(fileOrResource);
        }
        return template;
    }

    /** Indicates that no template was found by the above search. */
    private static RuntimeException templateNotFound(String name) {
        return runExF("Error: No template named \"{}\" was found. Please verify that this refers to a valid file or registry name.", name);
    }

    /** Does not account for offsets. Must be calculated beforehand. */
    public static void spawnStructure(Template template, PlacementSettings settings, World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        world.notifyBlockUpdate(pos, state, state, 3);
        template.addBlocksToWorld(world, pos, settings);
    }

    private static String removeNbt(String name) {
        return name.replace(".nbt", "");
    }
}
