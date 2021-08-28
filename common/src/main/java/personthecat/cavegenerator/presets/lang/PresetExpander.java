package personthecat.cavegenerator.presets.lang;

import org.apache.commons.lang3.ArrayUtils;
import org.hjson.JsonArray;
import org.hjson.JsonObject;
import org.hjson.JsonValue;
import personthecat.catlib.util.HjsonUtils;
import personthecat.catlib.util.JsonTransformer;
import personthecat.cavegenerator.exception.CaveEvaluationException;
import personthecat.cavegenerator.io.ModFolders;
import personthecat.cavegenerator.util.Calculator;
import personthecat.fresult.Result;

import java.io.File;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import static java.util.Optional.empty;
import static personthecat.catlib.util.Shorthand.full;
import static personthecat.cavegenerator.exception.CaveSyntaxException.caveSyntax;

public class PresetExpander {

    /** The name of the imports array at the top of any preset file. */
    public static final String IMPORTS = "imports";

    /** The name of the variables object at the top of any preset file. */
    public static final String VARIABLES = "variables";

    /** The name of the file containing default settings. */
    public static final String DEFAULTS = "defaults.cave";

    /** The name of the implicit variable holding all of the default settings. */
    public static final String VANILLA = "VANILLA";

    /** The keyword used for merging every field in an object. */
    public static final Pattern MERGE_ALL = Pattern.compile("all", Pattern.CASE_INSENSITIVE);

    /**
     * Expands all of the variable definitions and imports into concrete data.
     *
     * @param presets All of the main preset files mapped to their parent files.
     * @param definitions A map of all JSON objects in the imports folder.
     */
    public static void expandAll(final Map<File, JsonObject> presets, final Map<File, JsonObject> definitions) {
        // Automatically copy variables to the root level in imports.
        definitions.forEach((f, json) -> variablesToRoot(json));
        // Replace inner references so that invalid keys do not get copied out.
        definitions.forEach((f, json) -> { if (!json.has(IMPORTS)) expand(json); });
        // Enable imports to uniquely import each other. This is checked later.
        definitions.forEach((f, json) -> copyImports(definitions, json, true));
        // Expand any variables used inside of each import.
        definitions.forEach((f, json) -> expand(json));
        // Strip unused variables so they don't cause issues later.
        definitions.forEach((f, json) -> stripPrivateValues(definitions, json));
        // Copy all of the imports directly into each json.
        presets.forEach((f, json) -> copyImports(definitions, json, false));
        // Copy defaults.cave as VANILLA implicitly, if absent.
        copyVanilla(presets, definitions);
        // Expand the variables now inside of each json.
        presets.forEach((f, json) -> expandVariables(json));
        // Evaluate any arithmetic expressions in all presets.
        presets.forEach((f, json) -> calculateAll(json));
        // Delete all of the now unneeded imports and variables.
        presets.forEach((f, json) -> deleteUnused(json));
    }

    /**
     * Expands a single preset, automatically loading import files as needed.
     *
     * @param file The JSON file being expanded.
     */
    public static Result<JsonObject, CaveEvaluationException> expand(final File file) {
        final Optional<JsonObject> read = HjsonUtils.readSuppressing(file);
        if (!read.isPresent()) {
            return Result.err(new CaveEvaluationException(file));
        }
        final JsonObject json = read.get();
        final Map<File, JsonObject> singleton = Collections.singletonMap(ModFolders.CG_DIR, json);
        final Map<File, JsonObject> definitions = ImportHelper.locateDefinitions(json);
        expandAll(singleton, definitions);
        return Result.ok(json);
    }

    /**
     * Copies all of the variable definitions in <code>root.variables</code> to the
     * root level. This is used in import files to provide backward compatibility
     * with regular presets while also enabling them to read variables from the root
     * object.
     *
     * @param json The current JSON object being copied out of.
     */
    private static void variablesToRoot(final JsonObject json) {
        final JsonValue variables = json.get(VARIABLES);
        if (variables != null) {
            if (!variables.isObject()) {
                throw caveSyntax("\"{}\" is reserved for definitions and must be an object.", VARIABLES);
            }
            json.addAll(variables.asObject());
        }
    }

    /**
     * Copies all of the imports declared in the current preset directly into it.
     * They will be stored inside of <code>root.variables</code>
     *
     * @param definitions A map of all JSON objects in the imports folder.
     * @param json The current JSON object being copied into.
     */
    private static void copyImports(final Map<File, JsonObject> definitions, JsonObject json, final boolean root) {
        final Set<JsonObject> imports = new HashSet<>();
        // Copy by reference all of the required jsons into a set.
        HjsonUtils.getArray(json, IMPORTS).ifPresent(arr -> {
            for (final JsonValue value : arr) {
                if (!value.isString()) {
                    throw caveSyntax("Invalid data type in imports: {}", value.toString());
                }
                imports.add(ImportHelper.getRequiredImport(definitions, value.asString()));
            }
        });
        // copy the contents of each import into variables.
        final JsonObject variables = root ? json : HjsonUtils.getObjectOrNew(json, VARIABLES);
        imports.forEach(variables::addAll);
    }

    /**
     * Expands all of the variables in this object, using itself as a source.
     *
     * @param json The JSON object to be expanded.
     */
    private static void expand(final JsonObject json) {
        copyObject(json, json);
        mergeObject(json, json);
        overrideObject(json);
    }

    /**
     * Removes all imported variables and functions from an import file so that
     * the file can be re-exported only with the symbols it started with.
     *
     * @param definitions A map of all JSON objects in the imports folder.
     * @param json The JSON object to be stripped.
     */
    private static void stripPrivateValues(final Map<File, JsonObject> definitions, final JsonObject json) {
        // Imported variables are implicitly private.
        final JsonValue imports = json.get(IMPORTS);
        if (imports != null) {
            for (final String exp : HjsonUtils.toStringArray(HjsonUtils.asOrToArray(imports))) {
                for (final String key : ImportHelper.getKeys(definitions, exp)) {
                    json.remove(key);
                    json.remove(key + "()"); // unsophisticated for now
                }
            }
            json.remove(IMPORTS);
        }
        // Variables in the `variables` object are also private.
        final JsonValue variables = json.get(VARIABLES);
        if (variables != null) {
            if (!variables.isObject()) {
                throw caveSyntax("<variables> must be an object");
            }
            for (final JsonObject.Member variable : variables.asObject()) {
                // This will have been copied to the root level.
                json.remove(variable.getName());
                json.remove(variable.getName() + "()");
            }
            json.remove(VARIABLES);
        }
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
    private static void copyVanilla(final Map<File, JsonObject> presets, final Map<File, JsonObject> definitions) {
        // This should not be possible anyway.
        final JsonObject vanilla = getDefaults(definitions)
            .orElseThrow(() -> caveSyntax(DEFAULTS + " may not be renamed or deleted."));
        // Add this value implicitly in every preset.
        // It will be removed if unused.
        for (final JsonObject json : presets.values()) {
            final JsonObject variables = HjsonUtils.getObjectOrNew(json, VARIABLES);
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
    private static Optional<JsonObject> getDefaults(final Map<File, JsonObject> definitions) {
        for (final Map.Entry<File, JsonObject> entry : definitions.entrySet()) {
            if (entry.getKey().getName().equals(DEFAULTS)) {
                return full(entry.getValue());
            }
        }
        return empty();
    }

    /**
     * Expands all of the variables inside of <code>root.variables</code>.
     *
     * @param json The JSON object to be expanded.
     */
    private static void expandVariables(final JsonObject json) {
        final JsonObject variables = HjsonUtils.getObject(json, VARIABLES)
            .orElseThrow(() -> caveSyntax("Nothing to expand."));
        expand(variables);
        copyObject(variables, json);
        mergeObject(variables, json);
        overrideObject(json);
    }

    /**
     * Substitutes variable references inside of <code>to</code> with definitions from
     * <code>from</code>.
     *
     * @param from The source JSON containing variable declarations.
     * @param to The destination JSON which uses those declarations.
     */
    private static void copyObject(final JsonObject from, final JsonObject to) {
        final JsonObject clone = new JsonObject();
        for (final JsonObject.Member member : to) {
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
    private static void copyArray(final JsonObject from, final JsonArray to) {
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
    private static void mergeObject(final JsonObject from, final JsonObject to) {
        final JsonObject clone = new JsonObject();
        for (final JsonObject.Member member : to) {
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
     * @param to The array which may contain objects requiring merges.
     */
    private static void mergeArray(final JsonObject from, final JsonArray to) {
        // No clone needed. We won't merge into the array itself.
        for (final JsonValue value : to) {
            if (value.isObject()) {
                mergeObject(from, value.asObject());
            } else if (value.isArray()) {
                mergeArray(from, value.asArray());
            }
        }
    }

    /**
     * Applies all recursive overrides inside of a JSON object.
     *
     * @param json The object which may contain recursive overrides.
     */
    private static void overrideObject(final JsonObject json) {
        final JsonObject clone = new JsonObject();
        for (final JsonObject.Member member : json) {
            final String name = member.getName();
            final JsonValue value = member.getValue();
            if (name.startsWith("*")) {
                final String[] path = name.substring(1).split(Pattern.quote("."));
                final String recursiveKey = path[0];
                if (path.length == 1) {
                    forEachParent(json, recursiveKey, o -> o.set(recursiveKey, value));
                } else {
                    JsonTransformer.recursive(recursiveKey).forEach(json, o ->
                        JsonTransformer.withPath(ArrayUtils.subarray(path, 1, path.length - 1))
                            .forEach(o, c -> c.set(path[path.length - 1], value)));
                }
            } else {
                clone.add(name, value);
            }
            if (value.isObject()) {
                overrideObject(value.asObject());
            } else if (value.isArray()) {
                overrideArray(value.asArray());
            }
        }
        replaceContents(clone, json);
    }

    /**
     * Applies all recursive overrides inside of a JSON array.
     *
     * @param json The array which may contain recursive overrides.
     */
    private static void overrideArray(final JsonArray json) {
        for (final JsonValue value : json) {
            if (value.isObject()) {
                overrideObject(value.asObject());
            } else if (value.isArray()) {
                overrideArray(value.asArray());
            }
        }
    }

    /**
     * Performs an action on every JSON object containing the given field key.
     *
     * @param json The object which may contain this field at any depth.
     * @param key The key which the object must contain
     * @param fn The function to run for each object containing the key
     */
    private static void forEachParent(final JsonObject json, final String key, final Consumer<JsonObject> fn) {
        for (final JsonObject.Member member : json) {
            final String name = member.getName();
            final JsonValue value = member.getValue();
            if (name.equals(key)) {
                fn.accept(json);
                continue;
            }
            if (value.isObject()) {
                forEachParent(value.asObject(), key, fn);
            } else if (value.isArray()) {
                forEachParent(value.asArray(), key, fn);
            }
        }
    }

    /**
     * Performs an action on every JSON object containing the given field key.
     *
     * @param json The array which may contain this field at any depth.
     * @param key The key which the object must contain
     * @param fn The function to run for each object containing the key
     */
    private static void forEachParent(final JsonArray json, final String key, final Consumer<JsonObject> fn) {
        for (final JsonValue value : json) {
            if (value.isObject()) {
                forEachParent(value.asObject(), key, fn);
            } else if (value.isArray()) {
                forEachParent(value.asArray(), key, fn);
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
    private static boolean tryMerge(final JsonObject from, final JsonObject to, final String key, final JsonValue value) {
        final Optional<JsonValue> r = ReferenceHelper.trySubstitute(from, key);
        r.ifPresent(ref -> {
            if (!ref.isObject()) {
                throw caveSyntax("Only objects can be merged: {}", key);
            }
            if (value.isString() && MERGE_ALL.matcher(value.asString()).matches()) {
                to.addAll(ref.asObject());
            } else {
                addAllReferences(ref.asObject(), to, HjsonUtils.asOrToArray(value));
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
    private static void addAllReferences(final JsonObject from, final JsonObject to, final JsonArray array) {
        for (final JsonValue v : array) {
            if (!v.isString()) {
                throw caveSyntax("Not a field: {}", v);
            }
            final String key = v.asString();
            // If the value exists, override it.
            to.set(key, ReferenceHelper.readValue(from, key));
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
    public static void calculateAll(final JsonObject json) {
        final JsonObject clone = new JsonObject();
        for (final JsonObject.Member member : json) {
            final String name = member.getName();
            final JsonValue value = member.getValue();
            if (value.isString()) {
                final String exp = value.asString();
                if (Calculator.isExpression(exp)) {
                    clone.add(name, Calculator.evaluate(value.asString()));
                    continue;
                }
            } else if (value.isObject()) {
                calculateAll(value.asObject());
            } else if (value.isArray()) {
                calculateAll(value.asArray());
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
    public static void calculateAll(final JsonArray json) {
        final JsonArray clone = new JsonArray();
        for (final JsonValue value : json) {
            if (value.isString()) {
                final String exp = value.asString();
                if (Calculator.isExpression(exp)) {
                    clone.add(Calculator.evaluate(value.asString()));
                    continue;
                }
            } else if (value.isObject()) {
                calculateAll(value.asObject());
            } else if (value.isArray()) {
                calculateAll(value.asArray());
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
    private static void replaceContents(final JsonObject from, final JsonObject to) {
        to.clear();
        to.addAll(from);
    }

    /**
     * Copies every JSON value from one array to another.
     *
     * @param from The JSON array to copy from.
     * @param to The JSON array to copy into.
     */
    private static void replaceContents(final JsonArray from, final JsonArray to) {
        to.clear();
        to.addAll(from);
    }

    /**
     * Deletes <code>imports</code> and <code>variables</code> from this JSON.
     *
     * @param json The JSON object to be cleaned.
     */
    private static void deleteUnused(final JsonObject json) {
        json.remove(IMPORTS);
        json.remove(VARIABLES);
    }
}
