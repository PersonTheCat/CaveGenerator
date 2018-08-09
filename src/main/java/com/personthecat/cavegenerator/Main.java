package com.personthecat.cavegenerator;

import org.apache.logging.log4j.Logger;

import com.personthecat.cavegenerator.config.ConfigFile;
import com.personthecat.cavegenerator.proxy.CommonProxy;
import com.personthecat.cavegenerator.util.CommonMethods;
import com.personthecat.cavegenerator.world.ReplaceVanillaCaveGen;
import com.personthecat.cavegenerator.world.anticascade.CorrectionStorage;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;

@Mod(modid = "cavegenerator",
     name = "Cave Generator",
     version = "0.5"
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
	}
	
	@EventHandler
	public static void init(FMLInitializationEvent event)
	{
		CommonMethods.copyPresetFiles();
		CaveInit.init();		
		MinecraftForge.TERRAIN_GEN_BUS.register(ReplaceVanillaCaveGen.class);
		
		logger.info("Cave Generator init phase complete.");
	}
	
	/**
	 * Used to avoid cascading gen lag. Still not ideal. Beta.
	 */
	@EventHandler
	public static void onServerAboutToStartEvent(FMLServerAboutToStartEvent event)
	{
		logger.info("Decorate walls option set to " + ConfigFile.decorateWallsOption);
		
		if (ConfigFile.decorateWallsOption == 1)
		{
			CorrectionStorage.setCorrectionsFromSave(event.getServer().getFolderName());
		}
	}

	@EventHandler
	public static void onServerStartingEvent(FMLServerStartingEvent event)
	{
		event.registerServerCommand(new CommandReloadCaves());
	}
	
	/**
	 * Has to happen here as opposed to WorldEvent.Save. 
	 * Avoid repeatedly writing to the disk.
	 */
	@EventHandler
	public static void onServerStoppingEvent(FMLServerStoppingEvent event)
	{
		if (ConfigFile.decorateWallsOption == 1)
		{
			CorrectionStorage.recordCorrections();
		}		
	}
}