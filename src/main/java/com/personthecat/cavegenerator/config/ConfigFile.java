package com.personthecat.cavegenerator.config;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.Config.Comment;
import net.minecraftforge.common.config.Config.RangeInt;
import net.minecraftforge.common.config.Config.RequiresMcRestart;

@Config(modid = "cavegenerator")
public class ConfigFile
{
	@RequiresMcRestart()
	public static boolean enableVanillaStoneClusters = true;
	
	@RequiresMcRestart()
	public static boolean runAlongsideOtherCaveGenerators = false;
	
	@RequiresMcRestart()
	public static boolean runAlongsideOtherRavineGenerators = false;
	
	@Comment({
		"Moves fields from one preset to another. Could theoretically",
		"be used for any two jsons of the same type. This will remove",
		"comments and any two fields that have the same name. (i.e. \"//\")",
		"Syntax: originalPresetName.fieldName, newPresetName",
		"Example: stone_clusters.stoneClusters, vanilla",
		"Example: stalactites.blockFillers.stalactite, vanilla"})
	@RequiresMcRestart()
	public static String[] presetCombiners = new String[0];
}