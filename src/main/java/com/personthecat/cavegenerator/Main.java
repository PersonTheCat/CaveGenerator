package com.personthecat.cavegenerator;

import com.personthecat.cavegenerator.commands.CommandCave;
import com.personthecat.cavegenerator.io.JarFiles;
import com.personthecat.cavegenerator.world.*;
import com.personthecat.cavegenerator.world.feature.CaveFeatureGenerator;
import com.personthecat.cavegenerator.world.feature.StructureSpawner;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.Map.Entry;
import java.util.HashMap;
import java.util.TreeMap;

import static com.personthecat.cavegenerator.util.CommonMethods.*;

@Mod(
    modid = Main.MODID,
    name = "Cave Generator",
    version = "0.18",
    dependencies = "after:worleycaves;",
    acceptableRemoteVersions = "*"
)
public class Main {

    /** The main instance of this mod, as required by Forge. */
    @Instance public static Main instance;

    /** This mod's ID and namespace. */
    public static final String MODID = "cavegenerator";

    /** A non-null log4j logger, matching Forge's formatting conventions. */
    public final Logger logger = LogManager.getLogger(MODID);

    /** A non-null map of ID -> CaveGenerator to be filled on WorldEvent.Load. */
    public final Int2ObjectOpenHashMap<Map<String, CaveGenerator>> generators = new Int2ObjectOpenHashMap<>();

    /** A non-null map of ID -> GeneratorSettings to be filled at runtime. */
    public final Map<String, GeneratorSettings> presets = new TreeMap<>();

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
        GameRegistry.registerWorldGenerator(new CaveFeatureGenerator(), 0);
        info("Cave Generator init phase complete.");
    }

    @EventHandler
    @SuppressWarnings("unused")
    public static void onServerStartingEvent(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandCave());
        info("Cave Generator commands registered.");
    }

    @EventHandler
    @SuppressWarnings("unused")
    public static void onServerStoppingEvent(FMLServerStoppingEvent event) {
        info("Unloading generators.");
        Main.instance.generators.clear();
    }

    /** Loads a generator for the current dimension, if applicable. */
    public Map<String, CaveGenerator> loadGenerators(World world) {
        final int dim = world.provider.getDimension();
        if (generators.containsKey(dim)) {
            return this.generators.get(dim);
        }
        final Map<String, CaveGenerator> generators = new TreeMap<>();
        for (Entry<String, GeneratorSettings> entry : presets.entrySet()) {
            final GeneratorSettings cfg = entry.getValue();
            if (CaveInit.validPreset(cfg, dim)) {
                generators.put(entry.getKey(), new CaveGenerator(world, entry.getValue()));
            }
        }
        this.generators.put(dim, generators);
        return generators;
    }
}