package personthecat.cavegenerator.config;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Comment;
import personthecat.catlib.data.Lazy;
import personthecat.cavegenerator.util.Reference;
import personthecat.overwritevalidator.annotations.Overwrite;
import personthecat.overwritevalidator.annotations.OverwriteClass;

import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;

@OverwriteClass
@Config(name = Reference.MOD_ID)
public class Cfg implements ConfigData {

    @Comment(
        "Whether vanilla stone clusters--including andesite,\n" +
        "diorite, and granite--should spawn in the world.")
    public boolean enableVanillaStoneClusters = false;

    @Comment("Whether to enable vanilla water lakes underground.")
    public boolean enableWaterLakes = false;

    @Comment("Whether to enable vanilla lava lakes underground.")
    public boolean enableLavaLakes = false;

    @Comment("Whether to enable vanilla mineshafts underground.")
    public boolean enableMineshafts = true;

    @Comment(
        "Whether this mod will attempt to run simultaneously\n" +
        "with one other cave generation mod, such as Worley's\n" +
        "Caves or Yung's Better Caves.")
    public boolean otherGeneratorsEnabled = false;

    @Comment(
        "When this field is set to true, PresetTester is allowed to\n" +
        "crash the game when more serious errors are detected. Users\n" +
        "who are serious about creating cleaner and more efficient\n" +
        "presets should consider enabling this field to make sure that\n" +
        "nothing slips by.")
    public boolean strictPresets = false;

    @Comment(
        "When this field is set to true, PresetTester will skip over\n" +
        "any invalid presets and simply not load them. Make sure to\n" +
        "check your log to determine if any presets erred.")
    public boolean ignoreInvalidPresets = false;

    @Comment("Whether to override and replace caverns in the nether.")
    public boolean netherGenerate = false;

    @Comment(
        "Whether to automatically format your preset files. They will\n" +
        "still be reformatted if values are updated.")
    public boolean autoFormat = false;

    @Comment(
        "Whether to automatically generate preset files inside of\n" +
        "cavegenerator/generated. This will help you see how your\n" +
        "variables are getting expanded every time you reload your\n" +
        "presets.")
    public boolean autoGenerate = false;

    @Comment(
        "Whether to automatically update import files, as much as\n" +
        "possible. Note that compatibility updates will still occur.")
    public boolean updateImports = true;

    @Comment("The chunk search range for tunnel and ravine features.")
    public int mapRange = 8;

    @Comment(
        "The range in chunks to read biomes for features that use" +
        "distance-based biome testing.")
    public int biomeRange = 2;

    private static final Lazy<Cfg> CONFIG =
        Lazy.of(() -> AutoConfig.getConfigHolder(Cfg.class).getConfig());

    @Overwrite
    public static final BooleanSupplier ENABLE_VANILLA_STONE_CLUSTERS =
        () -> CONFIG.get().enableVanillaStoneClusters;

    @Overwrite
    public static final BooleanSupplier ENABLE_WATER_LAKES =
        () -> CONFIG.get().enableWaterLakes;

    @Overwrite
    public static final BooleanSupplier ENABLE_LAVA_LAKES =
        () -> CONFIG.get().enableLavaLakes;

    @Overwrite
    public static final BooleanSupplier ENABLE_MINESHAFTS =
        () -> CONFIG.get().enableMineshafts;

    @Overwrite
    public static final BooleanSupplier ENABLE_OTHER_GENERATORS =
        () -> CONFIG.get().enableOtherGenerators;

    @Overwrite
    public static final BooleanSupplier STRICT_PRESETS =
        () -> CONFIG.get().strictPresets;

    @Overwrite
    public static final BooleanSupplier IGNORE_INVALID_PRESETS =
        () -> CONFIG.get().ignoreInvalidPresets;

    @Overwrite
    public static final BooleanSupplier NETHER_GENERATE =
        () -> CONFIG.get().netherGenerate;

    @Overwrite
    public static final BooleanSupplier AUTO_FORMAT =
        () -> CONFIG.get().autoFormat;

    @Overwrite
    public static final BooleanSupplier AUTO_GENERATE =
        () -> CONFIG.get().autoGenerate;

    @Overwrite
    public static final BooleanSupplier UPDATE_IMPORTS =
        () -> CONFIG.get().updateImports;

    @Overwrite
    public static final IntSupplier MAP_RANGE =
        () -> CONFIG.get().mapRange;

    @Overwrite
    public static final IntSupplier BIOME_RANGE =
        () -> CONFIG.get().biomeRange;
}
