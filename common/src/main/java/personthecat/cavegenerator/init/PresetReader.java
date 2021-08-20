package personthecat.cavegenerator.init;

import lombok.extern.log4j.Log4j2;
import org.hjson.JsonObject;
import org.hjson.JsonValue;
import org.hjson.ParseException;
import personthecat.catlib.util.HjsonUtils;
import personthecat.cavegenerator.config.Cfg;
import personthecat.cavegenerator.presets.CavePreset;
import personthecat.cavegenerator.presets.lang.PresetExpander;
import personthecat.cavegenerator.presets.lang.SyntaxHelper;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.*;

import static java.util.Optional.empty;
import static personthecat.catlib.io.FileIO.listFiles;
import static personthecat.catlib.util.Shorthand.f;
import static personthecat.catlib.util.Shorthand.full;
import static personthecat.catlib.util.PathUtils.extension;
import static personthecat.catlib.util.PathUtils.noExtension;
import static personthecat.cavegenerator.exception.CaveSyntaxException.caveSyntax;

/**
 * This class is responsible for initiating all of the raw JSON-related operations
 * that need to take place before a preset can be translated into a concrete Java
 * object. This includes expanding variables in to regular JSON data and separating
 * inner-presets into their regular counterparts, using overrides from parent presets.
 */
@Log4j2
public class PresetReader {

    /** The key where inner presets are stored recursively. */
    public static final String INNER_KEY = "inner";

    /**
     * Reads a series of {@link CavePreset} objects from a directory.
     *
     * @throws ParseException if the preset contains a syntax error.
     * @param dir The directory to read presets from.
     * @return A map of filename -> GeneratorSettings.
     */
    public static Map<String, CavePreset> loadPresets(final File dir, final File imports) {
        final Map<File, JsonObject> jsons = loadJsons(dir);
        final Map<File, JsonObject> definitions = loadJsons(imports);
        // Detect a few common syntax errors.
        jsons.forEach(SyntaxHelper::check);
        definitions.forEach(SyntaxHelper::check);
        // Update all of the raw json objects.
        definitions.forEach((file, json) -> PresetCompat.updateImport(json, file)
            .expect("Updating import {}", file));
        jsons.forEach((file, json) -> PresetCompat.updatePreset(json, file)
            .expect("Updating preset {}", file));
        // Expand all of the variable definitions and imports.
        PresetExpander.expandAll(jsons, definitions);
        // Mark all values as unused so we can track them.
        jsons.forEach((file, json) -> json.setAllAccessed(false));
        // Convert to a map of filename -> POJO;
        final Map<String, CavePreset> settings = toSettings(jsons);
        // Check each preset and inform the user of potential mistakes.
        settings.forEach((name, cfg) ->
            new PresetTester(cfg, name, Cfg.STRICT_PRESETS.getAsBoolean()).run());
        return settings;
    }

    /** Loads and updates every JSON file in a directory. */
    private static Map<File, JsonObject> loadJsons(final File dir) {
        final Map<File, JsonObject> jsons = new HashMap<>();
        loadInto(jsons, dir);
        return jsons;
    }

    /** Recursively loads all presets in the directory. */
    private static void loadInto(final Map<File, JsonObject> jsons, final File dir) {
        for (final File file : listFiles(dir)) {
            if (file.isDirectory()) {
                loadInto(jsons, file);
            } else if (CaveInit.validExtension(file)) {
                log.info("Parsing preset file: {}", file.getName());
                jsons.put(file, loadJson(file).asObject());
            }
        }
    }

    /** Converts a map of (file -> json) to (filename -> POJO)  */
    private static Map<String, CavePreset> toSettings(final Map<File, JsonObject> jsons) {
        final Map<String, CavePreset> settings = new TreeMap<>();
        extractInner(jsons).forEach((name, json) -> {
            try {
                settings.put(name, CavePreset.from(json));
            } catch (RuntimeException e) {
                final String msg = f("Error reading {}: {}", name, e.getMessage());
                if (Cfg.IGNORE_INVALID_PRESETS.getAsBoolean()) {
                    log.error(msg);
                } else {
                    throw caveSyntax(msg);
                }
            }
        });
        return settings;
    }

    private static Map<String, JsonObject> extractInner(final Map<File, JsonObject> jsons) {
        final Map<String, JsonObject> extracted = new HashMap<>();
        jsons.forEach((file, json) -> extractRecursive(extracted, noExtension(file), json));
        return extracted;
    }

    private static void extractRecursive(final Map<String, JsonObject> data, final String name, final JsonObject parent) {
        final List<JsonObject> inner = HjsonUtils.getRegularObjects(parent, INNER_KEY);
        for (int i = 0; i < inner.size(); i++) {
            final JsonObject child = inner.get(i);
            final JsonObject clone = new JsonObject().addAll(parent).remove(INNER_KEY);
            for (JsonObject.Member member : child) {
                clone.set(member.getName(), member.getValue());
            }
            final String innerName = f("{}[{}]", name, i);
            extractRecursive(data, innerName, clone);
        }
        data.put(name, parent);
    }

    /**
     * Returns a JsonObject from the input file. Ensures that an error is handled
     * by any external callers.
     */
    public static Optional<JsonObject> getPresetJson(final File file) {
        try {
            return full(loadJson(file).asObject());
        } catch (RuntimeException ignored) {
            return empty();
        }
    }

    /** Parses the contents of @param file into a generic JsonValue. */
    private static JsonValue loadJson(final File file) {
        try {
            final Reader reader = new FileReader(file);
            if ("json".equals(extension(file))) {
                return JsonObject.readJSON(reader);
            } else {
                return JsonObject.readHjson(reader);
            }
        } catch (IOException ignored) {
            throw caveSyntax("Unable to load preset file {}", file.getName());
        } catch (ParseException e) {
            throw caveSyntax("Error reading {}", file.getName(), e);
        }
    }
}