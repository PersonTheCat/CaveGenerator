package com.personthecat.cavegenerator.util;

import com.personthecat.cavegenerator.world.WallDecorators;
import fastnoise.FastNoise;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.structure.template.PlacementSettings;
import net.minecraftforge.common.BiomeDictionary;
import org.apache.commons.io.output.NullWriter;
import org.hjson.HjsonOptions;
import org.hjson.JsonArray;
import org.hjson.JsonObject;
import org.hjson.JsonValue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.*;
import java.util.function.Consumer;

import static com.personthecat.cavegenerator.util.CommonMethods.*;

public class HjsonTools {
    /** The settings to be used when outputting JsonObjects to the disk. */
    private static final HjsonOptions FORMATTER = new HjsonOptions()
        .setAllowCompact(true)
        .setAllowMultiVal(true)
        .setCommentIndent(1)
        .setIndent(4)
        .setNlBraces(false);

    /** Writes the JsonObject to the disk. */
    public static Result<IOException> writeJson(JsonObject json, File file) {
        Result<IOException> result = Result.ok();
        Writer tw = new NullWriter();

        try {
           tw = new FileWriter(file);
           json.writeTo(tw, FORMATTER);
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

    /** Safely retrieves a boolean from the input object. */
    public static Optional<Boolean> getBool(JsonObject json, String field) {
        return Optional.ofNullable(json.get(field))
            .map(JsonValue::asBoolean);
    }

    /** Shorthand for getBool(json, field).ifPresent(ifPresent). */
    public static void getBool(JsonObject json, String field, Consumer<Boolean> ifPresent) {
        JsonValue value = json.get(field);
        if (value != null) {
            ifPresent.accept(value.asBoolean());
        }
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

    /** Shorthand for getInt(). */
    public static void getInt(JsonObject json, String field, Consumer<Integer> ifPresent) {
        JsonValue value = json.get(field);
        if (value != null) {
            ifPresent.accept(value.asInt());
        }
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

    /** Shorthand for getString(). */
    public static void getString(JsonObject json, String field, Consumer<String> ifPresent) {
        JsonValue value = json.get(field);
        if (value != null) {
            ifPresent.accept(value.asString());
        }
    }

    /** Retrieves a string from the input object. Returns `or` if nothing is found. */
    public static String getStringOr(JsonObject json, String field, String orElse) {
        return getString(json, field).orElse(orElse);
    }

    /** Safely retrieves a boolean from the input json. */
    public static Optional<JsonArray> getArray(JsonObject json, String field) {
        return Optional.ofNullable(json.get(field))
            .map(JsonValue::asArray);
    }

    /** Shorthand for getArray().*/
    public static void getArray(JsonObject json, String field, Consumer<JsonArray> ifPresent) {
        JsonValue value = json.get(field);
        if (value != null) {
            ifPresent.accept(value.asArray());
        }
    }

    /** Safely retrieves a boolean from the input json. */
    public static Optional<JsonObject> getObject(JsonObject json, String field) {
        return Optional.ofNullable(json.get(field))
            .map(JsonValue::asObject);
    }

    /** Shorthand for getObject(). */
    public static void getObject(JsonObject json, String field, Consumer<JsonObject> ifPresent) {
        JsonValue value = json.get(field);
        if (value != null) {
            ifPresent.accept(value.asObject());
        }
    }

    /**
     * Safely retrieves an array of JsonObjects from the input json.
     * To-do: Be more consistent and use Optional, instead.
     */
    public static List<JsonObject> getObjectArray(JsonObject json, String field) {
        List<JsonObject> array = new ArrayList<>();
        getArray(json, field, a -> a.forEach(e -> {
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

    /** Shorthand for getIntArray */
    public static void getIntArray(JsonObject json, String field, Consumer<int[]> ifPresent) {
        JsonValue value = json.get(field);
        if (value != null) {
            ifPresent.accept(toIntArray(value.asArray()));
        }
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
        Iterator<JsonValue> iter = array.iterator();
        while (iter.hasNext()) {
            ints.add(iter.next().asInt());
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
            .map((v) -> toStringArray(v.asArray()));
    }

    /** Shorthand for getStringArray(). */
    public static void getStringArray(JsonObject json, String field, Consumer<List<String>> ifPresent) {
        JsonValue value = json.get(field);
        if (value != null) {
            ifPresent.accept(toStringArray(value.asArray()));
        }
    }

    /** Converts a JsonArray into a List of Strings. */
    public static List<String> toStringArray(JsonArray array) {
        List<String> strings = new ArrayList<>();
        Iterator<JsonValue> iter = array.iterator();
        while (iter.hasNext()) {
            strings.add(iter.next().asString());
        }
        return strings;
    }

    /**
     * Gets the required "state" field which must exist in many objects.
     * Throws an exception when no block is found with the input name.
     */
    public static IBlockState getGuranteedState(JsonObject json, String requiredFor) {
        String stateName = getString(json, "state")
            .orElseThrow(() -> runExF("Each %s object must contain the field \"state.\"", requiredFor));
        return getBlockState(stateName)
            .orElseThrow(() -> noBlockNamed(stateName));
    }

    /**
     * Gets the required "states" field which must exist in many objects.
     * Throws an exception when any block cannot be found.
     */
    public static IBlockState[] getGuranteedStates(JsonObject json, String requiredFor) {
        JsonArray stateNames = getArray(json, "states")
            .orElseThrow(() -> runEx("Each WallDecorator object must contain the field \"states.\""));
        // Handles crashing when no block is found.
        return toBlocks(stateNames);
    }

    /** Retrieves a single IBlockState from the input json. */
    public static Optional<IBlockState> getBlock(JsonObject json, String field) {
        return getString(json, field)
            .map(s -> getBlockState(s)
                .orElseThrow(() -> noBlockNamed(s)));
    }

    /**
     * Retrives an IBlockState from the input json, returning `orElse`
     * if no object is found.
     */
    public static IBlockState getBlockOr(JsonObject json, String field, IBlockState orElse) {
        return getBlock(json, field).orElse(orElse);
    }

    /** Safely retrieves an array of blocks from the input json. */
    public static Optional<IBlockState[]> getBlocks(JsonObject json, String field) {
        return getArray(json, field).map(HjsonTools::toBlocks);
    }

    /** Shorthand for getBlocks(). */
    public static void getBlocks(JsonObject json, String field, Consumer<IBlockState[]> ifPresent) {
        getBlocks(json, field).ifPresent(ifPresent);
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

    /** Shorthand for getDirections(). */
    public static void getDirections(JsonObject json, String field, Consumer<Direction[]> ifPresent) {
        getDirections(json, field).ifPresent(ifPresent);
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
    public static Optional<WallDecorators.Preference> getPreference(JsonObject json, String field) {
        return getString(json, field).map(WallDecorators.Preference::from);
    }

    /**
     * Retrieves a Preference object from the input json, substituting
     * `orElse` if no object can be found.
     */
    public static WallDecorators.Preference getPreferenceOr(JsonObject json, String field, WallDecorators.Preference orElse) {
        return getPreference(json, field).orElse(orElse);
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

    /** Shorthand for getPositions(). */
    public static void getPositions(JsonObject json, String field, Consumer<BlockPos[]> ifPresent) {
        getPositions(json, field).ifPresent(ifPresent);
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
        getStringArray(json, "names").ifPresent((a) -> {
            for (String s : a) {
                Biome biome = getBiome(s).orElseThrow(() -> noBiomeNamed(s));
                biomes.add(biome);
            }
        });
        // Get biomes by ID.
        getIntArray(json, "IDs").ifPresent((a) -> {
            for (int i : a) {
                Biome biome = getBiome(i).orElseThrow(() -> noBiomeID(i));
                biomes.add(biome);
            }
        });
        // Get biomes by type.
        getBiomeTypes(json, "types").ifPresent((a) -> {
            for (BiomeDictionary.Type t : a) {
                Collections.addAll(biomes, getBiomes(t));
            }
        });
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
        Iterator<JsonValue> iter = array.iterator();
        while (iter.hasNext()) {
            types.add(getBiomeType(iter.next().asString()));
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
        // Access the object with name `field`. If it exists, get whichever
        // values are present in the json, substituting from `defaults`,
        // if the value does not exist.
        return getObject(json, field).map(o -> ScalableFloat.fromDefaults(
            defaults,
            getFloat(o, "exponent"),
            getFloat(o, "factor"),
            getFloat(o, "randFactor"),
            getFloat(o, "startVal"),
            getFloat(o, "startValRandFactor")
        ));
    }

    /** Retrieves a scalable float from the input json. Returns the default values when no object is found. */
    public static ScalableFloat getScalableFloatOr(JsonObject json, String field, ScalableFloat defaults) {
        return getScalableFloat(json, field, defaults).orElse(defaults);
    }

    /**
     * Safely retrieves a NoiseSettings3D object from the input json,
     * substituting defaults necessary.
     */
    public static NoiseSettings3D getNoiseSettingsOr(JsonObject json, String field, NoiseSettings3D defaults) {
        return getObject(json, field)
            .map(o -> toNoiseSettings(o, defaults))
            .orElse(defaults);
    }

    /** Safely retrieves a NoiseSettings2D object from the input json. */
    public static NoiseSettings2D getNoiseSettingsOr(JsonObject json, String field, NoiseSettings2D defaults) {
        return getObject(json, field)
            .map(o -> toNoiseSettings(o, defaults))
            .orElse(defaults);
    }

    /** Converts the input json into a NoiseSettings3D object. */
    public static NoiseSettings3D toNoiseSettings(JsonObject json, NoiseSettings3D defaults) {
        float frequency = getFloat(json, "frequency").orElse(defaults.getFrequency());
        float scale = getFloat(json, "scale").orElse(defaults.getScale());
        float scaleY = getFloat(json, "scaleY").orElse(defaults.getScaleY());
        int octaves = getInt(json, "octaves").orElse(defaults.getOctaves());
        FastNoise.NoiseType type = getString(json, "type").map(HjsonTools::noiseType)
            .orElse(defaults.getNoiseType());
        FastNoise.FractalType fractal = getString(json, "fractal").map(HjsonTools::fractalType)
            .orElse(defaults.getFractalType());
        return new NoiseSettings3D(frequency, scale, scaleY, octaves, type, fractal);
    }

    /** Converts the input json into a NoiseSettings2D object. */
    public static NoiseSettings2D toNoiseSettings(JsonObject json, NoiseSettings2D defaults) {
        float frequency = getFloat(json, "frequency").orElse(defaults.frequency);
        float scale = getFloat(json, "scale").orElse(defaults.getScale());
        int min = getInt(json, "min").orElse(defaults.min);
        int max = getInt(json, "max").orElse(defaults.max);
        return new NoiseSettings2D(frequency, scale, min, max);
    }

    public static FastNoise.NoiseType noiseType(String s) {
        Optional<FastNoise.NoiseType> dir = find(FastNoise.NoiseType.values(), (v) -> v.toString().equalsIgnoreCase(s));
        return dir.orElseThrow(() -> {
            final String o = Arrays.toString(FastNoise.NoiseType.values());
            return runExF("Error: NoiseType \"%s\" does not exist. The following are valid options:\n\n", s, o);
        });
    }

    public static FastNoise.FractalType fractalType(String s) {
        Optional<FastNoise.FractalType> dir = find(FastNoise.FractalType.values(), (v) -> v.toString().equalsIgnoreCase(s));
        return dir.orElseThrow(() -> {
            final String o = Arrays.toString(FastNoise.FractalType.values());
            return runExF("Error: FractalType \"%s\" does not exist. The following are valid options:\n\n", s, o);
        });
    }


    /** Informs the user that they have entered an invalid biome name. */
    public static RuntimeException noBiomeNamed(String name) {
        return runExF("There is no biome named \"%s.\"", name);
    }

    /** Informs the user that they have entered an invalid biome ID. */
    public static RuntimeException noBiomeID(int ID) {
        return runExF("There is no biome with id \"%d.\"", ID);
    }

    /** Informs the user that they have entered an invalid block name. */
    public static RuntimeException noBlockNamed(String name) {
        return runExF("There is no block named \"%s.\"", name);
    }
}