package com.personthecat.cavegenerator.config;

import org.hjson.JsonArray;
import org.hjson.JsonObject;
import org.hjson.JsonValue;

import java.io.File;
import java.util.*;

import static com.personthecat.cavegenerator.util.HjsonTools.*;
import static com.personthecat.cavegenerator.util.CommonMethods.*;

public class PresetExpander {

    /**
     * Expands all of the variable definitions and imports into concrete data.
     *
     * @param presets All of the main preset files mapped to their parent files.
     * @param definitions A map of all JSON objects in the imports folder.
     */
    public static void expandAll(Map<File, JsonObject> presets, Map<File, JsonObject> definitions) {
        // Expand any variables used inside of each import.
        definitions.forEach((f, json) -> expand(json));
        // Copy all of the imports directly into each json.
        presets.forEach((f, json) -> copyImports(definitions, json));
        // Expand the variables now inside of each json.
        presets.forEach((f, json) -> expandVariables(json));
        // Delete all of the now unneeded imports and variables.
        presets.forEach((f, json) -> deleteUnused(json));
    }

    /**
     * Copies all of the imports declared in the current preset directly into it.
     * They will be stored inside of <code>root.variables</code>
     *
     * @param definitions A map of all JSON objects in the imports folder.
     * @param json The current JSON object being copied into.
     */
    private static void copyImports(Map<File, JsonObject> definitions, JsonObject json) {
        final Set<JsonObject> imports = new HashSet<>();
        // Copy by reference all of the required jsons into a set.
        getArray(json, "imports").ifPresent(arr -> {
            for (JsonValue value : arr) {
                if (!value.isString()) {
                    throw runExF("Invalid data type in imports: {}", value.toString());
                }
                imports.add(getRequiredImport(definitions, value.asString()));
            }
        });
        // copy the contents of each import into variables.
        final JsonObject variables = getObjectOrNew(json, "variables");
        for (JsonObject o : imports) {
            variables.addAll(o);
        }
    }

    /**
     * Gets a JSON from the map by filename. A variable name may optionally be appended
     * to the filename to indicate a specific variable to include from this json.
     *
     * For example, you can write <code>defaults.cave::VANILLA</code> to import only
     * the variable <code>VANILLA</code> from <code>defaults.cave</code>.
     *
     * Todo: Make it to where you can use a path instead.
     *
     * @throws RuntimeException If the import does not exist.
     * @throws NullPointerException If filename is null.
     *
     * @param definitions A map of all JSON objects in the imports folder.
     * @param filename The name of the file (w/wo variable) to get from definitions.
     *
     * @return The located JsonObject, if present.
     */
    private static JsonObject getRequiredImport(Map<File, JsonObject> definitions, String filename) {
        Objects.requireNonNull(filename, "Imports may not be null.");
        final String[] split = filename.split("::");
        final String name = split[0];

        final JsonObject json = find(definitions.entrySet(), e -> name.equals(e.getKey().getName()))
            .map(Map.Entry::getValue)
            .orElseThrow(() -> runExF("Use of undeclared import: {}", name));

        // The user wants to import a specific variable.
        if (split.length > 1) {
            final String key = split[1];
            if (!json.has(key)) {
                throw runExF("Import refers to unknown key: {}", key);
            }
            return new JsonObject().add(key, json.get(key));
        }
        // The user wants to include the entire file.
        return json;
    }

    /**
     * Expands all of the variables in this object, using itself as a source.
     *
     * @param json The JSON object to be expanded.
     */
    private static void expand(JsonObject json) {
        copyObject(json, json);
        mergeObject(json, json);
    }

    /**
     * Expands all of the variables inside of <code>root.variables</code>.
     *
     * @param json The JSON object to be expanded.
     */
    private static void expandVariables(JsonObject json) {
        final JsonObject variables = getObject(json, "variables")
            .orElseThrow(() -> runEx("Nothing to expand."));
        expand(variables);
        copyObject(variables, json);
        mergeObject(variables, json);
    }

    /**
     * Substitutes variable references inside of <code>to</code> with definitions from
     * <code>from</code>.
     *
     * @param from The source JSON containing variable declarations.
     * @param to The destination JSON which uses those declarations.
     */
    private static void copyObject(JsonObject from, JsonObject to) {
        final JsonObject clone = new JsonObject();
        for (JsonObject.Member member : to) {
            final String name = member.getName();
            final JsonValue value = member.getValue();
            // Clone the data so it can be updated.
            clone.add(name, value);
            // Substitute recursively.
            if (value.isString()) {
                trySubstitute(from, value.asString()).ifPresent(val -> clone.set(name, val));
            } else if (value.isArray()) {
                copyArray(from, value.asArray());
            } else if (value.isObject()) {
                copyObject(from, value.asObject());
            }
        }
        replaceContents(clone, to);
    }

    /**
     * Substitutes variable references inside of <code>to</code> with definitions from
     * <code>from</code>.
     *
     * @param from The source JSON containing variable declarations.
     * @param to The destination JSON which uses those declarations.
     */
    private static void copyArray(JsonObject from, JsonArray to) {
        final JsonArray clone = new JsonArray();
        for (int i = 0; i < to.size(); i++) {
            final JsonValue value = to.get(i);
            // Clone the data so it can be updated.
            clone.add(value);
            // Substitute recursively.
            if (value.isString()) {
                final Optional<JsonValue> variable = trySubstitute(from, value.asString());
                if (variable.isPresent()) { // i is not effectively final.
                    clone.set(i, variable.get());
                }
            } else if (value.isArray()) {
                copyArray(from, value.asArray());
            } else if (value.isObject()) {
                copyObject(from, value.asObject());
            }
        }
        replaceContents(clone, to);
    }

    /**
     * If <code>val</code> is a reference, the source will be copied into <code>clone</code>.
     *
     * @param from The source where variables are defined.
     * @param s A string which may or may not be a reference.
     * @return The substituted value, if possible.
     */
    private static Optional<JsonValue> trySubstitute(JsonObject from, String s) {
        return asReference(s).map(ref -> substitute(from, ref));
    }

    /**
     * Attempts to extract a variable reference from any potential <code>JsonString</code>.
     *
     * @param s A string which may or may not be a reference.
     * @return The name of the reference, if present.
     */
    private static Optional<String> asReference(String s) {
        if (s.startsWith("$")) {
            return full(s.substring(1));
        }
        return empty();
    }

    /**
     * Copies a key from a JSON object, asserting that one must exist.
     *
     * @throws RuntimeException If the JSON does not contain the expected key.
     * @param from The source where variables are defined.
     * @param ref A string which is known to be a reference.
     * @return The value contained within <code>from</code>.
     */
    private static JsonValue substitute(JsonObject from, String ref) {
        if (from.has(ref)) {
            return from.get(ref);
        }
        throw runExF("Use of undeclared variable: {}", ref);
    }

    /**
     *  Applies the merge operation to every JSON key in <code>to</code>. The "merge"
     * operation accepts a variable name as a JSON key with a value being an array
     * of strings indicating which fields to merge, or else <code>ALL</code>.
     *
     * <p>
     *  For example, assume the following preset: <code>
     *    variables: {
     *      TEST: {
     *          hello: world
     *      }
     *    }
     *    demo: {
     *        $TEST: ALL
     *        hi: mom
     *    }
     * </code>
     *  After expanding the value <code>TEST</code>, the contents of <code>demo</code>
     *  become the following: <code>
     *    demo: {
     *      hello: world
     *      hi: mom
     *    }
     * </code>
     * </p>
     *
     * @param from The source where variables are defined.
     * @param to The object to merge variables inside of.
     */
    private static void mergeObject(JsonObject from, JsonObject to) {
        final JsonObject clone = new JsonObject();
        for (JsonObject.Member member : to) {
            final String name = member.getName();
            final JsonValue value = member.getValue();
            // Clone the data so it can be updated.
            clone.add(name, value);
            // Merge recursively.
            if (value.isObject()) {
                mergeObject(from, value.asObject());
            } else {
                tryMerge(from, clone, name, value);
            }
        }
        replaceContents(clone, to);
    }

    private static void tryMerge(JsonObject from, JsonObject to, String key, JsonValue value) {
        trySubstitute(from, key).ifPresent(ref -> {
            if (!ref.isObject()) {
                throw runExF("Only objects can be merged: {}", key);
            }
            final JsonArray arr = asOrToArray(value);
            if (arr.contains(JsonValue.valueOf("ALL"))) {
                to.addAll(ref.asObject());
            } else {
                for (JsonValue v : arr) {
                    if (!v.isString()) {
                        throw runExF("Not a field: {}", v);
                    }
                    to.add(v.asString(), substitute(ref.asObject(), v.asString()));
                }
            }
            // Remove the original key which has now been expanded.
            to.remove(key);
        });
    }

    /**
     * Copies every JSON member from one JSON to another.
     *
     * @param from The JSON object to copy from.
     * @param to The JSON object to copy into.
     */
    private static void replaceContents(JsonObject from, JsonObject to) {
        to.clear();
        to.addAll(from);
    }

    /**
     * Copies every JSON value from one array to another.
     *
     * @param from The JSON array to copy from.
     * @param to The JSON array to copy into.
     */
    private static void replaceContents(JsonArray from, JsonArray to) {
        to.clear();
        to.addAll(from);
    }

    /**
     * Deletes <code>imports</code> and <code>variables</code> from this JSON.
     *
     * @param json The JSON object to be cleaned.
     */
    private static void deleteUnused(JsonObject json) {
        json.remove("imports");
        json.remove("variables");
    }
}
