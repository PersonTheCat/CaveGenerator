package com.personthecat.cavegenerator;

import org.apache.logging.log4j.Logger;

import com.personthecat.cavegenerator.config.PresetCombiner;
import com.personthecat.cavegenerator.proxy.CommonProxy;
import com.personthecat.cavegenerator.util.CommonMethods;
import com.personthecat.cavegenerator.world.DisableVanillaStoneGen;
import com.personthecat.cavegenerator.world.ReplaceVanillaCaveGen;
import com.personthecat.cavegenerator.world.feature.CaveFeatureGenerator;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

@Mod(modid = "cavegenerator",
     name = "Cave Generator",
     version = "0.9",
     dependencies = "after:worleycaves;"
)
public class Main
{
	@Instance
	public static Main instance;
	
	@SidedProxy(clientSide = "com.personthecat.cavegenerator.proxy.ClientProxy", 
	            serverSide = "com.personthecat.cavegenerator.proxy.CommonProxy"
	)
	public static CommonProxy proxy;
	
	public static Logger logger;
	
	@EventHandler
	public static void preInit(FMLPreInitializationEvent event)
	{
		logger = event.getModLog();
		
		PresetCombiner.init();
	}
	
	@EventHandler
	public static void init(FMLInitializationEvent event)
	{
		CommonMethods.copyPresetFiles();
		CaveInit.init();		
		MinecraftForge.TERRAIN_GEN_BUS.register(ReplaceVanillaCaveGen.class);
		MinecraftForge.ORE_GEN_BUS.register(DisableVanillaStoneGen.class);
		GameRegistry.registerWorldGenerator(new CaveFeatureGenerator(), 0);
		
		logger.info("Cave Generator init phase complete.");
	}

	@EventHandler
	public static void onServerStartingEvent(FMLServerStartingEvent event)
	{
		event.registerServerCommand(new CommandReloadCaves());
	}
}