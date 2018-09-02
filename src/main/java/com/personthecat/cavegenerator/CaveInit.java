package com.personthecat.cavegenerator;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static com.personthecat.cavegenerator.Main.logger;

import com.personthecat.cavegenerator.config.PresetReader;
import com.personthecat.cavegenerator.world.CaveGenerator;

import net.minecraftforge.fml.common.Loader;

public class CaveInit
{
	public static final Map<String, CaveGenerator> GENERATORS = new HashMap<>();
	
	public static void init()
	{
		GENERATORS.clear();
		
		File presetFolder = new File(Loader.instance().getConfigDir().getPath() + "/cavegenerator/presets");
		
		for (File inDir : presetFolder.listFiles())
		{
			if (inDir.getName().endsWith("json"))
			{
				GENERATORS.put(inDir.getName().replaceAll(".json", ""), new PresetReader(inDir).getGenerator());
			}
		}
		
		printLoadedPresets();
	}
	
	private static void printLoadedPresets()
	{
		for (String name : GENERATORS.keySet())
		{
			logger.info(
				"Successfully loaded " + name + ".json. "
			  + "It is " + ((GENERATORS.get(name).enabledGlobally) ? "enabled." : "disabled."));
		}
	}
	
	public static boolean isPresetRegistered(String name)
	{
		for (String preset : GENERATORS.keySet())
		{
			if (preset.equalsIgnoreCase(name))
			{
				return true;
			}
		}
		
		return false;
	}
	
	public static boolean isAnyGeneratorEnabledForDimension(int dimension)
	{
		for (CaveGenerator generator : GENERATORS.values())
		{
			if (generator.enabledGlobally && generator.canGenerateInDimension(dimension))
			{
				return true;
			}
		}
		
		return false;
	}
	
	public static boolean correctionsNeeded()
	{
		for (CaveGenerator generator : GENERATORS.values())
		{
			if (generator.shouldPregenerateAir()) return true;
		}
			
		return false;
	}
}