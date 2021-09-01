package personthecat.cavegenerator.config;

import personthecat.catlib.exception.MissingOverrideException;
import personthecat.overwritevalidator.annotations.OverwriteTarget;
import personthecat.overwritevalidator.annotations.PlatformMustOverwrite;

import java.util.Collections;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

@OverwriteTarget
public class Cfg {
    public static final Supplier<List<String>> DISABLED_CARVERS = Collections::emptyList;
    public static final Supplier<List<String>> DISABLED_FEATURES = Collections::emptyList;
    public static final Supplier<List<String>> DISABLED_STRUCTURES = Collections::emptyList;
    public static final BooleanSupplier ENABLE_OTHER_GENERATORS = () -> false;
    public static final BooleanSupplier STRICT_PRESETS = () -> false;
    public static final BooleanSupplier IGNORE_INVALID_PRESETS = () -> false;
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
