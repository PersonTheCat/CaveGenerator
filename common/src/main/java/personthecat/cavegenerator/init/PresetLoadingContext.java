package personthecat.cavegenerator.init;

import lombok.extern.log4j.Log4j2;
import org.hjson.JsonObject;
import org.jetbrains.annotations.Nullable;
import personthecat.catlib.data.JsonType;
import personthecat.catlib.data.SafeRegistry;
import personthecat.catlib.event.error.LibErrorContext;
import personthecat.catlib.exception.FormattedIOException;
import personthecat.catlib.exception.GenericFormattedException;
import personthecat.catlib.io.FileIO;
import personthecat.catlib.util.HjsonUtils;
import personthecat.cavegenerator.compat.PresetCompat;
import personthecat.cavegenerator.config.Cfg;
import personthecat.cavegenerator.io.JarFiles;
import personthecat.cavegenerator.io.ModFolders;
import personthecat.cavegenerator.presets.CavePreset;
import personthecat.cavegenerator.presets.lang.CaveLangExtension;
import personthecat.cavegenerator.presets.lang.SyntaxHelper;
import personthecat.cavegenerator.util.Reference;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import static personthecat.catlib.io.FileIO.mkdirsOrThrow;
import static personthecat.catlib.util.PathUtils.extension;
import static personthecat.catlib.util.PathUtils.noExtension;
import static personthecat.catlib.util.Shorthand.f;
import static personthecat.cavegenerator.io.ModFolders.*;

@Log4j2
public class PresetLoadingContext {

    private static final SafeRegistry<String, File> PRESETS =
        SafeRegistry.of(() -> collectPresets(ModFolders.PRESET_DIR)).canBeReset(true);

    private static final SafeRegistry<String, File> IMPORTS =
        SafeRegistry.of(() -> collectPresets(ModFolders.IMPORT_DIR)).canBeReset(true);

    private PresetLoadingContext() {}

    private static Map<String, File> collectPresets(final File dir) {
        final Map<String, File> files = new HashMap<>();
        for (final File file : FileIO.listFilesRecursive(dir)) {
            if (isPreset(file)) {
                files.put(noExtension(file), file);
            }
        }
        return files;
    }

    public static boolean isPreset(final File file) {
        return !JarFiles.isSpecialFile(file.getName()) && Reference.VALID_EXTENSIONS.contains(extension(file));
    }

    public static Map<String, CavePreset> loadPresets() {
        final Map<String, CavePreset> presets = new HashMap<>();
        final Context ctx = new Context();

        mkdirsOrThrow(PRESET_DIR, IMPORT_DIR);
        ctx.getPresets().forEach((name, json) -> {
            try {
                final CavePreset preset = CavePreset.from(name, json);
                onPresetLoaded(name, preset);
                presets.put(name, preset);
            } catch (final RuntimeException e) { // Todo: better formatted errors.
                LibErrorContext.error(Reference.MOD, new GenericFormattedException(e, "todo"));
            }
        });
        if (Cfg.AUTO_GENERATE.getAsBoolean()) {
            saveGenerated(presets);
        }
        return presets;
    }

    private static void onPresetLoaded(final String name, final CavePreset preset) {
        final String enabled = preset.enabled ? "enabled" : "disabled";
        log.info("Successfully loaded {}. It is {}.", name, enabled);
    }

    private static void saveGenerated(final Map<String, CavePreset> presets) {
        mkdirsOrThrow(ModFolders.GENERATED_DIR);
        presets.forEach((name, preset) -> {
            final File file = new File(GENERATED_DIR, name + ".cave");
            HjsonUtils.writeJson(preset.raw, file)
                .ifErr(e -> LibErrorContext.error(Reference.MOD, new GenericFormattedException(e, "todo")))
                .ifOk(v -> log.debug("Recorded the generated copy of {}", name));
        });
    }

    public static Map<String, File> getPresetFiles() {
        return PRESETS;
    }

    public static Map<String, File> getImportFiles() {
        return IMPORTS;
    }

    public static void reset() {
        SafeRegistry.resetAll(PRESETS, IMPORTS);
    }

    public static @Nullable JsonObject readJson(final File file) {
        try (final FileReader reader = new FileReader(file)) {
            if (JsonType.isJson(file)) {
                return JsonObject.readJSON(reader).asObject();
            }
            return JsonObject.readHjson(reader).asObject();
        } catch (final IOException e) { // Todo: better formatted errors.
            LibErrorContext.error(Reference.MOD, new FormattedIOException(file, e));
        } catch (final RuntimeException e) {
            LibErrorContext.error(Reference.MOD, new GenericFormattedException(e, "todo"));
        }
        return null;
    }

    private static class Context {
        final Map<File, JsonObject> rawPresets = loadFiles(PRESETS);
        final Map<File, JsonObject> importPresets = loadFiles(IMPORTS);

        static Map<File, JsonObject> loadFiles(final Map<String, File> files) {
            final Map<File, JsonObject> parsed = new HashMap<>();
            for (final File file : files.values()) {
                final JsonObject json = readJson(file);
                if (json != null) {
                    parsed.put(file, json);
                    log.debug("Successfully loaded {}.", file.getName());
                } else {
                    log.warn("Unable to load {}. Check the error menu for details.", file.getName());
                }
            }
            return parsed;
        }

        Map<String, JsonObject> getPresets() {
            if (Cfg.DETECT_EXTRA_TOKENS.getAsBoolean()) {
                this.runStringInspections();
            }
            if (Cfg.shouldUpdatePresets()) {
                this.runTransforms();
            }
            if (Cfg.CAVE_EL.getAsBoolean()) {
                CaveLangExtension.expandAll(this.rawPresets, this.importPresets);
            }
            final Map<String, JsonObject> extracted = extractInner(this.rawPresets);
            if (Cfg.DEEP_TRANSFORMS.getAsBoolean()) {
                extracted.forEach((file, json) -> PresetCompat.transformPresetOnly(json));
            }
            extracted.forEach((name, json) -> json.setAllAccessed(false));
            return extracted;
        }

        void runStringInspections() {
            inspect(this.rawPresets);
            inspect(this.importPresets);
        }

        static void inspect(final Map<File, JsonObject> presets) {
            for (final Map.Entry<File, JsonObject> preset : new HashSet<>(presets.entrySet())) {
                try {
                    SyntaxHelper.check(preset.getKey(), preset.getValue());
                } catch (final RuntimeException e) { // Todo: formatted error
                    log.warn("Extraneous tokens found in {}. It will be ignored.", preset.getKey().getName());
                    LibErrorContext.error(Reference.MOD, new GenericFormattedException(e, "todo"));
                }
            }
        }

        void runTransforms() {
            this.rawPresets.forEach(PresetCompat::transformPreset);
            this.importPresets.forEach(PresetCompat::transformImport);
        }

        static Map<String, JsonObject> extractInner(final Map<File, JsonObject> presets) {
            final Map<String, JsonObject> extracted = new HashMap<>();
            presets.forEach((file, json) -> extractRecursive(extracted, noExtension(file), json));
            return extracted;
        }

        static void extractRecursive(final Map<String, JsonObject> data, final String name, final JsonObject parent) {
            final List<JsonObject> inner = HjsonUtils.getRegularObjects(parent, CavePreset.INNER_KEY);
            for (int i = 0; i < inner.size(); i++) {
                final JsonObject child = inner.get(i);
                final JsonObject clone = new JsonObject().addAll(parent).remove(CavePreset.INNER_KEY);
                for (JsonObject.Member member : child) {
                    clone.set(member.getName(), member.getValue());
                }
                final String innerName = f("{}[{}]", name, i);
                extractRecursive(data, innerName, clone);
            }
            data.put(name, parent);
        }
    }
}
