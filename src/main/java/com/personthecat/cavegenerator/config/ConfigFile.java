package com.personthecat.cavegenerator.config;

import com.personthecat.cavegenerator.Main;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.Config.Comment;
import net.minecraftforge.common.config.Config.RequiresMcRestart;
import net.minecraftforge.common.config.Config.RequiresWorldRestart;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber
@Config(modid = Main.MODID)
public class ConfigFile {

    @Comment({
        "Whether vanilla stone clusters--including andesite,",
        "diorite, and granite--should spawn in the world."})
    @RequiresMcRestart
    public static boolean enableVanillaStoneClusters = true;

    @Comment("Whether to enable vanilla water lakes underground.")
    @RequiresMcRestart
    public static boolean enableWaterLakes = true;

    @Comment("Whether to enable vanilla lava lakes underground.")
    @RequiresMcRestart
    public static boolean enableLavaLakes = true;

    @Comment("Whether to enable vanilla mineshafts underground.")
    @RequiresMcRestart
    public static boolean enableMineshafts = true;

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
        "A list of dimensions where HeightMapLocator will check for the",
        "surface to avoid spawning caverns in water. Disable this in your",
        "dimension if you don't have regular oceans spawning."
    })
    @RequiresWorldRestart
    public static int[] heightMapDims = { 0 };

    @Comment("The chunk search range for tunnel and ravine features.")
    public static int mapRange = 8;

    @Comment({
        "The range in chunks to read biomes for features that use",
        "distance-based biome testing."})
    public static int biomeRange = 2;

    @Comment("Whether to override and replace caverns in the nether.")
    @RequiresMcRestart
    public static boolean netherGenerate = false;

    @Comment({
        "Whether to automatically format your preset files. They will",
        "still be reformatted if values are updated."})
    public static boolean autoFormat = true;

    @SubscribeEvent
    public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (event.getModID().equals(Main.MODID)) {
            ConfigManager.sync(Main.MODID, Config.Type.INSTANCE);
        }
    }
}
