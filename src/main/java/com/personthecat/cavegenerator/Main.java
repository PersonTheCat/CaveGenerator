package com.personthecat.cavegenerator;

import com.personthecat.cavegenerator.proxy.CommonProxy;
import com.personthecat.cavegenerator.util.CommonMethods;
import com.personthecat.cavegenerator.world.ReplaceVanillaCaveGen;
import com.personthecat.cavegenerator.world.anticascade.CorrectionStorage;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
import net.minecraftforge.fml.server.FMLServerHandler;

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
	
	@EventHandler
	public static void init(FMLInitializationEvent event)
	{
		CommonMethods.copyPresetFiles();
		CaveInit.init();		
		MinecraftForge.TERRAIN_GEN_BUS.register(ReplaceVanillaCaveGen.class);
	}
	
	/**
	 * Used to avoid cascading gen lag. Still not ideal.
	 */
	@EventHandler
	public static void onServerAboutToStartEvent(FMLServerAboutToStartEvent event)
	{
		CorrectionStorage.setCorrectionsFromSave(event.getServer().getFolderName());
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
		CorrectionStorage.recordCorrections();
	}
}