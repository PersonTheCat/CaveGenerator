package com.personthecat.cavegenerator;

import com.personthecat.cavegenerator.util.CommonMethods;
import com.personthecat.cavegenerator.world.ReplaceVanillaCaveGen;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;

@Mod(modid = "cavegenerator",
     name = "Cave Generator",
     version = "0.4"
)
public class Main
{
	@Instance
	public static Main instance;
	
	@EventHandler
	public static void init(FMLInitializationEvent event)
	{
		CommonMethods.copyPresetFiles();
		CaveInit.init();		
		MinecraftForge.TERRAIN_GEN_BUS.register(ReplaceVanillaCaveGen.class);
	}

	@EventHandler
	public static void onServerStartingEvent(FMLServerStartingEvent event)
	{
		event.registerServerCommand(new CommandReloadCaves());
	}
}