package com.personthecat.cavegenerator.util;

import com.personthecat.cavegenerator.model.Direction;
import com.personthecat.cavegenerator.model.NoiseSettings2D;
import com.personthecat.cavegenerator.model.NoiseSettings3D;
import com.personthecat.cavegenerator.model.ScalableFloat;
import com.personthecat.cavegenerator.world.generator.WallDecorator;
import fastnoise.FastNoise.*;
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

import static com.personthecat.cavegenerator.util.CommonMethods.*;

@SuppressWarnings("WeakerAccess")
public class HjsonTools {

    /** The settings to be used when outputting JsonObjects to the disk. */
    private static final HjsonOptions FORMATTER = new HjsonOptions()
        .setAllowCondense(true)
        .setAllowMultiVal(true)
        .setCommentSpace(0)
        .setSpace(2)
        .setBracesSameLine(true)
        .setOutputComments(true)
        .setOutputEmptyLines(true);

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
        return Optional.ofNullable(json.get(field))
            .map(JsonValue::asBoolean);
    }

    /** Retrieves a boolean from the input object. Returns `or` if nothing is found. */
    public static boolean getBoolOr(JsonObject json, String field, boolean orElse) {
        return getBool(json, field).orElse(orElse);
    }

    /** Safely retrieves an integer from the input json. */
    public static Optional<Integer> getInt(JsonObject json, String field) {
        return Optional.ofNullable(json.get(field))
            .map(JsonValue::asInt);
    }

    /** Retrieves an integer from the input object. Returns `or` if nothing is found. */
    public static int getIntOr(JsonObject json, String field, int orElse) {
        return getInt(json, field).orElse(orElse);
    }

    /** Safely retrieves a boolean from the input json. */
    public static Optional<Float> getFloat(JsonObject json, String field) {
        return Optional.ofNullable(json.get(field))
            .map(JsonValue::asFloat);
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
        return Optional.ofNullable(json.get(field))
            .map(JsonValue::asString);
    }

    /** Safely retrieves a JsonArray from the input json. */
    public static Optional<JsonArray> getArray(JsonObject json, String field) {
        return Optional.ofNullable(json.get(field))
            .map(HjsonTools::asOrToArray);
    }

    /**  Retrieves an array or creates a new one, if absent. */
    public static JsonArray getArrayOrNew(JsonObject json, String field) {
        if (!json.has(field)) {
            json.set(field, new JsonArray());
        }
        return getArray(json, field).orElseThrow(() -> runEx("Unreachable."));
    }

    /** Retrieves an array, casts or converts if not an array, creates if absent. */
    public static JsonArray getForceArray(JsonObject json, String field) {
        if (!json.has(field)) {
            json.set(field, new JsonArray());
        }
        return getValue(json, field).map(HjsonTools::asOrToArray)
            .orElseThrow(() -> runEx("Unreachable."));
    }

    /** Casts or converts a JsonValue to a JsonArray.*/
    public static JsonArray asOrToArray(JsonValue value) {
        return value.isArray() ? value.asArray() : new JsonArray().add(value);
    }

    /** Safely retrieves a boolean from the input json. */
    public static Optional<JsonObject> getObject(JsonObject json, String field) {
        return Optional.ofNullable(json.get(field))
            .map(JsonValue::asObject);
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

    /** Safely retrieves an int array from the input json. */
    public static Optional<int[]> getIntArray(JsonObject json, String field) {
        return Optional.ofNullable(json.get(field))
            .map((v) -> toIntArray(v.asArray()));
    }

    /** Retrieves an array of integers from the input object. Returns `or` if nothing is found. */
    public static int[] getIntArrayOr(JsonObject json, String field, int[] orElse) {
        return getIntArray(json, field).orElse(orElse);
    }

    /** Converts a JsonArray into an array of ints. */
    public static int[] toIntArray(JsonArray array) {
        // Create a List of Integer objects.
        List<Integer> ints = new ArrayList<>();
        // Iterate through the array, adding to the list.
        for (JsonValue value : array) {
            ints.add(value.asInt());
        }
        // Convert the Integer objects into
        // their primitive counterparts.
        return ints.stream()
            .mapToInt((i) -> i)
            .toArray();
    }

    /** Safely retrieves a List of Strings from the input json. */
    public static Optional<List<String>> getStringArray(JsonObject json, String field) {
        return Optional.ofNullable(json.get(field))
            .map(v -> toStringArray(asOrToArray(v)));
    }

    /** Converts a JsonArray into a List of Strings. */
    public static List<String> toStringArray(JsonArray array) {
        List<String> strings = new ArrayList<>();
        for (JsonValue value : array) {
            strings.add(value.asString());
        }
        return strings;
    }

    /**
     * Gets the required "state" field which must exist in many objects.
     * Throws an exception when no block is found with the input name.
     */
    public static IBlockState getGuaranteedState(JsonObject json, String requiredFor) {
        String stateName = getString(json, "state")
            .orElseThrow(() -> runExF("Each {} object must contain the field \"state.\"", requiredFor));
        return getBlockState(stateName)
            .orElseThrow(() -> noBlockNamed(stateName));
    }

    /**
     * Gets the required "states" field which must exist in many objects.
     * Throws an exception when any block cannot be found.
     */
    public static IBlockState[] getGuaranteedStates(JsonObject json, String requiredFor) {
        JsonArray stateNames = getArray(json, "states")
            .orElseThrow(() -> runExF("Each {} object must contain the field \"states.\"", requiredFor));
        // Handles crashing when no block is found.
        return toBlocks(stateNames);
    }

    /** Retrieves a single IBlockState from the input json. */
    public static Optional<IBlockState> getBlock(JsonObject json, String field) {
        return getString(json, field)
            .map(s -> getBlockState(s)
                .orElseThrow(() -> noBlockNamed(s)));
    }

    /** Safely retrieves an array of blocks from the input json. */
    public static Optional<IBlockState[]> getBlocks(JsonObject json, String field) {
        return getArray(json, field).map(HjsonTools::toBlocks);
    }

    /** Converts each element in the array into an IBlockState. */
    public static IBlockState[] toBlocks(JsonArray array) {
        List<IBlockState> blocks = new ArrayList<>();
        for (String s : toStringArray(array)) {
            IBlockState state = getBlockState(s).orElseThrow(() -> noBlockNamed(s));
            blocks.add(state);
        }
        return blocks.toArray(new IBlockState[0]);
    }

    /**
     * Retrieves an array of IBlockStates from the input json, substituting
     * `orElse` if no object is found.
     */
    public static IBlockState[] getBlocksOr(JsonObject json, String field, IBlockState... orElse) {
        return getBlocks(json, field).orElse(orElse);
    }

    /** Safely retrieves an array of type Direction from the input json. */
    public static Optional<Direction[]> getDirections(JsonObject json, String field) {
        Optional<List<String>> array = getStringArray(json, field);
        if (array.isPresent()) {
            List<Direction> directions = new ArrayList<>();
            for (String s : array.get()) {
                Direction d = Direction.from(s);
                directions.add(d);
            }
            return full(toArray(directions, Direction.class));
        }
        return empty();
    }

    /**
     * Retrieves an array of type `Direction` from the input json.
     * returns `orElse` if no object is found.
     */
    public static Direction[] getDirectionsOr(JsonObject json, String field, Direction... orElse) {
        return getDirections(json, field).orElse(orElse);
    }

    /** Safely retrieves a BlockPos from the input object. */
    public static Optional<BlockPos> getPosition(JsonObject json, String field) {
        return getArray(json, field).map(HjsonTools::toPosition);
    }

    /** Safely retrieves a Preference object from the input json. */
    public static Optional<WallDecorator.Preference> getPreference(JsonObject json, String field) {
        return getString(json, field).map(WallDecorator.Preference::from);
    }

    /**
     * Retrieves a BlockPos from the input json, returning `orElse`
     * if no object can be found.
     */
    public static BlockPos getPositionOr(JsonObject json, String field, BlockPos orElse) {
        return getPosition(json, field).orElse(orElse);
    }

    /** Safely retrieves an array of type BlockPos from the input json. */
    public static Optional<BlockPos[]> getPositions(JsonObject json, String field) {
        return getArray(json, field).map(a -> {
            List<BlockPos> positions = new ArrayList<>();
            for (JsonValue v : a) {
                positions.add(toPosition(v.asArray()));
            }
            return toArray(positions, BlockPos.class);
        });
    }

    /**
     * Retrieves an array of type `BlockPos` from the input object,
     * returning `orElse` if no object is found.
     */
    public static BlockPos[] getPositionsOr(JsonObject json, String field, BlockPos... orElse) {
        return getPositions(json, field).orElse(orElse);
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

    /** For the biome object at the top level. */
    public static Biome[] getAllBiomes(JsonObject json) {
        List<Biome> biomes = new ArrayList<>();
        // Get biomes by registry name.
        for (String s : toStringArray(getForceArray(json, "names"))) {
            biomes.add(getBiome(s).orElseThrow(() -> noBiomeNamed(s)));
        }
        // Get biomes by ID.
        for (int i : toIntArray(getForceArray(json, "IDs"))) {
            biomes.add(getBiome(i).orElseThrow(() -> noBiomeID(i)));
        }
        // Get biomes by type.
        for (BiomeDictionary.Type t : toBiomeTypes(getForceArray(json, "types"))) {
            Collections.addAll(biomes, getBiomes(t));
        }
        return toArray(biomes, Biome.class);
    }

    /** Safely retrieves a List of BiomeTypes from the input json. */
    public static Optional<List<BiomeDictionary.Type>> getBiomeTypes(JsonObject json, String field) {
        return Optional.ofNullable(json.get(field))
            .map(v -> toBiomeTypes(v.asArray()));
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
            if (v.isObject()) {
                return toScalableFloat(v.asObject(), defaults);
            }
            return toScalableFloat(v.asArray(), defaults);
        });
    }

    /** Retrieves a scalable float from the input json. Returns the default values when no object is found. */
    public static ScalableFloat getScalableFloatOr(JsonObject json, String field, ScalableFloat defaults) {
        return getScalableFloat(json, field, defaults).orElse(defaults);
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

    /** Safely retrieves a NoiseSettings2D object from the input json. */
    public static NoiseSettings2D getNoiseSettingsOr(JsonObject json, String field, NoiseSettings2D defaults) {
        return getObject(json, field)
            .map(o -> toNoiseSettings(o, defaults))
            .orElse(defaults);
    }

    /** Converts the input json into a NoiseSettings3D object. */
    public static NoiseSettings3D toNoiseSettings(JsonObject json, NoiseSettings3D defaults) {
        final NoiseSettings3D.NoiseSettings3DBuilder builder = defaults.toBuilder()
            .seed(getInt(json, "seed"));

        // Initialize these with defaults, if applicable.
        getFloat(json, "jitter").ifPresent(jitter -> {
            builder.jitterX(jitter);
            builder.jitterY(jitter);
            builder.jitterZ(jitter);
        });

        getFloat(json, "frequency").ifPresent(builder::frequency);
        getFloat(json, "scale").ifPresent(builder::scale);
        getFloat(json, "scaleY").ifPresent(builder::scaleY);
        getFloat(json, "lacunarity").ifPresent(builder::lacunarity);
        getFloat(json, "gain").ifPresent(builder::gain);
        getFloat(json, "perturbAmp").ifPresent(builder::perturbAmp);
        getFloat(json, "perturbFreq").ifPresent(builder::perturbFreq);
        getFloat(json, "jitterX").ifPresent(builder::jitterX);
        getFloat(json, "jitterY").ifPresent(builder::jitterY);
        getFloat(json, "jitterZ").ifPresent(builder::jitterZ);
        getInt(json, "octaves").ifPresent(builder::octaves);
        getInt(json, "offset").ifPresent(builder::offset);
        getBool(json, "perturb").ifPresent(builder::perturb);
        getBool(json, "invert").ifPresent(builder::invert);
        getString(json, "interp").map(HjsonTools::interp).ifPresent(builder::interp);
        getString(json, "type").map(HjsonTools::noiseType).ifPresent(builder::noiseType);
        getString(json, "fractal").map(HjsonTools::fractalType).ifPresent(builder::fractalType);
        getString(json, "distFunc").map(HjsonTools::distanceFunction).ifPresent(builder::distanceFunction);
        getString(json, "returnType").map(HjsonTools::returnType).ifPresent(builder::returnType);
        getString(json, "cellularLookup").map(HjsonTools::noiseType).ifPresent(builder::cellularLookup);

        return builder.build();
    }

    /** Retrieves either an array of noise settings or a single value containing `defaults`. */
    public static NoiseSettings3D[] getNoiseArray(JsonObject json, String field, NoiseSettings3D defaults) {
        List<NoiseSettings3D> noise = new ArrayList<>();
        JsonArray array = getValue(json, field)
            .map(HjsonTools::asOrToArray)
            .orElse(new JsonArray().add(new JsonObject()));
        for (JsonValue value : array) {
            noise.add(toNoiseSettings(value.asObject(), defaults));
        }
        return toArray(noise, NoiseSettings3D.class);
    }

    /** Converts the input json into a NoiseSettings2D object. */
    public static NoiseSettings2D toNoiseSettings(JsonObject json, NoiseSettings2D defaults) {
        final NoiseSettings2D.NoiseSettings2DBuilder builder = defaults.toBuilder()
            .seed(getInt(json, "seed"));

        getFloat(json, "frequency").ifPresent(builder::frequency);
        getFloat(json, "scale").ifPresent(builder::scale);
        getInt(json, "minVal").ifPresent(builder::min);
        getInt(json, "maxVal").ifPresent(builder::max);

        return builder.build();
    }

    public static Interp interp(String s) {
        Optional<Interp> dir = find(Interp.values(), (v) -> v.toString().equalsIgnoreCase(s));
        return dir.orElseThrow(() -> {
            final String o = Arrays.toString(Interp.values());
            return runExF("Error: Interp \"{}\" does not exist. The following are valid options:\n\n", s, o);
        });
    }

    public static NoiseType noiseType(String s) {
        Optional<NoiseType> dir = find(NoiseType.values(), (v) -> v.toString().equalsIgnoreCase(s));
        return dir.orElseThrow(() -> {
            final String o = Arrays.toString(NoiseType.values());
            return runExF("Error: NoiseType \"{}\" does not exist. The following are valid options:\n\n", s, o);
        });
    }

    public static FractalType fractalType(String s) {
        Optional<FractalType> dir = find(FractalType.values(), (v) -> v.toString().equalsIgnoreCase(s));
        return dir.orElseThrow(() -> {
            final String o = Arrays.toString(FractalType.values());
            return runExF("Error: FractalType \"{}\" does not exist. The following are valid options:\n\n", s, o);
        });
    }

    public static CellularDistanceFunction distanceFunction(String s) {
        Optional<CellularDistanceFunction> dir = find(CellularDistanceFunction.values(), (v) -> v.toString().equalsIgnoreCase(s));
        return dir.orElseThrow(() -> {
            final String o = Arrays.toString(CellularDistanceFunction.values());
            return runExF("Error: CellularDistanceFunction \"{}\" does not exist. The following are valid options:\n\n", s, o);
        });
    }

    public static CellularReturnType returnType(String s) {
        Optional<CellularReturnType> dir = find(CellularReturnType.values(), (v) -> v.toString().equalsIgnoreCase(s));
        return dir.orElseThrow(() -> {
            final String o = Arrays.toString(CellularReturnType.values());
            return runExF("Error: CellularReturnType \"{}\" does not exist. The following are valid options:\n\n", s, o);
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