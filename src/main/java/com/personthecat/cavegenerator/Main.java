package com.personthecat.cavegenerator;

import static com.personthecat.cavegenerator.util.CommonMethods.*;
import com.personthecat.cavegenerator.commands.CommandCave;
import com.personthecat.cavegenerator.util.JarFiles;
import com.personthecat.cavegenerator.world.CaveGenerator;
import com.personthecat.cavegenerator.world.DisableVanillaStoneGen;
import com.personthecat.cavegenerator.world.GeneratorSettings;
import com.personthecat.cavegenerator.world.ReplaceVanillaCaveGen;
import com.personthecat.cavegenerator.world.feature.CaveFeatureGenerator;
import com.personthecat.cavegenerator.world.feature.StructureSpawner;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntSortedSet;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.MapGenBase;
import net.minecraft.world.gen.structure.template.Template;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLLoadCompleteEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;

import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.HashMap;
import java.util.Optional;

@Mod(
    modid = "cavegenerator",
    name = "Cave Generator",
    version = "0.15",
    dependencies = "after:worleycaves;",
    acceptableRemoteVersions = "*"
)
public class Main {
    /** The main instance of this mod, as required by Forge. */
    @Instance public static Main instance;
    /** A non-null log4j logger, matching Forge's formatting conventions. */
    public final Logger logger = LogManager.getLogger("cavegenerator");
    /** A non-null map of ID -> CaveGenerator to be filled on WorldEvent.Load. */

    public final Int2ObjectOpenHashMap<Map<String, CaveGenerator>> generatorMap = new Int2ObjectOpenHashMap<>();

    //public final Map<String, CaveGenerator> generators = new HashMap<>();
    /** A non-null map of ID -> GeneratorSettings to be filled at runtime. */
    public final Map<String, GeneratorSettings> presets = new HashMap<>();
    /** A non-null map of ID -> Structure to be filled at runtime. */
    public final Map<String, Template> structures = new HashMap<>();
    /** A non-null instance of the most recent, non-vanilla cave generator. */
    public Optional<MapGenBase> priorCaves = empty();
    /** A non-null instance of the most recent, non-vanilla ravine generator. */
    public Optional<MapGenBase> priorRavines = empty();

    @EventHandler
    @SuppressWarnings("unused")
    public static void init(FMLInitializationEvent event) {
        JarFiles.copyPresetFiles();
        JarFiles.copyExampleStructures();
        StructureSpawner.loadAllStructures(instance.structures);
        CaveInit.forceInitPresets(instance.presets);
        MinecraftForge.EVENT_BUS.register(CaveInit.class);
        MinecraftForge.TERRAIN_GEN_BUS.register(ReplaceVanillaCaveGen.class);
        MinecraftForge.ORE_GEN_BUS.register(DisableVanillaStoneGen.class);
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
        //instance.generators.clear();
        //info("All cave generators unloaded successfully.");
    }

    public Map<String, CaveGenerator> getGenerators (World world) {
        int dim = world.provider.getDimension();
        if (generatorMap.containsKey(dim)) {
            Map<String, CaveGenerator> generators = generatorMap.get(dim);
            generators.values().forEach(cg -> cg.updateWorld(world));
            return generators;
        }

        Map<String, CaveGenerator> generators = new HashMap<>();
        for (Map.Entry<String, GeneratorSettings> entry : presets.entrySet()) {
            generators.put(entry.getKey(), new CaveGenerator(world, entry.getValue()));
        }
        generatorMap.put(dim, generators);
        return generators;
    }
}