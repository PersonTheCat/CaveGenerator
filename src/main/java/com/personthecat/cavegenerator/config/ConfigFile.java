package com.personthecat.cavegenerator.config;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.Config.Comment;
import net.minecraftforge.common.config.Config.RequiresMcRestart;

@Config(modid = "cavegenerator")
public class ConfigFile {
    @Comment({
        "Whether vanilla stone clusters--including andesite,",
        "diorite, and granite--should spawn in the world."})
    @RequiresMcRestart()
    public static boolean enableVanillaStoneClusters = true;

    @Comment({
        "Whether this mod will attempt to run simultaneously",
        "with one other cave generation mod, such as Worley's",
        "Caves."})
    @RequiresMcRestart()
    public static boolean otherGeneratorEnabled = false;

    @Comment({
        "When this field is set to true, PresetTester is allowed to",
        "crash the game when more serious errors are detected. Users",
        "who are more serious about creating cleaner and more efficient",
        "presets should consider enabling this field to make sure that",
        "nothing slips by."})
    public static boolean strictPresets = false;

    @Comment("Temporary method used to alter the cavern / stone generation order.")
    public static boolean debugOrder = true;
}
