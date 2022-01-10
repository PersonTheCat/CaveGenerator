package personthecat.cavegenerator.compat;

import lombok.extern.log4j.Log4j2;
import org.hjson.JsonObject;
import org.hjson.JsonValue;
import personthecat.catlib.util.HjsonUtils;
import personthecat.catlib.util.JsonTransformer;
import personthecat.catlib.util.JsonTransformer.ObjectResolver;
import personthecat.cavegenerator.compat.transformer.CaveTransformers;
import personthecat.cavegenerator.compat.transformer.ImportTransformers;
import personthecat.cavegenerator.config.Cfg;
import personthecat.cavegenerator.presets.CavePreset;
import personthecat.cavegenerator.presets.lang.CaveLangExtension;

import java.io.File;

@Log4j2
public class PresetCompat {

    private static final ObjectResolver PRESET_TRANSFORMER =
        JsonTransformer.root()
            .include(CaveTransformers.ROOT_TRANSFORMER)
            .include(CaveTransformers.CAVERN_TRANSFORMER)
            .include(CaveTransformers.RAVINE_TRANSFORMER)
            .include(CaveTransformers.ROOM_TRANSFORMER)
            .include(CaveTransformers.TUNNEL_TRANSFORMER)
            .include(CaveTransformers.CLUSTER_TRANSFORMER)
            .include(CaveTransformers.LAYER_TRANSFORMER)
            .include(CaveTransformers.STALACTITE_TRANSFORMER)
            .include(CaveTransformers.PILLAR_TRANSFORMER)
            .include(CaveTransformers.STRUCTURE_TRANSFORMER)
            .include(CaveTransformers.BURROW_TRANSFORMER)
            .include(CaveTransformers.RECURSIVE_TRANSFORMER)
            .freeze();

    private static final ObjectResolver DEFAULTS_TRANSFORMER =
        ImportTransformers.DEFAULTS_TRANSFORMER;

    public static void transformPreset(final File file, final JsonObject preset) {
        final boolean autoFormat = Cfg.AUTO_FORMAT.getAsBoolean();
        final int hash = autoFormat ? 0 : preset.hashCode();

        transformPresetOnly(preset);

        if (autoFormat || hash != preset.hashCode()) {
            HjsonUtils.writeJson(preset, file)
                .ifErr(e -> log.warn("Unable to record transformations. Ignoring... ({})", file.getName()))
                .ifOk(t -> log.debug("Cave preset updated successfully! ({})", file.getName()));
        } else {
            log.debug("Nothing to update in {}. It will not be saved.", file.getName());
        }
    }

    public static void transformPresetOnly(final JsonObject preset) {
        PRESET_TRANSFORMER.updateAll(preset);

        for (final JsonObject inner : HjsonUtils.getRegularObjects(preset, CavePreset.INNER_KEY)) {
            transformPresetOnly(inner.asObject());
        }
    }

    public static void transformImport(final File file, final JsonObject preset) {
        final boolean autoFormat = Cfg.AUTO_FORMAT.getAsBoolean();
        final int hash = autoFormat ? 0 : preset.hashCode();

        if (CaveLangExtension.isLegacyDefaults(file, preset)) {
            DEFAULTS_TRANSFORMER.updateAll(preset);
        } else {
            transformPresetOnly(preset);
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
