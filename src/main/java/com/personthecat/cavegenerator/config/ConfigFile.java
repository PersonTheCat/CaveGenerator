package com.personthecat.cavegenerator.config;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.Config.Comment;
import net.minecraftforge.common.config.Config.RangeInt;

@Config(modid = "cavegenerator")
public class ConfigFile
{
	@Comment({"Used to allow BlockFillers to decorate chunk borders.",
			  "Experimental. This option will not exist forever.",
			  "Feedback is welcome.", 
			  "",
			  "\t" + "0 = off",
			  "\t" + "1 = save corrections to disk (moderately fast)",
			  "\t" + "2 = discover corrections procedurally (slow)",
			  "",
			  "Option 1 may cause problems for slower drives.",
			  ""})
	@RangeInt(min = 0, max = 2)
	public static int decorateWallsOption = 0;
}