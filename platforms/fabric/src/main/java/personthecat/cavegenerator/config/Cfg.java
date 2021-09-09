package personthecat.cavegenerator.config;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Comment;
import personthecat.catlib.config.HjsonConfigSerializer;
import personthecat.catlib.data.Lazy;
import personthecat.cavegenerator.util.Reference;
import personthecat.overwritevalidator.annotations.Inherit;
import personthecat.overwritevalidator.annotations.Overwrite;
import personthecat.overwritevalidator.annotations.OverwriteClass;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

@OverwriteClass
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
    public boolean strictPresets = false;

    @Comment(
        "When this field is set to true, PresetTester will skip over\n" +
        "any invalid presets and simply not load them. Make sure to\n" +
        "check your log to determine if any presets erred.")
    public boolean ignoreInvalidPresets = false;

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
    public static final Supplier<List<String>> DISABLED_CARVERS =
        () -> Arrays.asList(CONFIG.get().disabledCarvers);

    @Overwrite
    public static final Supplier<List<String>> DISABLED_FEATURES =
        () -> Arrays.asList(CONFIG.get().disabledFeatures);

    @Overwrite
    public static final Supplier<List<String>> DISABLED_STRUCTURES =
        () -> Arrays.asList(CONFIG.get().disabledStructures);

    @Overwrite
    public static final BooleanSupplier FALLBACK_CARVERS =
        () -> CONFIG.get().fallbackCarvers;

    @Overwrite
    public static final BooleanSupplier FALLBACK_FEATURES =
        () -> CONFIG.get().fallbackFeatures;

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

    @Overwrite
    public static final LongSupplier FALLBACK_CARVER_SEED =
        () -> CONFIG.get().fallbackCarverSeed;
}
