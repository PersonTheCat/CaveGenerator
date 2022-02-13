package personthecat.cavegenerator.init;

import lombok.extern.log4j.Log4j2;
import org.hjson.JsonObject;
import org.hjson.JsonValue;
import org.hjson.ParseException;
import org.jetbrains.annotations.Nullable;
import personthecat.catlib.data.SafeRegistry;
import personthecat.catlib.event.error.LibErrorContext;
import personthecat.catlib.exception.GenericFormattedException;
import personthecat.catlib.io.FileIO;
import personthecat.catlib.util.HjsonUtils;
import personthecat.cavegenerator.compat.PresetCompat;
import personthecat.cavegenerator.config.Cfg;
import personthecat.cavegenerator.exception.CorruptPresetException;
import personthecat.cavegenerator.exception.ExtraneousTokensException;
import personthecat.cavegenerator.exception.PresetSyntaxException;
import personthecat.cavegenerator.io.JarFiles;
import personthecat.cavegenerator.io.ModFolders;
import personthecat.cavegenerator.presets.CavePreset;
import personthecat.cavegenerator.presets.CaveOutput;
import personthecat.cavegenerator.presets.lang.CaveLangExtension;
import personthecat.cavegenerator.presets.lang.SyntaxHelper;
import personthecat.cavegenerator.util.Reference;
import personthecat.fresult.Result;

import java.io.File;
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
        ctx.getPresets().forEach((name, output) ->
            CavePreset.from(name, output).ifPresent(preset -> {
                onPresetLoaded(name);
                presets.put(name, preset);
            })
        );
        if (Cfg.autoGenerate()) {
            saveGenerated(presets);
        }
        return presets;
    }

    private static void onPresetLoaded(final String name) {
        log.info("Successfully loaded {}. It is enabled.", name);
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

    public static void reset() {
        SafeRegistry.resetAll(PRESETS, IMPORTS);
    }

    public static @Nullable JsonObject readJson(final File file) {
        final String contents = getContents(file);
        if (contents == null) {
            return null;
        }
        return readContents(file, contents);
    }

    private static @Nullable String getContents(final File file) {
        final Result<String, IOException> result = FileIO.readFile(file);
        final Optional<IOException> error = result.getErr();
        if (error.isPresent()) {
            LibErrorContext.error(Reference.MOD, new CorruptPresetException(file.getName(), error.get()));
            return null;
        }
        return result.unwrap();
    }

    private static @Nullable JsonObject readContents(final File file, final String contents) {
        final Result<JsonValue, ParseException> result = HjsonUtils.readValue(contents);
        final Optional<ParseException> error = result.getErr();
        if (error.isPresent()) {
            LibErrorContext.error(Reference.MOD, new PresetSyntaxException(file.getName(), contents, error.get()));
            return null;
        }
        return result.unwrap().asObject();
    }

    private static class Context {
        final Map<File, JsonObject> cavePresets = loadFiles(PRESETS);
        final Map<File, JsonObject> importPresets = loadFiles(IMPORTS);
        final Map<String, JsonObject> rawPresets = clonePresets(cavePresets);

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

        static Map<String, JsonObject> clonePresets(final Map<File, JsonObject> presets) {
            final Map<String, JsonObject> clones = new HashMap<>();
            for (final Map.Entry<File, JsonObject> preset : presets.entrySet()) {
                if (CavePreset.isEnabled(preset.getValue())) {
                    clones.put(noExtension(preset.getKey()), (JsonObject) preset.getValue().deepCopy());
                }
            }
            return clones;
        }

        Map<String, CaveOutput> getPresets() {
            if (Cfg.detectExtraTokens()) {
                this.runStringInspections();
            }
            if (Cfg.shouldUpdatePresets()) {
                this.cavePresets.forEach(PresetCompat::transformPreset);
            }
            if (Cfg.updateImports()) {
                this.importPresets.forEach(PresetCompat::transformImport);
            }
            if (Cfg.caveEL()) {
                CaveLangExtension.expandAll(this.cavePresets, this.importPresets);
            }
            final Map<String, CaveOutput> extracted = extractAll(this.cavePresets);
            if (Cfg.deepTransforms()) {
                extracted.forEach((file, output) -> PresetCompat.transformPresetOnly(output.generated));
            }
            extracted.forEach((name, output) -> output.generated.setAllAccessed(false));
            return extracted;
        }

        void runStringInspections() {
            inspect(this.cavePresets);
            inspect(this.importPresets);
        }

        static void inspect(final Map<File, JsonObject> presets) {
            presets.entrySet().removeIf(preset -> {
                final List<String> messages = SyntaxHelper.getExtraneousTokens(preset.getValue());
                if (!messages.isEmpty()) {
                    LibErrorContext.error(Reference.MOD, new ExtraneousTokensException(
                        preset.getKey().getName(), messages, preset.getValue()));
                    return true;
                }
                return false;
            });
        }

        Map<String, CaveOutput> extractAll(final Map<File, JsonObject> presets) {
            final Map<String, CaveOutput> extracted = new HashMap<>();
            presets.forEach((file, json) -> {
                final String name = noExtension(file);
                this.extractRecursive(extracted, name, json, json, this.getUserPreset(name));
            });
            return extracted;
        }

        JsonObject getUserPreset(final String name) {
            final int bracket = name.lastIndexOf('[');
            final String actualName = bracket > 0 ? name.substring(0, bracket) : name;
            final JsonObject get = this.rawPresets.get(actualName);
            return get != null ? get : new JsonObject();
        }

        void extractRecursive(Map<String, CaveOutput> data, String name, JsonObject parent, JsonObject root, JsonObject user) {
            final List<JsonObject> inner = HjsonUtils.getRegularObjects(parent, CavePreset.INNER_KEY);
            for (int i = 0; i < inner.size(); i++) {
                final JsonObject child = inner.get(i);
                final JsonObject clone = ((JsonObject) parent.shallowCopy()).remove(CavePreset.INNER_KEY);
                for (JsonObject.Member member : child) {
                    clone.set(member.getName(), member.getValue());
                }
                final String innerName = f("{}[{}]", name, i);
                extractRecursive(data, innerName, clone, root, user);
            }
            data.put(name, new CaveOutput(root, parent, user));
        }
    }
}
