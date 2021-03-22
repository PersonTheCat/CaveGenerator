package com.personthecat.cavegenerator.util;

import com.personthecat.cavegenerator.model.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.structure.template.PlacementSettings;
import net.minecraftforge.common.BiomeDictionary;
import org.apache.commons.io.output.NullWriter;
import org.hjson.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.personthecat.cavegenerator.util.CommonMethods.extension;
import static com.personthecat.cavegenerator.util.CommonMethods.find;
import static com.personthecat.cavegenerator.util.CommonMethods.getBiome;
import static com.personthecat.cavegenerator.util.CommonMethods.getBiomes;
import static com.personthecat.cavegenerator.util.CommonMethods.getBiomeType;
import static com.personthecat.cavegenerator.util.CommonMethods.getBlockState;
import static com.personthecat.cavegenerator.util.CommonMethods.runEx;
import static com.personthecat.cavegenerator.util.CommonMethods.runExF;

@SuppressWarnings("WeakerAccess")
public class HjsonTools {

    /** The settings to be used when outputting JsonObjects to the disk. */
    private static final HjsonOptions FORMATTER = new HjsonOptions()
        .setAllowCondense(true)
        .setAllowMultiVal(true)
        .setCommentSpace(0)
        .setSpace(2)
        .setBracesSameLine(true)
        .setOutputComments(true);

    /** Writes the JsonObject to the disk. */
    public static Result<IOException> writeJson(JsonObject json, File file) {
        Result<IOException> result = Result.ok();
        Writer tw = new NullWriter();

        try {
           tw = new FileWriter(file);
           if (extension(file).equals("json")) { // Write as json.
               json.writeTo(tw, Stringify.FORMATTED);
           } else { // Write as hjson.
               json.writeTo(tw, FORMATTER);
           }
        } catch (IOException e) {
            result = Result.of(e);
        } finally {
            assertCloseWriter(tw);
        }
        return result;
    }

    /** Forcibly closes the input writer, asserting that there should be no errors. */
    private static void assertCloseWriter(Writer tw) {
        try {
            tw.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /** Variant of setOrAdd() used for boolean values. */
    public static JsonObject setOrAdd(JsonObject json, String field, boolean value) {
        return setOrAdd(json, field, JsonValue.valueOf(value));
    }

    /** Modifies or adds a field with the input value. Avoids duplicate fields. */
    public static JsonObject setOrAdd(JsonObject json, String field, JsonValue value) {
        if (json.get(field) != null) {
            return json.set(field, value);
        } else {
            return json.add(field, value);
        }
    }

    /** Updates a single value in a json based on a full, dotted path.  */
    public static void setValueFromPath(JsonObject json, List<PathComponent> path, JsonValue value) {
        if (path.isEmpty()) {
            return;
        }
        final PathComponent lastVal = path.get(path.size() - 1);
        setEither(getLastContainer(json, path), lastVal, value);
    }

    /** Retrieves the last JsonObject or JsonArray represented by the path. */
    public static JsonValue getLastContainer(JsonObject json, List<PathComponent> path) {
        if (path.isEmpty()) {
            return json;
        }
        JsonValue current = json;
        for (int i = 0; i < path.size() - 1; i++) {
            final PathComponent val = path.get(i);
            final PathComponent peek = path.get(i + 1);

            if (val.index.isPresent()) { // Index
                current = getOrTryNew(current.asArray(), val.index.get(), peek);
            } else if (peek.key.isPresent()) { // Key -> key -> object
                current = getObjectOrNew(current.asObject(), val.key
                    .orElseThrow(() -> runEx("Unreachable.")));
            } else { // Key -> index -> array
                current = getArrayOrNew(current.asObject(), val.key
                    .orElseThrow(() -> runEx("Unreachable.")));
            }
        }
        return current;
    }

    /** Attempts to retrieve an object or an array. Creates a new one, if absent. */
    private static JsonValue getOrTryNew(JsonArray array, int index, PathComponent type) {
        if (index == array.size()) { // The value must be added.
            type.key.ifPresent(s -> array.add(new JsonObject()));
            type.index.ifPresent(i -> array.add(new JsonArray()));
        } // if index >= newSize -> index out of bounds
        return array.get(index);
    }

    /** Attempts to set a value in a container which may either be an object or an array. */
    private static void setEither(JsonValue container, PathComponent either, JsonValue value) {
        either.key.ifPresent(key -> container.asObject().set(key, value));
        either.index.ifPresent(index -> container.asArray().set(index, value));
    }

    /** Safely retrieves a boolean from the input object. */
    public static Optional<Boolean> getBool(JsonObject json, String field) {
        return getValue(json, field).map(JsonValue::asBoolean);
    }

    /** Safely retrieves an integer from the input json. */
    public static Optional<Integer> getInt(JsonObject json, String field) {
        return getValue(json, field).map(JsonValue::asInt);
    }

    public static int getIntOr(JsonObject json, String field, int orElse) {
        return getInt(json, field).orElse(orElse);
    }

    /** Retrieves a range of integers from the input object. */
    public static Optional<Range> getRange(JsonObject json, String field) {
        return getValue(json, field)
            .map(HjsonTools::asOrToArray)
            .map(HjsonTools::toIntArray)
            .map(CommonMethods::sort)
            .map(HjsonTools::toRange);
    }

    private static Range toRange(int[] range) {
        if (range.length == 0) {
            return new Range(0);
        }
        return range.length == 1 ? new Range(range[0]) : new Range(range[0], range[range.length - 1]);
    }

    public static Optional<FloatRange> getFloatRange(JsonObject json, String field) {
        return getValue(json, field)
            .map(HjsonTools::asOrToArray)
            .map(HjsonTools::toFloatArray)
            .map(CommonMethods::sort)
            .map(HjsonTools::toFloatRange);
    }

    private static FloatRange toFloatRange(float[] range) {
        if (range.length == 0) {
            return new FloatRange(0F);
        }
        return range.length == 1 ? new FloatRange(range[0]) : new FloatRange(range[0], range[range.length -1]);
    }

    /** Safely retrieves a boolean from the input json. */
    public static Optional<Float> getFloat(JsonObject json, String field) {
        return getValue(json, field).map(JsonValue::asFloat);
    }

    /** Shorthand for getFloat(). */
    public static void getFloat(JsonObject json, String field, Consumer<Float> ifPresent) {
        JsonValue value = json.get(field);
        if (value != null) {
            ifPresent.accept(value.asFloat());
        }
    }

    /** Retrieves a float from the input object. Returns `or` if nothing is found. */
    public static float getFloatOr(JsonObject json, String field, float orElse) {
        return getFloat(json, field).orElse(orElse);
    }

    /** Safely retrieves a string from the input json. */
    public static Optional<String> getString(JsonObject json, String field) {
        return getValue(json, field).map(JsonValue::asString);
    }

    /** Safely retrieves a JsonArray from the input json. */
    public static Optional<JsonArray> getArray(JsonObject json, String field) {
        return getValue(json, field).map(HjsonTools::asOrToArray);
    }

    /**  Retrieves an array or creates a new one, if absent. */
    public static JsonArray getArrayOrNew(JsonObject json, String field) {
        if (!json.has(field)) {
            json.set(field, new JsonArray());
        }
        return getArray(json, field).orElseThrow(() -> runEx("Unreachable."));
    }

    /** Casts or converts a JsonValue to a JsonArray.*/
    public static JsonArray asOrToArray(JsonValue value) {
        return value.isArray() ? value.asArray() : new JsonArray().add(value);
    }

    /** Safely retrieves a boolean from the input json. */
    public static Optional<JsonObject> getObject(JsonObject json, String field) {
        return getValue(json, field).map(JsonValue::asObject);
    }

    /** Retrieves an object from the input object. Returns an empty object, if nothing is found. */
    public static JsonObject getObjectOrNew(JsonObject json, String field) {
        if (!json.has(field)) {
            json.set(field, new JsonObject());
        }
        return getObject(json, field).orElseThrow(() -> runEx("Unreachable."));
    }

    /** Safely retrieves a JsonValue from the input object. */
    public static Optional<JsonValue> getValue(JsonObject json, String field) {
        return Optional.ofNullable(json.get(field));
    }

    /**
     * Safely retrieves an array of JsonObjects from the input json.
     * To-do: Be more consistent and use Optional, instead.
     */
    public static List<JsonObject> getObjectArray(JsonObject json, String field) {
        List<JsonObject> array = new ArrayList<>();
        getValue(json, field).map(HjsonTools::asOrToArray)
            .ifPresent(a -> a.forEach(e -> {
                // This is assumed to be an object. If it isn't,
                // The user should be informed (i.e. crash).
                array.add(e.asObject());
            }));
        return array;
    }

    /** Variant of {@link #getObjectArray} which does not coerce values into objects. */
    public static List<JsonObject> getRegularObjects(JsonObject json, String field) {
        final List<JsonObject> list = new ArrayList<>();
        final JsonArray array = HjsonTools.getValue(json, field)
                .map(HjsonTools::asOrToArray)
                .orElseGet(JsonArray::new);
        for (JsonValue value : array) {
            if (value.isObject()) { // Ignore values that don't belong.
                list.add(value.asObject());
            }
        }
        return list;
    }

    public static Optional<List<Integer>> getIntList(JsonObject json, String field) {
        return getArray(json, field).map(HjsonTools::toIntList);
    }

    private static List<Integer> toIntList(JsonArray array) {
        final List<Integer> ints = new ArrayList<>();
        for (JsonValue value : array) {
            if (!value.isNumber()) {
                throw runExF("Expected an numeric value: {}", value);
            }
            ints.add(value.asInt());
        }
        return ints;
    }

    /** Converts a JsonArray into an array of ints. */
    public static int[] toIntArray(JsonArray array) {
        final int[] ints = new  int[array.size()];
        for (int i = 0; i < array.size(); i++) {
            ints[i] = array.get(i).asInt();
        }
        return ints;
    }

    public static float[] toFloatArray(JsonArray array) {
        final float[] floats = new float[array.size()];
        for (int i = 0; i < array.size(); i++) {
            floats[i] = array.get(i).asFloat();
        }
        return floats;
    }

    /** Safely retrieves a List of Strings from the input json. */
    public static Optional<List<String>> getStringArray(JsonObject json, String field) {
        return getValue(json, field).map(v -> toStringArray(asOrToArray(v)));
    }

    /** Converts a JsonArray into a List of Strings. */
    public static List<String> toStringArray(JsonArray array) {
        List<String> strings = new ArrayList<>();
        for (JsonValue value : array) {
            strings.add(value.asString());
        }
        return strings;
    }

    public static Optional<IBlockState> getState(JsonObject json, String field) {
        return getString(json, field).map(id -> getBlockState(id).orElseThrow(() -> noBlockNamed(id)));
    }

    public static Optional<List<IBlockState>> getStateList(JsonObject json, String field) {
        return getStringArray(json, field).map(HjsonTools::toStateList);
    }

    private static List<IBlockState> toStateList(List<String> ids) {
        return ids.stream().map(id -> getBlockState(id).orElseThrow(() -> noBlockNamed(id)))
            .collect(Collectors.toList());
    }

    public static Optional<List<Direction>> getDirectionList(JsonObject json, String field) {
        return getStringArray(json, field).map(HjsonTools::toDirections);
    }

    private static List<Direction> toDirections(List<String> directions) {
        return directions.stream().map(s -> toEnumValue(s, Direction.class)).collect(Collectors.toList());
    }

    /** Safely retrieves a BlockPos from the input object. */
    public static Optional<BlockPos> getPosition(JsonObject json, String field) {
        return getArray(json, field).map(HjsonTools::toPosition);
    }

    public static Optional<List<BlockPos>> getPositionList(JsonObject json, String field) {
        return getArray(json, field).map(HjsonTools::toPositionList);
    }

    /** Converts the input JsonArray into a BlockPos object. */
    public static BlockPos toPosition(JsonArray coordinates) {
        // Expect exactly 3 elements.
        if (coordinates.size() != 3) {
            throw runEx("Relative coordinates must be specified in an array of 3 elements, e.g. [0, 0, 0].");
        }
        // Convert the array into a BlockPos object.
        return new BlockPos(
            coordinates.get(0).asInt(),
            coordinates.get(1).asInt(),
            coordinates.get(2).asInt()
        );
    }

    private static List<BlockPos> toPositionList(JsonArray positions) {
        final List<BlockPos> list = new ArrayList<>();
        for (JsonValue position : positions) {
            if (position.isNumber()) {
                return Collections.singletonList(toPosition(positions));
            } else if (!position.isArray()) {
                throw runEx("Expected a list of positions, e.g. [[0, 0, 0], [1, 1, 1]].");
            }
            list.add(toPosition(position.asArray()));
        }
        return list;
    }

    public static Optional<List<Biome>> getBiomeList(JsonObject json, String field) {
        return getObject(json, field).map(HjsonTools::toBiomes);
    }

    private static List<Biome> toBiomes(JsonObject json) {
        final List<Biome> biomes = new ArrayList<>();
        // Get biomes by registry name.
        getArray(json, "names").map(HjsonTools::toStringArray).ifPresent(a -> {
            for (String s : a) {
                biomes.add(getBiome(s).orElseThrow(() -> noBiomeNamed(s)));
            }
        });
        // Get biomes by ID.
        getArray(json, "IDs").map(HjsonTools::toIntArray).ifPresent(a -> {
            for (int i : a) {
                biomes.add(getBiome(i).orElseThrow(() -> noBiomeID(i)));
            }
        });
        // Get biomes by type.
        getArray(json, "types").map(HjsonTools::toBiomeTypes).ifPresent(a -> {
            for (BiomeDictionary.Type t : a) {
                Collections.addAll(biomes, getBiomes(t));
            }
        });
        return biomes;
    }

    /** Converts a JsonArray in to a list of BiomeTypes. */
    public static List<BiomeDictionary.Type> toBiomeTypes(JsonArray array) {
        List<BiomeDictionary.Type> types = new ArrayList<>();
        for (JsonValue value : array) {
            types.add(getBiomeType(value.asString()));
        }
        return types;
    }

    /**
     * Constructs the standard PlacementSettings object used by the mod,
     * apply additional values, when possible.
     */
    public static PlacementSettings getPlacementSettings(JsonObject json) {
        // Construct the basic placement values to be modified.
        PlacementSettings settings = new PlacementSettings()
                .setReplacedBlock(Blocks.STONE);

        // Get the additional values for `placement`.
        getFloat(json, "integrity", settings::setIntegrity);

        // Return the final value.
        return settings;
    }

    /**
     * Safely retrieves a ScalableFloat from the input json using default
     * values when necessary.
     */
    private static Optional<ScalableFloat> getScalableFloat(JsonObject json, String field, ScalableFloat defaults) {
        // Attempt to retrieve a corresponding JsonValue from `json`. If
        // it exists, test to see whether it is an object. If it is an
        // object, it should be converted into a ScalableFloat according
        // to the presence of all relevant fields, substituting from
        // `defaults`, when necessary. Else, attempt to parse it as an
        // array. In this case, values are read according to their order
        // and then passed directly into the constructor in-code. Absent
        // values are likewise substituted from `defaults`.
        // Asserting that the value must be an array means that non-array
        // values can also be used at this position, according to the
        // spec of our custom hjson parser. This is especially convenient
        // for users who only want to change the starting value of the
        // resultant float.
        return getValue(json, field).map(v -> {
            if (v.isNumber()) {
                return toScalableFloat(v.asInt(), defaults);
            } else if (v.isObject()) {
                return toScalableFloat(v.asObject(), defaults);
            } else if (!v.isArray()) {
                throw runEx("Scalable float values must be a number, array, or object.");
            }
            return toScalableFloat(v.asArray(), defaults);
        });
    }

    /** Retrieves a scalable float from the input json. Returns the default values when no object is found. */
    public static ScalableFloat getScalableFloatOr(JsonObject json, String field, ScalableFloat defaults) {
        return getScalableFloat(json, field, defaults).orElse(defaults);
    }

    public static ScalableFloat toScalableFloat(int startVal, ScalableFloat defaults) {
        return new ScalableFloat(startVal, defaults.startValRandFactor, defaults.factor, defaults.randFactor, defaults.exponent);
    }

    public static ScalableFloat toScalableFloat(JsonArray array, ScalableFloat defaults) {
        return new ScalableFloat(
            array.size() > 0 ? array.get(0).asFloat() : defaults.startVal,
            array.size() > 1 ? array.get(1).asFloat() : defaults.startValRandFactor,
            array.size() > 2 ? array.get(2).asFloat() : defaults.factor,
            array.size() > 3 ? array.get(3).asFloat() : defaults.randFactor,
            array.size() > 4 ? array.get(4).asFloat() : defaults.exponent
        );
    }

    public static ScalableFloat toScalableFloat(JsonObject json, ScalableFloat defaults) {
        return new ScalableFloat(
            getFloatOr(json, "startVal", defaults.startVal),
            getFloatOr(json, "startValRandFactor", defaults.startValRandFactor),
            getFloatOr(json, "factor", defaults.factor),
            getFloatOr(json, "randFactor", defaults.randFactor),
            getFloatOr(json, "exponent", defaults.exponent)
        );
    }

    public static <T extends Enum<T>> Optional<T> getEnumValue(JsonObject json, String field, Class<T> clazz) {
        return getString(json, field).map(s -> toEnumValue(s, clazz));
    }

    private static <T extends Enum<T>> T toEnumValue(String s, Class<T> clazz) {
        final T[] constants = clazz.getEnumConstants();
        return find(constants, v -> v.toString().equalsIgnoreCase(s)).orElseThrow(() -> {
            final String name = clazz.getSimpleName();
            final String values = Arrays.toString(constants);
            return runExF("{} \"{}\" does not exist. Valid options are: {}", name, s, values);
        });
    }

    /** Informs the user that they have entered an invalid biome name. */
    public static RuntimeException noBiomeNamed(String name) {
        return runExF("There is no biome named \"{}.\"", name);
    }

    /** Informs the user that they have entered an invalid biome ID. */
    public static RuntimeException noBiomeID(int ID) {
        return runExF("There is no biome with id \"%d.\"", ID);
    }

    /** Informs the user that they have entered an invalid block name. */
    public static RuntimeException noBlockNamed(String name) {
        return runExF("There is no block named \"{}.\"", name);
    }
}