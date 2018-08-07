package com.personthecat.cavegenerator.world;

import net.minecraft.world.gen.MapGenBase;
import net.minecraftforge.event.terraingen.InitMapGenEvent;
import net.minecraftforge.event.terraingen.InitMapGenEvent.EventType;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class ReplaceVanillaCaveGen
{
	/**
	 * Not the same as using {@link InitMapGenEvent#getOriginalGen()}.
	 * Can also be used to retrieve MGB instances from other mods.
	 */
	protected static MapGenBase previousCaveGen, previousRavineGen;
	
	@SubscribeEvent
	public static void onMapGen(InitMapGenEvent event)
	{
		if (event.getType().equals(EventType.CAVE))
		{
			previousCaveGen = event.getNewGen();
			
			event.setNewGen(new CaveManager());
		}
		else if (event.getType().equals(EventType.RAVINE))
		{
			previousRavineGen = event.getNewGen();
			
			event.setNewGen(new RavineManager());
		}
	}
}