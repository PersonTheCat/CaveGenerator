package personthecat.cavegenerator.compat;

import lombok.extern.log4j.Log4j2;
import org.hjson.JsonObject;
import personthecat.catlib.util.HjsonUtils;
import personthecat.catlib.util.JsonTransformer;
import personthecat.catlib.util.JsonTransformer.ObjectResolver;
import personthecat.cavegenerator.config.Cfg;
import personthecat.cavegenerator.presets.lang.CaveLangExtension;

import java.io.File;

@Log4j2
public class PresetCompat {

    private static final ObjectResolver PRESET_TRANSFORMER =
        JsonTransformer.root()
            .freeze();

    private static final ObjectResolver IMPORT_TRANSFORMER =
        JsonTransformer.root()
            .freeze();

    private static final ObjectResolver DEFAULTS_TRANSFORMER =
        JsonTransformer.root()
            .freeze();

    public static void transformPreset(final File file, final JsonObject preset) {
        final boolean autoFormat = Cfg.AUTO_FORMAT.getAsBoolean();
        final int hash = autoFormat ? 0 : preset.hashCode();

        PRESET_TRANSFORMER.updateAll(preset);

        if (autoFormat || hash != preset.hashCode()) {
            HjsonUtils.writeJson(preset, file)
                .ifErr(e -> log.warn("Unable to record transformations. Ignoring... ({})", file.getName()))
                .ifOk(t -> log.debug("Cave preset updated successfully! ({})", file.getName()));
        } else {
            log.debug("Nothing to update in {}. It will not be saved.", file.getName());
        }
    }

    public static void transformPreset(final JsonObject preset) {
        PRESET_TRANSFORMER.updateAll(preset);
    }

    public static void transformImport(final File file, final JsonObject preset) {
        final boolean autoFormat = Cfg.AUTO_FORMAT.getAsBoolean();
        final int hash = autoFormat ? 0 : preset.hashCode();

        if (CaveLangExtension.isLegacyDefaults(file, preset)) {
            DEFAULTS_TRANSFORMER.updateAll(preset);
        } else {
            IMPORT_TRANSFORMER.updateAll(preset);
        }

        if (autoFormat || hash != preset.hashCode()) {
            HjsonUtils.writeJson(preset, file)
                .ifErr(e -> log.warn("Unable to record transformations. Ignoring... ({})", file.getName()))
                .ifOk(t -> log.debug("Import preset updated successfully! ({})", file.getName()));
        } else {
            log.debug("Nothing to update in {}. It will not be saved.", file.getName());
        }
    }
}
