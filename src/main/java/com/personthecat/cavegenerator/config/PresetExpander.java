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
     * to the filename to indicate a specific variable to include from this JSON.
     *
     * <p>
     * For example, you can write <code>defaults.cave::VANILLA</code> to import only
     * the variable <code>VANILLA</code> from <code>defaults.cave</code>.
     *
     * In addition, users may append an <code>as</code> operator to rename whichever variable
     * they are importing.
     *
     * For example, you can write <code>forest.cave::BIOME as FOREST_BIOME</code> to import
     * <code>BIOME</code> from <code>forest.cave</code> renamed to <code>FOREST_BIOME</code>.
     *
     * If they are not importing a specific variable, they can instead use
     * the <code>as</code> operator to import the entire file into a single variable.
     *
     * For example, you can write <code>forest.cave as FOREST</code> to import the entire
     * object inside of <code>forest.cave</code> into a variable called <code>FOREST</code>.
     * </p>
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
        final ImportHelper helper = new ImportHelper(filename);
        // Get the JSON object by filename.
        final JsonObject json = find(definitions.entrySet(), e -> helper.filename.equals(e.getKey().getName()))
            .map(Map.Entry::getValue)
            .orElseThrow(() -> runExF("Use of undeclared import: {}", filename));
        // No specific variable && AS statement -> name the object itself.
        if (helper.as.isPresent() && !helper.variable.isPresent()) {
            return new JsonObject().add(helper.as.get(), json);
        } else if (helper.variable.isPresent()) {
            final String key = helper.variable.get();
            // Make sure the variable exists, then copy it only.
           if (!json.has(key)) {
               throw runExF("Import refers to unknown key: {}", key);
           }
           final String name = helper.as.orElse(key);
           return new JsonObject().add(name, json.get(key));
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
            if (!tryMerge(from, clone, name, value)) {
                if (value.isObject()) {
                    mergeObject(from, value.asObject());
                } else if (value.isArray()) {
                    mergeArray(from, value.asArray());
                }
            }
        }
        replaceContents(clone, to);
    }

    /**
     *  Applies the merge operation to every JSON object nested in an array.
     *
     * @param from The source where variables are defined.
     * @param to the array which may contain objects requiring merges.
     */
    private static void mergeArray(JsonObject from, JsonArray to) {
        // No clone needed. We won't merge into the array itself.
        for (JsonValue value : to) {
            if (value.isObject()) {
                mergeObject(from, value.asObject());
            } else if (value.isArray()) {
                mergeArray(from, value.asArray());
            }
        }
    }

    /**
     * If required, merges all of the fields from <code>key</code>--if it is a reference
     * to an object--into <code>to</code>.
     *
     * @param from The source of the object to be merged.
     * @param to The JSON object to be written into.
     * @param key The object key which may or may not be a reference.
     * @param value The value which may or may not be an array of field names.
     * @return Whether a merge took place.
     */
    private static boolean tryMerge(JsonObject from, JsonObject to, String key, JsonValue value) {
        final Optional<JsonValue> r = trySubstitute(from, key);
        r.ifPresent(ref -> {
            if (!ref.isObject()) {
                throw runExF("Only objects can be merged: {}", key);
            }
            final JsonArray arr = asOrToArray(value);
            if (arr.contains("ALL")) {
                to.addAll(ref.asObject());
            } else {
                addAllReferences(ref.asObject(), to, arr);
            }
            // Remove the original key which has now been expanded.
            to.remove(key);
        });
        return r.isPresent();
    }

    /**
     * Substitutes a series of references at the current level.
     *
     * @param from The source where variables are defined.
     * @param to The object to write these variables inside of.
     * @param array The array of keys to look up.
     */
    private static void addAllReferences(JsonObject from, JsonObject to, JsonArray array) {
        for (JsonValue v : array) {
            if (!v.isString()) {
                throw runExF("Not a field: {}", v);
            }
            final String key = v.asString();
            if (!to.has(key)) { // Allow overrides.
                to.add(key, substitute(from, key));
            }
        }
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

    /** Splits an import statement into its tokens. */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private static class ImportHelper {
        final String filename;
        final Optional<String> variable;
        final Optional<String> as;

        ImportHelper(String statement) {
            final String[] splitAs = statement.split("\\s*(as|AS|As)\\s*");
            final String[] splitVar = splitAs[0].split("\\s*::\\s*");
            filename = splitVar[0];
            variable = splitVar.length > 1 ? full(splitVar[1]) : empty();
            as = splitAs.length > 1 ? full(splitAs[1]) : empty();
        }
    }
}
