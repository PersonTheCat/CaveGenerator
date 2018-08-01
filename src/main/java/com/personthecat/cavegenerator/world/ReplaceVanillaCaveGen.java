package com.personthecat.cavegenerator.world;

import net.minecraftforge.event.terraingen.InitMapGenEvent;
import net.minecraftforge.event.terraingen.InitMapGenEvent.EventType;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class ReplaceVanillaCaveGen
{
	@SubscribeEvent
	public static void onMapGen(InitMapGenEvent event)
	{
		if (event.getType().equals(EventType.CAVE))
		{
			event.setNewGen(new CaveManager());
		}
		else if (event.getType().equals(EventType.RAVINE))
		{
			event.setNewGen(new RavineManager());
		}
	}
}
