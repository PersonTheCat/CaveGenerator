package personthecat.cavegenerator.config;

import personthecat.catlib.exception.MissingOverrideException;
import personthecat.overwritevalidator.annotations.OverwriteTarget;
import personthecat.overwritevalidator.annotations.PlatformMustOverwrite;

import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;

@OverwriteTarget
public class Cfg {
    public static final BooleanSupplier ENABLE_VANILLA_STONE_CLUSTERS = () -> false;
    public static final BooleanSupplier ENABLE_WATER_LAKES = () -> false;
    public static final BooleanSupplier ENABLE_LAVA_LAKES = () -> false;
    public static final BooleanSupplier ENABLE_MINESHAFTS = () -> true;
    public static final BooleanSupplier ENABLE_OTHER_GENERATORS = () -> false;
    public static final BooleanSupplier STRICT_PRESETS = () -> false;
    public static final BooleanSupplier IGNORE_INVALID_PRESETS = () -> false;
    public static final BooleanSupplier NETHER_GENERATE = () -> false;
    public static final BooleanSupplier AUTO_FORMAT = () -> true;
    public static final BooleanSupplier AUTO_GENERATE = () -> false;
    public static final BooleanSupplier UPDATE_IMPORTS = () -> true;
    public static final IntSupplier MAP_RANGE = () -> 8;
    public static final IntSupplier BIOME_RANGE = () -> 2;

    @PlatformMustOverwrite
    public static void register() {
        throw new MissingOverrideException();
    }
}
