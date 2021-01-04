package com.personthecat.cavegenerator.util;

import com.personthecat.cavegenerator.Main;

import java.io.File;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.BiomeDictionary.Type;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Level;

import static com.personthecat.cavegenerator.io.SafeFileIO.*;

/**
 * A collection of methods and functions to be imported into
 * most classes throughout the mod for syntactic clarity
 */
public class CommonMethods {

    /** The directory where all file backups will be stored. */
    private static final File BACKUP_DIR = new File(Loader.instance().getConfigDir(), "cavegenerator/backup");

    /*
     * ////////////////////////////////////////////////////////////////////////
     *         Shorthand methods to be used throughout the program.
     *         Should improve readability by reducing boilerplate.
     * ////////////////////////////////////////////////////////////////////////
     */

    /** Accesses the mod's main instance to send a message using its logger. */
    public static void info(String x, Object... args) {
        Main.instance.logger.info(x, args);
    }

    /** Accesses the mod's main instance to debug using its logger. */
    public static void debug(String x, Object... args) {
        Main.instance.logger.debug(x, args);
    }

    /** Accesses the mod's main instance to send a warning using its logger. */
    public static void warn(String x, Object... args) {
        Main.instance.logger.warn(x, args);
    }

    /** Accesses the mod's main instance to send an error using its logger. */
    public static void error(String x, Object... args) {
        Main.instance.logger.error(x, args);
    }

    /** Accesses the mod's main instance to log information using its logger. */
    public static void log(Level level, String x, Object... args) {
        if (level.equals(Level.FATAL)) {
            throw runExF(x, args);
        }
        Main.instance.logger.log(level, x, args);
    }

    /** Returns a clean-looking, general-purpose RuntimeException. */
    public static RuntimeException runEx(String x) {
        return new RuntimeException(x);
    }

    /** Shorthand for a RuntimeException using String#format. */
    public static RuntimeException runExF(String x, Object... args) {
        return new RuntimeException(String.format(x, args));
    }

    /*
     * ////////////////////////////////////////////////////////////////////////
     *                  Common, general-purpose functions.
     * ////////////////////////////////////////////////////////////////////////
     */

    /**
     * Uses a linear search algorithm to locate a value in an array,
     * matching the predicate `by`. Shorthand for Stream#findFirst.
     *
     * Example:
     *  // Find x by x.name
     *  Object[] vars = getObjectsWithNames();
     *  Optional<Object> var = find(vars, (x) -> x.name.equals("Cat"));
     *  // You can then get the value -> NPE
     *  Object result = var.get()
     *  // Or use an alternative. Standard java.util.Optional. -> no NPE
     *  Object result = var.orElse(new Object("Cat"))
     */
    public static <T> Optional<T> find(T[] values, Predicate<T> by) {
        for (T val : values) {
            if (by.test(val)) {
                return full(val);
            }
        }
        return empty();
    }

    public static <T> Optional<T> find(Collection<T> values, Predicate<T> by) {
        for (T val : values) {
            if (by.test(val)) {
                return full(val);
            }
        }
        return empty();
    }

    /**
     * Converts a generic List into its standard array counterpart.
     * Unsafe. Should not be used for any primitive data type. In
     * Most cases where this method is used, storing the data in a
     * primitive array probably offers little or no benefit. As a
     * result, I may try to remove this sometime in the near future.
     */
    @SuppressWarnings("unchecked")
    public static <T extends Object> T[] toArray(List<T> list, Class<T> clazz) {
        return list.toArray((T[]) Array.newInstance(clazz, 0));
    }

    /** Safely retrieves a value from the input map. */
    public static <K, V> Optional<V> safeGet(Map<K, V> map, K key) {
        return Optional.ofNullable(map.get(key));
    }

    /** Determines the extension of the input `file`. */
    public static String extension(final File file) {
        String[] split = file.getName().split(Pattern.quote("."));
        return split[split.length - 1];
    }

    /** Copies a file to the backup directory. */
    public static void backup(File file) {
        final File backup = new File(BACKUP_DIR, file.getName());
        if (!safeFileExists(BACKUP_DIR, "Unable to handle backup directory.")) {
            safeMkdirs(BACKUP_DIR);
        }
        if (safeFileExists(backup, "Unable to handle existing backup file.")) {
            backup.delete();
        }
        safeCopy(file, BACKUP_DIR).throwIfPresent();
    }

    /** Shorthand for calling Optional#empty. */
    public static <T> Optional<T> empty() {
        return Optional.empty();
    }

    /**
     * Shorthand for calling Optional#of, matching the existing syntax of
     * `empty`, while being more clear than `of` alone.
     */
    public static <T> Optional<T> full(T val) {
        return Optional.of(val);
    }

    public static int getMin(int a, int b) {
        return a < b ? a : b;
    }

    public static int getMax(int a, int b) {
        return a > b ? a : b;
    }

    /** Divides 1 / `value` without any divide by zero errors or unsightly casting. */
    public static int invert(double value) {
        return value == 0 ? Integer.MAX_VALUE : (int) (1 / value);
    }

    /*
     * ///////////////////////////////////////////////////////////////////////
     *                   Functions related to Forge / MC
     * ///////////////////////////////////////////////////////////////////////
     */

    /**
     * Used for retrieving a Biome from either a registry name
     * or unique ID. Returns an Optional<Biome> to ensure that
     * null checks are propagated elsewhere.
     */
    public static Optional<Biome> getBiome(String biomeName) {
        return Optional.ofNullable(ForgeRegistries.BIOMES.getValue(new ResourceLocation(biomeName)));
    }

    public static Optional<Biome> getBiome(int biomeNumber) {
        return Optional.ofNullable((Biome.getBiomeForId(biomeNumber)));
    }

    public static Biome[] getBiomes(Type biomeType) {
        return BiomeDictionary.getBiomes(biomeType).toArray(new Biome[0]);
    }

    public static Type getBiomeType(String name) {
        return Type.getType(name);
    }

    /**
     * Variant of ForgeRegistries::BLOCKS#getValue that does not substitute
     * air for blocks that aren't found. Using Optional to improve null-safety.
     */
    public static Optional<IBlockState> getBlockState(String registryName) {
        // Ensure that air is returned if that is the query.
        if (registryName.equals("air") || registryName.equals("minecraft:air")) {
            return full(Blocks.AIR.getDefaultState());
        }

        // View the components of this string separately.
        final String[] split = registryName.split(":");

        // Ensure the number of segments to be valid.
        if (!(split.length > 0 && split.length < 4)) {
            throw runExF("Syntax error: could not determine blockstate from %s", registryName);
        }

        // Use the end section to determine the format.
        final String end = split[split.length - 1];

        // If the end of the string is numeric, it must be the metadata.
        if (StringUtils.isNumeric(end)) {
            final int meta = Integer.parseInt(end);
            final String updated = registryName.replace(":" + end, "");
            return _getBlock(updated, meta);
        }
        // The end isn't numeric, so the name is in the standard format.
        return _getBlock(registryName, 0);
    }

    /**
     * Internal variant of ForgeRegistries::BLOCKS#getValue that does not
     * return air. This ensures that a valid block has always been determined,
     * except of course in cases where that block is air.
     */
    @SuppressWarnings("deprecation") // No good alternative in 1.12. Just ignoring this.
    private static Optional<IBlockState> _getBlock(String registryName, int meta) {
        final ResourceLocation location = new ResourceLocation(registryName);
        final IBlockState ret;
        try { // Block#getStateFromMeta may throw a NullPointerException. Extremely annoying.
            ret = ForgeRegistries.BLOCKS.getValue(location).getStateFromMeta(meta);
        } catch (NullPointerException e) {
            return empty();
        }
        // Ensure this value to be anything but air.
        if (ret.equals(Blocks.AIR.getDefaultState())){
            return empty();
        }
        return full(ret);
    }

    /** Shorthand for retrieving state variants directly from a block. */
    public static <T extends Comparable<T>, V extends T> IBlockState getVariant(Block block, IProperty<T> property, V value) {
        return block.getDefaultState().withProperty(property, value);
    }

    /** Returns the center block in the specified chunk */
    public static BlockPos centerCoords(int chunkX, int chunkZ) {
        return new BlockPos((chunkX * 16) + 8, 0, (chunkZ * 16) + 8);
    }

    /** Returns the absolute position in the specified chunk */
    public static BlockPos absoluteCoords(int chunkX, int chunkZ) {
        return new BlockPos(chunkX * 16, 0, chunkZ * 16);
    }
}