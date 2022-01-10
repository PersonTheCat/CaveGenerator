package personthecat.cavegenerator.config;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Comment;
import personthecat.catlib.config.HjsonConfigSerializer;
import personthecat.catlib.data.Lazy;
import personthecat.cavegenerator.util.Reference;
import personthecat.overwritevalidator.annotations.Inherit;
import personthecat.overwritevalidator.annotations.InheritMissingMembers;
import personthecat.overwritevalidator.annotations.Overwrite;
import personthecat.overwritevalidator.annotations.OverwriteClass;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@OverwriteClass
@InheritMissingMembers
@Config(name = Reference.MOD_ID)
public class Cfg implements ConfigData {

    @Inherit
    private static final List<String> DEFAULT_DISABLED_FEATURES = Collections.emptyList();

    @Comment(
        "To use this feature, toggle enableOtherGenerators and\n" +
        "list out the registry names of any world carvers you wish\n" +
        "to manually disable.\n" +
        "For example, `cave` or `minecraft:underwater_canyon`.\n" +
        "For a list of all carvers, run `/cave debug carvers`.")
    public String[] disabledCarvers = {};

    @Comment(
        "A list of all feature types OR configured features being\n" +
        "globally disabled by the mod.\n" +
        "For example, `ore` or `minecraft:ore_coal`.\n" +
        "For a list of all features, run `/cave debug features`.")
    public String[] disabledFeatures = DEFAULT_DISABLED_FEATURES.toArray(new String[0]);

    @Comment(
        "A list of all structure features being globally disabled\n" +
        "by the mod." +
        "For example, `minecraft:mineshaft`.\n" +
        "For a list of all features, run `/cave debug structures`.")
    public String[] disabledStructures = {};

    @Comment(
        "Whether to apply preset transforms after expressions are\n" +
        "evaluated. These changes will not be saved. This is essentially\n" +
        "a backwards compatibility setting which can be disabled for\n" +
        "performance purposes.")
    public boolean deepTransforms = true;

    @Comment(
        "Whether to enable the fallback generator compatibility layer\n" +
        "for support with mods that use custom chunk generators.")
    public boolean fallbackCarvers = false;

    @Comment(
        "Whether to enable the fallback feature compatibility layer\n" +
        "for support with mods that use custom chunk generators.")
    public boolean fallbackFeatures = false;

    @Comment(
        "Whether this mod will attempt to run simultaneously\n" +
        "with one other cave generation mod, such as Worley's\n" +
        "Caves or Yung's Better Caves.")
    public boolean enableOtherGenerators = false;

    @Comment(
        "When this field is set to true, PresetTester is allowed to\n" +
        "crash the game when more serious errors are detected. Users\n" +
        "who are serious about creating cleaner and more efficient\n" +
        "presets should consider enabling this field to make sure that\n" +
        "nothing slips by.")
    public boolean strictPresets = true;

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
        "The range in chunks to read biomes for features that use\n" +
        "distance-based biome testing.")
    public int biomeRange = 2;

    @Comment(
        "The seed to for the fallback generator when this feature\n" +
        "is enabled.")
    public long fallbackCarverSeed = 24L;

    @Overwrite
    public static void register() {
        AutoConfig.register(Cfg.class, HjsonConfigSerializer::new);
    }

    private static final Lazy<Cfg> CONFIG =
        Lazy.of(() -> AutoConfig.getConfigHolder(Cfg.class).getConfig());

    @Overwrite
    public static List<String> disabledCarvers() {
        return Arrays.asList(CONFIG.get().disabledCarvers);
    }

    @Overwrite
    public static List<String> disabledFeatures() {
        return Arrays.asList(CONFIG.get().disabledFeatures);
    }

    @Overwrite
    public static List<String> disabledStructures() {
        return Arrays.asList(CONFIG.get().disabledStructures);
    }

    @Overwrite
    public static boolean deepTransforms() {
        return CONFIG.get().deepTransforms;
    }

    @Overwrite
    public static boolean fallbackCarvers() {
        return CONFIG.get().fallbackCarvers;
    }

    @Overwrite
    public static boolean fallbackFeatures() {
        return CONFIG.get().fallbackFeatures;
    }

    @Overwrite
    public static boolean enableOtherGenerators() {
        return CONFIG.get().enableOtherGenerators;
    }

    @Overwrite
    public static boolean strictPresets() {
        return CONFIG.get().strictPresets;
    }

    @Overwrite
    public static boolean autoFormat() {
        return CONFIG.get().autoFormat;
    }

    @Overwrite
    public static boolean autoGenerate() {
        return CONFIG.get().autoGenerate;
    }

    @Overwrite
    public static boolean updateImports() {
        return CONFIG.get().updateImports;
    }

    @Overwrite
    public static int mapRange() {
        return CONFIG.get().mapRange;
    }

    @Overwrite
    public static int biomeRange() {
        return CONFIG.get().biomeRange;
    }

    @Overwrite
    public static long fallbackCarverSeed() {
        return CONFIG.get().fallbackCarverSeed;
    }
}
