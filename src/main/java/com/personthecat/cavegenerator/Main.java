package com.personthecat.cavegenerator;

import com.personthecat.cavegenerator.commands.CommandCave;
import com.personthecat.cavegenerator.config.CavePreset;
import com.personthecat.cavegenerator.io.JarFiles;
import com.personthecat.cavegenerator.noise.CachedNoiseHelper;
import com.personthecat.cavegenerator.world.*;
import com.personthecat.cavegenerator.world.event.DisablePopulateChunkEvent;
import com.personthecat.cavegenerator.world.event.DisableVanillaStoneGen;
import com.personthecat.cavegenerator.world.event.ReplaceVanillaCaveGen;
import com.personthecat.cavegenerator.world.feature.FeatureCaveHook;
import com.personthecat.cavegenerator.world.feature.StructureSpawner;
import lombok.extern.log4j.Log4j2;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.template.Template;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

import java.util.Map;
import java.util.HashMap;
import java.util.TreeMap;

@Mod(
    modid = Main.MODID,
    name = "Cave Generator",
    version = "1.0",
    dependencies = "after:worleycaves;",
    acceptableRemoteVersions = "*"
)
@Log4j2
public class Main {

    /** The main instance of this mod, as required by Forge. */
    @Instance public static Main instance;

    /** This mod's ID and namespace. */
    public static final String MODID = "cavegenerator";

    /** A non-null map of ID -> CaveGenerator to be filled on WorldEvent.Load. */
    public final Map<String, GeneratorController> generators = new TreeMap<>();

    /** A non-null map of ID -> GeneratorSettings to be filled at runtime. */
    public final Map<String, CavePreset> presets = new TreeMap<>();

    /** A non-null map of ID -> Structure to be filled at runtime. */
    public final Map<String, Template> structures = new HashMap<>();

    @EventHandler
    @SuppressWarnings("unused")
    public static void init(FMLInitializationEvent event) {
        JarFiles.copyFiles();
        StructureSpawner.loadAllStructures(instance.structures);
        CaveInit.initPresets(instance.presets);
        MinecraftForge.EVENT_BUS.register(CaveInit.class);
        MinecraftForge.TERRAIN_GEN_BUS.register(ReplaceVanillaCaveGen.class);
        MinecraftForge.ORE_GEN_BUS.register(DisableVanillaStoneGen.class);
        MinecraftForge.TERRAIN_GEN_BUS.register(DisablePopulateChunkEvent.class);
        GameRegistry.registerWorldGenerator(new FeatureCaveHook(), 0);
        log.info("Cave Generator init phase complete.");
    }

    @EventHandler
    @SuppressWarnings("unused")
    public static void onServerStartingEvent(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandCave());
        log.info("Cave Generator commands registered.");
    }

    @EventHandler
    @SuppressWarnings("unused")
    public static void onServerStoppingEvent(FMLServerStoppingEvent event) {
        log.info("Unloading generators.");
        Main.instance.generators.clear();
        CachedNoiseHelper.removeAll();
    }

    /** Loads a generator for the current dimension, if applicable. */
    public Map<String, GeneratorController> loadGenerators(World world) {
        if (presets.isEmpty()) {
            return generators; // i.e. never load them.
        }
        if (generators.isEmpty()) {
            for (Map.Entry<String, CavePreset> entry : presets.entrySet()) {
                final CavePreset preset = entry.getValue();
                if (preset.enabled) {
                    generators.put(entry.getKey(), GeneratorController.from(preset, world));
                }
            }
        }
        return generators;
    }
}