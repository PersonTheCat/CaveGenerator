package personthecat.cavegenerator.config;

import lombok.experimental.UtilityClass;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;
import personthecat.catlib.config.HjsonFileConfig;
import personthecat.catlib.util.McUtils;
import personthecat.cavegenerator.util.Reference;
import personthecat.overwritevalidator.annotations.Overwrite;
import personthecat.overwritevalidator.annotations.OverwriteClass;

import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;

@UtilityClass
@OverwriteClass
public class Cfg {

    private static final ForgeConfigSpec.Builder COMMON = new ForgeConfigSpec.Builder();
    private static final String FILENAME = McUtils.getConfigDir() + "/" + Reference.MOD_ID;
    private static final HjsonFileConfig COMMON_CFG = new HjsonFileConfig(FILENAME + "-common.hjson");

    private static final BooleanValue ENABLE_VANILLA_STONE_CLUSTERS_VALUE = COMMON
        .comment("Whether vanilla stone clusters--including andesite",
                "diorite, and granite--should spawn in the world.")
        .define("enableVanillaStoneClusters", false);

    private static final BooleanValue ENABLE_WATER_LAKES_VALUE = COMMON
        .comment("Whether to enable vanilla water lakes underground.")
        .define("enableWaterLakes", false);

    private static final BooleanValue ENABLE_LAVA_LAKES_VALUE = COMMON
        .comment("Whether to enable vanilla lava lakes underground.")
        .define("enableLavaLakes", false);

    private static final BooleanValue ENABLE_MINESHAFTS_VALUE = COMMON
        .comment("Whether to enable vanilla mineshafts underground")
        .define("enableMineshafts", true);

    private static final BooleanValue ENABLE_OTHER_GENERATORS_VALUE = COMMON
        .comment("Whether this mod will attempt to run simultaneously",
                "with one other cave generation mod, such as Worley's",
                "Caves or Yung's Better Caves.")
        .define("enableOtherGenerators", false);

    private static final BooleanValue STRICT_PRESETS_VALUE = COMMON
        .comment("When this field is set to true, PresetTester is allowed to",
                "crash the game when more serious errors are detected. Users",
                "who are serious about creating cleaner and more efficient",
                "presets should consider enabling this field to make sure that",
                "nothing slips by.")
        .define("strictPresets", false);

    private static final BooleanValue IGNORE_INVALID_PRESETS_VALUE = COMMON
        .comment("When this field is set to true, PresetTester will skip over",
                "any invalid presets and simply not load them. Make sure to",
                "check your log to determine if any presets erred.")
        .define("ignoreInvalidPresets", false);

    private static final BooleanValue NETHER_GENERATE_VALUE = COMMON
        .comment("Whether to override and replace caverns in the nether.")
        .define("netherGenerate", false);

    private static final BooleanValue AUTO_FORMAT_VALUE = COMMON
        .comment("Whether to automatically format your preset files. They will",
                "still be reformatted if values are updated.")
        .define("autoFormat", true);

    private static final BooleanValue AUTO_GENERATE_VALUE = COMMON
        .comment("Whether to automatically generate preset files inside of",
                "cavegenerator/generated. This will help you see how your",
                "variables are getting expanded every time you reload your",
                "presets.")
        .define("autoGenerate", false);

    private static final BooleanValue UPDATE_IMPORTS_VALUE = COMMON
        .comment("Whether to automatically update import files, as much",
                "as possible. Note that compatibility updates will still",
                "occur.")
        .define("updateImports", true);

    private static final IntValue MAP_RANGE_VALUE = COMMON
        .comment("The search range for tunnel and ravine features.")
        .defineInRange("mapRange", 8, 1, 20);

    private static final IntValue BIOME_RANGE_VALUE = COMMON
        .comment("The range in chunks to read biomes for features that use",
                "distance-based biome testing.")
        .defineInRange("biomeRange", 2, 1, 20);

    @Overwrite
    public static final BooleanSupplier ENABLE_VANILLA_STONE_CLUSTERS =
        ENABLE_VANILLA_STONE_CLUSTERS_VALUE::get;

    @Overwrite
    public static final BooleanSupplier ENABLE_WATER_LAKES =
        ENABLE_WATER_LAKES_VALUE::get;

    @Overwrite
    public static final BooleanSupplier ENABLE_LAVA_LAKES =
        ENABLE_LAVA_LAKES_VALUE::get;

    @Overwrite
    public static final BooleanSupplier ENABLE_MINESHAFTS =
        ENABLE_MINESHAFTS_VALUE::get;

    @Overwrite
    public static final BooleanSupplier ENABLE_OTHER_GENERATORS =
        ENABLE_OTHER_GENERATORS_VALUE::get;

    @Overwrite
    public static final BooleanSupplier STRICT_PRESETS =
        STRICT_PRESETS_VALUE::get;

    @Overwrite
    public static final BooleanSupplier IGNORE_INVALID_PRESETS =
        IGNORE_INVALID_PRESETS_VALUE::get;

    @Overwrite
    public static final BooleanSupplier NETHER_GENERATE =
        NETHER_GENERATE_VALUE::get;

    @Overwrite
    public static final BooleanSupplier AUTO_FORMAT =
        AUTO_FORMAT_VALUE::get;

    @Overwrite
    public static final BooleanSupplier AUTO_GENERATE =
        AUTO_GENERATE_VALUE::get;

    @Overwrite
    public static final BooleanSupplier UPDATE_IMPORTS =
        UPDATE_IMPORTS_VALUE::get;

    @Overwrite
    public static final IntSupplier MAP_RANGE =
        MAP_RANGE_VALUE::get;

    @Overwrite
    public static final IntSupplier BIOME_RANGE =
        BIOME_RANGE_VALUE::get;
}