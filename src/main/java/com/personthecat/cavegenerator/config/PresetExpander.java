package com.personthecat.cavegenerator.config;

import com.personthecat.cavegenerator.CaveInit;
import com.personthecat.cavegenerator.util.Calculator;
import org.hjson.JsonArray;
import org.hjson.JsonObject;
import org.hjson.JsonValue;

import java.io.File;
import java.util.*;

import static com.personthecat.cavegenerator.util.HjsonTools.asOrToArray;
import static com.personthecat.cavegenerator.util.HjsonTools.getArray;
import static com.personthecat.cavegenerator.util.HjsonTools.getObject;
import static com.personthecat.cavegenerator.util.HjsonTools.getObjectOrNew;
import static com.personthecat.cavegenerator.util.HjsonTools.setOrAdd;
import static com.personthecat.cavegenerator.util.CommonMethods.empty;
import static com.personthecat.cavegenerator.util.CommonMethods.find;
import static com.personthecat.cavegenerator.util.CommonMethods.full;
import static com.personthecat.cavegenerator.util.CommonMethods.runEx;
import static com.personthecat.cavegenerator.util.CommonMethods.runExF;

public class PresetExpander {

    /** The name of the imports array at the top of any preset file. */
    public static final String IMPORTS = "imports";

    /** The name of the variables object at the top of any preset file. */
    public static final String VARIABLES = "variables";

    /** The name of the file containing default settings. */
    public static final String DEFAULTS = "defaults.cave";

    /** The name of the implicit variable holding all of the default settings. */
    public static final String VANILLA = "VANILLA";

    /**
     * Expands all of the variable definitions and imports into concrete data.
     *
     * @param presets All of the main preset files mapped to their parent files.
     * @param definitions A map of all JSON objects in the imports folder.
     */
    public static void expandAll(Map<File, JsonObject> presets, Map<File, JsonObject> definitions) {
        // Enable imports to uniquely import each other. This is checked later.
        definitions.forEach((f, json) -> copyImports(definitions, json, true));
        // Expand any variables used inside of each import.
        definitions.forEach((f, json) -> expand(json));
        // Copy all of the imports directly into each json.
        presets.forEach((f, json) -> copyImports(definitions, json, false));
        // Copy defaults.cave as VANILLA implicitly, if absent.
        copyVanilla(presets, definitions);
        // Expand the variables now inside of each json.
        presets.forEach((f, json) -> expandVariables(json));
        // Evaluate any arithmetic expressions in all presets.
        presets.forEach((f, json) -> evaluateAll(json));
        // Delete all of the now unneeded imports and variables.
        presets.forEach((f, json) -> deleteUnused(json));
    }

    /**
     * Copies the value <code>defaults.cave as VANILLA</code> implicitly. This will
     * improve concision and hopefully give new users less to learn. My concern is
     * that it will obscure the purpose of the <code>imports</code> array, as every
     * other variable still needs to be imported manually. I may quickly remove this
     * implicit variable if that turns out to be the case.
     *
     * @param presets All of the main preset files mapped to their parent files.
     * @param definitions A map of all JSON objects in the imports folder.
     */
    private static void copyVanilla(Map<File, JsonObject> presets, Map<File, JsonObject> definitions) {
        // This should not be possible anyway.
        final JsonObject vanilla = getDefaults(definitions)
            .orElseThrow(() -> runEx(DEFAULTS + " may not be renamed or deleted."));
        // Add this value implicitly in every preset.
        // It will be removed if unused.
        for (JsonObject json : presets.values()) {
            final JsonObject variables = getObjectOrNew(json, VARIABLES);
            // Users can declare their own variables called VANILLA.
            if (!variables.has(VANILLA)) {
                variables.add(VANILLA, vanilla);
            }
        }
    }

    /**
     * Retrieves the default values preset from this map, if present.
     *
     * @param definitions A map of all JSON objects in the imports folder.
     * @return defaults.cave, if present.
     */
    private static Optional<JsonObject> getDefaults(Map<File, JsonObject> definitions) {
        for (Map.Entry<File, JsonObject> entry : definitions.entrySet()) {
            if (entry.getKey().getName().equals(DEFAULTS)) {
                return full(entry.getValue());
            }
        }
        return empty();
    }

    /**
     * Copies all of the imports declared in the current preset directly into it.
     * They will be stored inside of <code>root.variables</code>
     *
     * @param definitions A map of all JSON objects in the imports folder.
     * @param json The current JSON object being copied into.
     */
    private static void copyImports(Map<File, JsonObject> definitions, JsonObject json, boolean root) {
        final Set<JsonObject> imports = new HashSet<>();
        // Copy by reference all of the required jsons into a set.
        getArray(json, IMPORTS).ifPresent(arr -> {
            for (JsonValue value : arr) {
                if (!value.isString()) {
                    throw runExF("Invalid data type in imports: {}", value.toString());
                }
                imports.add(getRequiredImport(definitions, value.asString()));
            }
        });
        // copy the contents of each import into variables.
        final JsonObject variables = root ? json : getObjectOrNew(json, VARIABLES);
        imports.forEach(variables::addAll);
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
        final Import helper = new Import(filename);
        // Get the JSON object by filename.
        final JsonObject json = helper.locate(definitions)
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
        final JsonObject variables = getObject(json, VARIABLES)
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
                ReferenceHelper.trySubstitute(from, value.asString()).ifPresent(val -> clone.set(name, val));
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
                final Optional<JsonValue> variable = ReferenceHelper.trySubstitute(from, value.asString());
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
     *  Applies the merge operation to every JSON key in <code>to</code>. The "merge"
     * operation accepts a variable name as a JSON key with a value being an array
     * of strings indicating which fields to merge, or else <code>ALL</code>.
     *
     * <p>
     *  For example, assume the following preset: <pre>
     *    variables: {
     *      TEST: {
     *          hello: world
     *      }
     *    }
     *    demo: {
     *        $TEST: ALL
     *        hi: mom
     *    }
     * </pre>
     *  After expanding the value <code>TEST</code>, the contents of <code>demo</code>
     *  become the following: <pre>
     *    demo: {
     *      hello: world
     *      hi: mom
     *    }
     * </pre>
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
        final Optional<JsonValue> r = ReferenceHelper.trySubstitute(from, key);
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
            // If the value exists, override it.
            setOrAdd(to, key, ReferenceHelper.readValue(from, key));
        }
    }

    /**
     * Evaluates any arithmetic expressions inside of a JSON object.
     *
     * <p>
     *  For example, given this object: <pre>
     *    test: {
     *      exp: ((2 + 2) * 2) ^ 2
     *    }
     * </pre>
     *  This will be the output: <pre>
     *    test: {
     *      exp: 64.0
     *    }
     * </pre>
     * </p>
     *
     * Remember that these objects are evaluated in place, and as such,
     * the original contents are destroyed.
     *
     * @param json An object which may or may not contain expressions.
     */
    private static void evaluateAll(JsonObject json) {
        final JsonObject clone = new JsonObject();
        for (JsonObject.Member member : json) {
            final String name = member.getName();
            final JsonValue value = member.getValue();
            if (value.isString()) {
                final String exp = value.asString();
                if (Calculator.isExpression(exp)) {
                    clone.add(name, Calculator.evaluate(value.asString()));
                    continue;
                }
            } else if (value.isObject()) {
                evaluateAll(value.asObject());
            } else if (value.isArray()) {
                evaluateAll(value.asArray());
            }
            clone.add(name, value);
        }
        replaceContents(clone, json);
    }

    /**
     * Evaluates any arithmetic expressions inside of a JSON array.
     *
     * @param json An array which may or may not contain expressions.
     */
    private static void evaluateAll(JsonArray json) {
        final JsonArray clone = new JsonArray();
        for (JsonValue value : json) {
            if (value.isString()) {
                final String exp = value.asString();
                if (Calculator.isExpression(exp)) {
                    clone.add(Calculator.evaluate(value.asString()));
                    continue;
                }
            } else if (value.isObject()) {
                evaluateAll(value.asObject());
            } else if (value.isArray()) {
                evaluateAll(value.asArray());
            }
            clone.add(value);
        }
        replaceContents(clone, json);
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
        json.remove(IMPORTS);
        json.remove(VARIABLES);
    }

    /** Splits an import statement into its tokens. */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private static class Import {
        final String filename;
        final Optional<String> variable;
        final Optional<String> as;

        Import(String statement) {
            final String[] splitAs = statement.split("\\s*(as|AS|As)\\s*");
            final String[] splitVar = splitAs[0].split("\\s*::\\s*");
            filename = splitVar[0];
            variable = splitVar.length > 1 ? full(splitVar[1]) : empty();
            as = splitAs.length > 1 ? full(splitAs[1]) : empty();
        }

        Optional<JsonObject> locate(Map<File, JsonObject> defs) {
            final JsonObject root = defs.get(new File(CaveInit.IMPORT_DIR, this.filename));
            if (root != null) {
                return full(root);
            }
            return find(defs.entrySet(), e -> this.matches(e.getKey()))
                .map(Map.Entry::getValue);
        }

        boolean matches(File f) {
            // Users can essentially be as specific as they like.
            return f.getPath().replace("\\", "/").endsWith(filename);
        }
    }
}
