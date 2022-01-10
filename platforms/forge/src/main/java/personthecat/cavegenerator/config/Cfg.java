package personthecat.cavegenerator.config;

import lombok.experimental.UtilityClass;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;
import net.minecraftforge.common.ForgeConfigSpec.LongValue;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import personthecat.catlib.config.CustomModConfig;
import personthecat.catlib.config.HjsonFileConfig;
import personthecat.catlib.util.McUtils;
import personthecat.cavegenerator.util.Reference;
import personthecat.overwritevalidator.annotations.Inherit;
import personthecat.overwritevalidator.annotations.InheritMissingMembers;
import personthecat.overwritevalidator.annotations.Overwrite;
import personthecat.overwritevalidator.annotations.OverwriteClass;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

@UtilityClass
@OverwriteClass
@InheritMissingMembers
public class Cfg {

    private static final ForgeConfigSpec.Builder COMMON = new ForgeConfigSpec.Builder();
    private static final String FILENAME = McUtils.getConfigDir() + "/" + Reference.MOD_ID;
    private static final HjsonFileConfig COMMON_CFG = new HjsonFileConfig(FILENAME + ".hjson");

    @Overwrite
    public static void register() {
        final ModContainer ctx = ModLoadingContext.get().getActiveContainer();
        ctx.addConfig(new CustomModConfig(ModConfig.Type.COMMON, COMMON.build(), ctx, COMMON_CFG));
    }

    @Inherit
    private static final List<String> DEFAULT_DISABLED_FEATURES = Collections.emptyList();

    private static final ConfigValue<List<String>> DISABLED_CARVERS = COMMON
        .comment("To use this feature, toggle enableOtherGenerators and",
                "list out the registry names of any world carvers you wish",
                "to manually disable.",
                "For example, `cave` or `minecraft:underwater_canyon`.",
                "For a list of all carvers, run `/cave debug carvers`.")
        .define("disabledCarvers", Collections.emptyList(), Objects::nonNull);

    private static final ConfigValue<List<String>> DISABLED_FEATURES = COMMON
        .comment("A list of all feature types OR configured features being",
                "globally disabled by the mod.",
                "For example, `ore` or `minecraft:ore_coal`.",
                "For a list of all features, run `/cave debug features`.")
        .define("disabledFeatures", DEFAULT_DISABLED_FEATURES, Objects::nonNull);

    private static final ConfigValue<List<String>> DISABLED_STRUCTURES = COMMON
        .comment("A list of all structure features being globally disabled",
                "by the mod.",
                "For example, `minecraft:mineshaft`.",
                "For a list of all features, run `/cave debug structures`.")
        .define("disabledStructures", Collections.emptyList(), Objects::nonNull);

    private static final BooleanValue FALLBACK_CARVERS = COMMON
        .comment("Whether to enable the fallback generator compatibility layer",
                "for support with mods that use custom chunk generators.")
        .define("fallbackCarvers", false);

    private static final BooleanValue FALLBACK_FEATURES = COMMON
        .comment("Whether to enable the fallback feature compatibility layer",
                "for support with mods that use custom chunk generators.")
        .define("fallbackFeatures", false);

    private static final BooleanValue ENABLE_OTHER_GENERATORS = COMMON
        .comment("Whether this mod will attempt to run simultaneously",
                "with one other cave generation mod, such as Worley's",
                "Caves or Yung's Better Caves.")
        .define("enableOtherGenerators", false);

    private static final BooleanValue STRICT_PRESETS = COMMON
        .comment("When this field is set to true, PresetTester is allowed to",
                "crash the game when more serious errors are detected. Users",
                "who are serious about creating cleaner and more efficient",
                "presets should consider enabling this field to make sure that",
                "nothing slips by.")
        .define("strictPresets", false);

    private static final BooleanValue AUTO_FORMAT = COMMON
        .comment("Whether to automatically format your preset files. They will",
                "still be reformatted if values are updated.")
        .define("autoFormat", true);

    private static final BooleanValue AUTO_GENERATE = COMMON
        .comment("Whether to automatically generate preset files inside of",
                "cavegenerator/generated. This will help you see how your",
                "variables are getting expanded every time you reload your",
                "presets.")
        .define("autoGenerate", false);

    private static final BooleanValue UPDATE_IMPORTS = COMMON
        .comment("Whether to automatically update import files, as much",
                "as possible. Note that compatibility updates will still",
                "occur.")
        .define("updateImports", true);

    private static final IntValue MAP_RANGE = COMMON
        .comment("The search range for tunnel and ravine features.")
        .defineInRange("mapRange", 8, 1, 20);

    private static final IntValue BIOME_RANGE = COMMON
        .comment("The range in chunks to read biomes for features that use",
                "distance-based biome testing.")
        .defineInRange("biomeRange", 2, 1, 20);

    private static final LongValue FALLBACK_CARVER_SEED = COMMON
        .comment("The seed to for the fallback generator when this feature",
                "is enabled.")
        .defineInRange("fallbackCarverSeed", 24L, Long.MIN_VALUE, Long.MAX_VALUE);

    @Overwrite
    public static List<String> disabledCarvers() {
        return DISABLED_CARVERS.get();
    }

    @Overwrite
    public static List<String> disabledFeatures() {
        return DISABLED_FEATURES.get();
    }

    @Overwrite
    public static List<String> disabledStructures() {
        return DISABLED_STRUCTURES.get();
    }

    @Overwrite
    public static boolean fallbackCarvers() {
        return FALLBACK_CARVERS.get();
    }

    @Overwrite
    public static boolean fallbackFeatures() {
        return FALLBACK_FEATURES.get();
    }

    @Overwrite
    public static boolean enableOtherGenerators() {
        return ENABLE_OTHER_GENERATORS.get();
    }

    @Overwrite
    public static boolean strictPresets() {
        return STRICT_PRESETS.get();
    }

    @Overwrite
    public static boolean autoFormat() {
        return AUTO_FORMAT.get();
    }

    @Overwrite
    public static boolean autoGenerate() {
        return AUTO_GENERATE.get();
    }

    @Overwrite
    public static boolean updateImports() {
        return UPDATE_IMPORTS.get();
    }

    @Overwrite
    public static int mapRange() {
        return MAP_RANGE.get();
    }

    @Overwrite
    public static int biomeRange() {
        return BIOME_RANGE.get();
    }

    @Overwrite
    public static long fallbackCarverSeed() {
        return FALLBACK_CARVER_SEED.get();
    }
}
