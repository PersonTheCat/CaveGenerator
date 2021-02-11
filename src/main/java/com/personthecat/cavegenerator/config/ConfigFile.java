package com.personthecat.cavegenerator.config;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.Config.Comment;
import net.minecraftforge.common.config.Config.RequiresMcRestart;
import net.minecraftforge.common.config.Config.RequiresWorldRestart;

@Config(modid = "cavegenerator")
public class ConfigFile {

    @Comment({
        "Whether vanilla stone clusters--including andesite,",
        "diorite, and granite--should spawn in the world."})
    @RequiresMcRestart
    public static boolean enableVanillaStoneClusters = true;

    @Comment("Whether to enable vanilla water lakes underground.")
    public static boolean enableWaterLakes = true;

    @Comment("Whether to enable vanilla lava lakes underground.")
    public static boolean enableLavaLakes = true;

    @Comment({
        "Whether this mod will attempt to run simultaneously",
        "with one other cave generation mod, such as Worley's",
        "Caves or Yung's Better Caves."})
    @RequiresMcRestart
    public static boolean otherGeneratorEnabled = false;

    @Comment({
        "When this field is set to true, PresetTester is allowed to",
        "crash the game when more serious errors are detected. Users",
        "who are more serious about creating cleaner and more efficient",
        "presets should consider enabling this field to make sure that",
        "nothing slips by."})
    public static boolean strictPresets = false;

    @Comment({
        "Enables the experimental feature of restricting caverns",
        "objects by biome. This will produce flat walls in your",
        "world and is not the preferred solution."})
    public static boolean forceEnableCavernBiomes = false;

    @Comment({
        "A list of dimensions where HeightMapLocator will check for the",
        "surface to avoid spawning caverns in water. Disable this in your",
        "dimension if you don't have regular oceans spawning."
    })
    @RequiresWorldRestart
    public static int[] heightMapDims = { 0 };
}
