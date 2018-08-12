package com.personthecat.cavegenerator.world;

import com.personthecat.cavegenerator.CaveInit;
import com.personthecat.cavegenerator.config.ConfigFile;

import net.minecraftforge.event.terraingen.OreGenEvent.GenerateMinable;
import net.minecraftforge.fml.common.eventhandler.Event.Result;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class DisableVanillaStoneGen
{
	@SubscribeEvent
	public static void onDisableVanillaStoneGen(GenerateMinable event)
	{
		if (!ConfigFile.enableVanillaStoneClusters)
		{
			int dimension = event.getWorld().provider.getDimension();
			
			if (CaveInit.isAnyGeneratorEnabledForDimension(dimension))
			{
				switch (event.getType())
				{
					case ANDESITE :
					case DIORITE :
					case GRANITE : event.setResult(Result.DENY);
					
					default : return;
				}
			}	
		}
	}
}