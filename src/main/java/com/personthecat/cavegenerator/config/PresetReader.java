package com.personthecat.cavegenerator.config;

import lombok.extern.log4j.Log4j2;
import org.hjson.JsonObject;
import org.hjson.JsonValue;
import org.hjson.ParseException;

import java.io.*;
import java.util.*;

import static com.personthecat.cavegenerator.util.CommonMethods.empty;
import static com.personthecat.cavegenerator.util.CommonMethods.extension;
import static com.personthecat.cavegenerator.util.CommonMethods.f;
import static com.personthecat.cavegenerator.util.CommonMethods.full;
import static com.personthecat.cavegenerator.util.CommonMethods.noExtension;
import static com.personthecat.cavegenerator.util.CommonMethods.runEx;
import static com.personthecat.cavegenerator.util.CommonMethods.runExF;
import static com.personthecat.cavegenerator.io.SafeFileIO.listFiles;

/**
 * This class is responsible for initiating all of the raw JSON-related operations
 * that need to take place before a preset can be translated into a concrete Java
 * object. This includes expanding variables in to regular JSON data and separating
 * inner-presets into their regular counterparts, using overrides from parent presets.
 */
@Log4j2
public class PresetReader {

    /**
     * Reads a series of {@link CavePreset} objects from a directory.
     *
     * @throws ParseException if the preset contains a syntax error.
     * @param dir The directory to read presets from.
     * @return A map of filename -> GeneratorSettings.
     */
    public static Map<String, CavePreset> getPresets(File dir, File imports) {
        final Map<File, JsonObject> jsons = loadJsons(dir);
        final Map<File, JsonObject> definitions = loadJsons(imports);
        // Detect a few common syntax errors.
        jsons.forEach(SyntaxHelper::check);
        definitions.forEach(SyntaxHelper::check);
        // Update all of the raw json objects.
        jsons.forEach((file, json) -> PresetCompat.update(json, file)
            .expectF("Error updating {}", file));
        // Expand all of the variable definitions and imports.
        PresetExpander.expandAll(jsons, definitions);
        // Mark all values as unused so we can track them.
        jsons.forEach((file, json) -> json.setAllAccessed(false));
        // Convert to a map of filename -> POJO;
        final Map<String, CavePreset> settings = toSettings(jsons);
        // Check each preset and inform the user of potential mistakes.
        settings.forEach((name, cfg) ->
            new PresetTester(cfg, name, ConfigFile.strictPresets).run());
        return settings;
    }

    /** Loads and updates every JSON file in a directory. */
    private static Map<File, JsonObject> loadJsons(File dir) {
        final Map<File, JsonObject> jsons = new HashMap<>();
        for (File file : listFiles(dir).orElse(new File[0])) {
            log.info("Parsing preset file: {}", file.getName());
            jsons.put(file, loadJson(file).asObject());
        }
        return jsons;
    }

    /** Converts a map of (file -> json) to (filename -> POJO)  */
    private static Map<String, CavePreset> toSettings(Map<File, JsonObject> jsons) {
        final Map<String, CavePreset> settings = new HashMap<>();
        for (Map.Entry<File, JsonObject> entry : jsons.entrySet()) {
            settings.put(noExtension(entry.getKey()), CavePreset.from(entry.getValue()));
        }
        return settings;
    }

    /**
     * Returns a JsonObject from the input file. Ensures that an error is handled
     * by any external callers.
     */
    public static Optional<JsonObject> getPresetJson(File file) {
        try {
            return full(loadJson(file).asObject());
        } catch (RuntimeException ignored) {
            return empty();
        }
    }

    /** Parses the contents of @param file into a generic JsonValue. */
    private static JsonValue loadJson(File file) {
        try {
            Reader reader = new FileReader(file);
            String extension = extension(file);
            if (extension.equals("json")) {
                return JsonObject.readJSON(reader);
            } else {
                return JsonObject.readHjson(reader);
            }
        } catch (IOException ignored) {
            throw runExF("Unable to load preset file {}", file.getName());
        } catch (ParseException e) {
            throw runEx(f("Error reading {}", file.getName()), e);
        }
    }
}