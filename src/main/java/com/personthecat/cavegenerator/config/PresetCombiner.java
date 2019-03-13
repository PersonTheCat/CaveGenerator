package com.personthecat.cavegenerator.config;

import com.personthecat.cavegenerator.CaveInit;
import org.apache.commons.lang3.ArrayUtils;
import org.hjson.JsonObject;
import org.hjson.JsonValue;

import java.io.File;
import java.util.regex.Pattern;

import static com.personthecat.cavegenerator.util.CommonMethods.*;
import static com.personthecat.cavegenerator.util.HjsonTools.*;

/** Used for merging JsonObject paths between json files. */
public class PresetCombiner {

    /** Responsible for loading all necessary data for merging JsonObjects. */
    public static void combine(String from, String to) {
        // Load all data.
        final String[] path = from.split(Pattern.quote("."));
        final File fileFrom = CaveInit.locatePreset(path[0])
            .orElseThrow(() -> runExF("Unable to locate a preset named \"%s\"", path[0]));
        final File fileTo = CaveInit.locatePreset(to)
            .orElseThrow(() -> runExF("Unable to locate a preset named \"%s\"", to));
        final JsonObject objFrom = PresetReader.getPresetJson(fileFrom)
            .orElseThrow(() -> runExF("The file named \"%s\" does not contain a valid json or hjson object.", fileFrom.getName()));
        final JsonObject objTo = PresetReader.getPresetJson(fileTo)
            .orElseThrow(() -> runExF("The file named \"%s\" does not contain a valid json or hjson object.", fileTo.getName()));
        // Create a backup and use the data.
        backup(fileTo);
        run(path, objFrom, objTo, fileTo);
    }

    /** Primary method for merging objects. */
    private static void run(String[] path, JsonObject from, JsonObject to, File fileTo) {
        StringBuilder currentPath = new StringBuilder("[root]");
        JsonObject fromCurrent = from;
        JsonObject toCurrent = to;

        for (String key : ArrayUtils.subarray(path, 1, path.length)) {
            final JsonValue fromValue = fromCurrent.get(key);
            final JsonValue toValue = toCurrent.get(key);

            currentPath.append('.');
            currentPath.append(key);
            if (fromValue == null) {
                throw runExF("The original json does not contain \"%s.\" Enter a valid path.", currentPath);
            }
            if (toValue == null || !fromValue.isObject()) {
                setOrAdd(toCurrent, key, fromValue);
                break;
            }
            fromCurrent = fromValue.asObject();
            toCurrent = toValue.asObject();
        }
        writeJson(to, fileTo).throwIfPresent();
    }
}