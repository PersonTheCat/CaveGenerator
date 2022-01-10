package personthecat.cavegenerator.config;

import personthecat.catlib.exception.MissingOverrideException;
import personthecat.overwritevalidator.annotations.OverwriteTarget;
import personthecat.overwritevalidator.annotations.PlatformMustOverwrite;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@OverwriteTarget
public class Cfg {

    private static final List<String> DEFAULT_DISABLED_FEATURES =
        Arrays.asList("spring_feature", "lake", "ore_dirt", "ore_gravel", "ore_andesite", "ore_diorite", "ore_granite");

    @PlatformMustOverwrite
    public static void register() {
        throw new MissingOverrideException();
    }

    // Todo: smartUpdatePresets with version tracker
    public static boolean shouldUpdatePresets() {
        final PresetUpdatePreference update = updatePreference();
        return update == PresetUpdatePreference.ALWAYS
            || (update == PresetUpdatePreference.MOD_UPDATED /* && mod updated */);
    }

    public static List<String> disabledCarvers() {
        return Collections.emptyList();
    };

    public static List<String> disabledFeatures() {
        return DEFAULT_DISABLED_FEATURES;
    }

    public static List<String> disabledStructures() {
        return Collections.emptyList();
    }

    public static PresetUpdatePreference updatePreference() {
        return PresetUpdatePreference.ALWAYS;
    }

    public static boolean deepTransforms() {
        return true;
    }

    public static boolean fallbackCarvers() {
        return false;
    }

    public static boolean fallbackFeatures() {
        return false;
    }

    public static boolean enableOtherGenerators() {
        return false;
    }

    public static boolean detectExtraTokens() {
        return true;
    }

    public static boolean caveEL() {
        return true;
    }

    public static boolean strictPresets() {
        return true;
    }

    public static boolean autoFormat() {
        return true;
    }

    public static boolean autoGenerate() {
        return false;
    }

    public static boolean updateImports() {
        return true;
    }

    public static int mapRange() {
        return 8;
    }

    public static int biomeRange() {
        return 2;
    }

    public static long fallbackCarverSeed() {
        return 24L;
    }
}
