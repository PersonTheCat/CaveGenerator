package personthecat.cavegenerator.presets.lang;

import org.hjson.JsonArray;
import org.hjson.JsonObject;
import org.hjson.JsonValue;
import personthecat.catlib.util.HjsonUtils;
import personthecat.cavegenerator.init.PresetLoadingContext;
import personthecat.cavegenerator.io.ModFolders;

import java.io.File;
import java.util.*;

import static java.util.Optional.empty;
import static personthecat.catlib.io.FileIO.locateFileRecursive;
import static personthecat.catlib.util.Shorthand.find;
import static personthecat.catlib.util.Shorthand.full;
import static personthecat.cavegenerator.exception.CaveSyntaxException.caveSyntax;

public class ImportHelper {

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
     * @param exp The name of the file (w/wo variable) to get from definitions.
     *
     * @return The located JsonObject, if present.
     */
    public static JsonObject getRequiredImport(final Map<File, JsonObject> definitions, final String exp) {
        Objects.requireNonNull(exp, "Imports may not be null");
        final Import helper = new Import(exp);
        // Get the JSON object by filename.
        final JsonObject json = helper.loadObject(definitions)
            .orElseThrow(() -> caveSyntax("Use of undeclared import: {}", exp));
        return readImport(json, helper);
    }

    /** Loads an import definition directly from the disk. */
    public static JsonObject getRequiredImport(final String exp) {
        Objects.requireNonNull(exp, "Imports may not be null");
        final Import helper = new Import(exp);
        final JsonObject json = helper.tryLoadObject()
            .orElseThrow(() -> caveSyntax("Invalid import: {}", exp));
        return readImport(json, helper);
    }

    /** Recursively loads all import definitions required by the expression. */
    public static Map<File, JsonObject> locateDefinitions(final String exp) {
        Objects.requireNonNull(exp, "Imports may not be null");
        final Map<File, JsonObject> definitions = new HashMap<>();
        locateRecursively(definitions, exp);
        return definitions;
    }

    /** Recursively loads all import definitions from the imports array in a JSON object. */
    public static Map<File, JsonObject> locateDefinitions(final JsonObject json) {
        final Optional<List<String>> imports = HjsonUtils.getStringArray(json, CaveLangExtension.IMPORTS);
        if (!imports.isPresent()) {
            return new HashMap<>();
        }
        final Map<File, JsonObject> definitions = new HashMap<>();
        imports.get().forEach(exp -> locateRecursively(definitions, exp));
        return definitions;
    }

    private static void locateRecursively(final Map<File, JsonObject> definitions, final String exp) {
        final Import helper = new Import(exp);
        final File file = helper.locateFile()
            .orElseThrow(() -> caveSyntax("Unable to locate {} in {}", helper.filename, exp));
        if (definitions.containsKey(file)) {
            return;
        }
        final JsonObject json = PresetLoadingContext.readJson(file);
        if (json == null) {
            return;
        }
        definitions.put(file, json);
        final JsonValue imports = json.get(CaveLangExtension.IMPORTS);
        if (imports != null) {
            final JsonArray importArray = HjsonUtils.asOrToArray(imports);
            for (JsonValue importExp : importArray) {
                if (!importExp.isString()) {
                    throw caveSyntax("Invalid import expression: {}", importExp);
                }
                locateRecursively(definitions, importExp.asString());
            }
        }
    }

    /** Returns the list of keys that were imported by this expression. */
    public static List<String> getKeys(final Map<File, JsonObject> definitions, final String exp) {
        Objects.requireNonNull(exp, "Imports may not be null");
        final Import helper = new Import(exp);

        if (helper.as.isPresent()) {
            return Collections.singletonList(helper.as.get());
        } else if (helper.variable.isPresent()) {
            return Collections.singletonList(helper.variable.get());
        }
        final JsonObject json = helper.loadObject(definitions)
            .orElseThrow(() -> caveSyntax("Expression not validated in time: {}", exp));
        return json.getNames();
    }

    /** Reads JSON data from an imported preset based on an expression. */
    private static JsonObject readImport(final JsonObject imported, final Import helper) {
        // No specific variable && AS statement -> name the object itself.
        if (helper.as.isPresent() && !helper.variable.isPresent()) {
            return new JsonObject().add(helper.as.get(), imported);
        } else if (helper.variable.isPresent()) {
            String key = helper.variable.get();
            // Make sure the variable exists, then copy it only.
            if (!imported.has(key)) {
                final String asFunction = key + "()";
                if (imported.has(asFunction)) {
                    key = asFunction;
                } else {
                    throw caveSyntax("Import refers to unknown key: {}", key);
                }
            }
            final String name = helper.as.orElse(key);
            return new JsonObject().add(name, imported.get(key));
        }
        // The user wants to include the entire file.
        return imported;
    }

    /** Splits an import statement into its tokens. */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private static class Import {
        final String filename;
        final Optional<String> variable;
        final Optional<String> as;

        Import(String statement) {
            final String[] splitAs = statement.split("\\s+(as|AS|As)\\s+");
            final String[] splitVar = splitAs[0].split("\\s*::\\s*");
            filename = splitVar[0];
            variable = splitVar.length > 1 ? full(splitVar[1]) : empty();
            as = splitAs.length > 1 ? full(splitAs[1]) : empty();
        }

        Optional<JsonObject> loadObject(final Map<File, JsonObject> definitions) {
            final JsonObject root = definitions.get(new File(ModFolders.IMPORT_DIR, this.filename));
            if (root != null) {
                return full(root);
            }
            return find(definitions.entrySet(), e -> this.matches(e.getKey()))
                .map(Map.Entry::getValue);
        }

        Optional<JsonObject> tryLoadObject() {
            return locateFile().flatMap(f -> Optional.ofNullable(PresetLoadingContext.readJson(f)));
        }

        Optional<File> locateFile() {
            final File root = new File(ModFolders.IMPORT_DIR, this.filename);
            if (root.exists()) {
                return full(root);
            }
            return locateFileRecursive(ModFolders.IMPORT_DIR, this::matches);
        }

        boolean matches(final File f) {
            // Users can essentially be as specific as they like.
            return f.getPath().replace("\\", "/").endsWith(filename);
        }
    }
}
