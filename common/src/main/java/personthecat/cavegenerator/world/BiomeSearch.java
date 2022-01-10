package personthecat.cavegenerator.world;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import personthecat.catlib.data.Lazy;
import personthecat.cavegenerator.config.Cfg;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BiomeSearch {
    public final Lazy<Biome[]> current;
    public final Lazy<Data[]> surrounding;

    /**
     * Checks the surrounding biomes for the first match when given a predicate.
     *
     * @param predicate The rule set which determines if a biome is valid.
     */
    public boolean anyMatches(final Predicate<Biome> predicate) {
        for (final Biome b : this.current.get()) {
            if (predicate.test(b)) {
                return true;
            }
        }
        return false;
    }

    /**
     * A public accessor to calculate the current biome array size.
     *
     * The size of the array in regular units.
     */
    public static int size() {
        final int d = Cfg.biomeRange() * 2 + 1;
        return d * d;
    }

    /**
     * Acquires all biomes at the four corners of this chunk.
     *
     * @param biomes A biome provider.
     * @param x The x chunk coordinate.
     * @param z The z chunk coordinate.
     */
    public static BiomeSearch in(final BiomeManager biomes, final int x, final int z) {
        final Lazy<Biome[]> current = Lazy.of(() -> inner(biomes, x, z));
        final Lazy<Data[]> surrounding = Lazy.of(() -> outer(biomes, x, z));
        return new BiomeSearch(current, surrounding);
    }

    /**
     * Gets an array of biomes representing the four corners of this chunk. In most cases,
     * this will provide a sufficient indication of whether a generator is valid in the
     * current chunk.
     *
     * @param biomes Whichever biome provider is currently available.
     * @param x The x chunk coordinate.
     * @param z The z chunk coordinate.
     * @return An array of <em>up to</em> 4 biomes.
     */
    private static Biome[] inner(final BiomeManager biomes, final int x, final int z) {
        final int actualX = x << 4;
        final int actualZ = z << 4;

        // Removes redundant entries.
        final Set<Biome> set = new HashSet<>(4);
        set.add(biomes.getBiome(new BlockPos(actualX + 1, 63, actualZ + 1)));
        set.add(biomes.getBiome(new BlockPos(actualX + 1, 63, actualZ + 14)));
        set.add(biomes.getBiome(new BlockPos(actualX + 14, 63, actualZ + 1)));
        set.add(biomes.getBiome(new BlockPos(actualX + 14, 63, actualZ + 14)));
        return set.toArray(new Biome[0]);
    }

    /**
     * Gets an array of biomes and their corresponding chunk coordinates surrounding the
     * current chunk. In the future, we will modify this method to search upward at a
     * configurable interval in order to support 3-dimensional biome maps.
     *
     * @param biomes Whichever biome provider is currently available.
     * @param x The x chunk coordinate.
     * @param z The z chunk coordinate.
     * @return An array containing biome info.
     */
    private static Data[] outer(final BiomeManager biomes, final int x, final int z) {
        final int r = Cfg.biomeRange();
        final int d = r * 2 + 1;
        final Data[] data = new Data[d * d];
        int index = 0;
        for (int cX = x - r; cX <= x + r; cX++) {
            for (int cZ = z - r; cZ <= z + r; cZ++) {
                data[index++] = Data.create(biomes, cX, cZ);
            }
        }
        return data;
    }

    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Data {
        public final Biome biome;
        public final int chunkX;
        public final int chunkZ;
        public final int centerX;
        public final int centerZ;

        private static Data create(final BiomeManager biomes, final int chunkX, final int chunkZ) {
            final int centerX = (chunkX << 4) + 8;
            final int centerZ = (chunkZ << 4) + 8;
            final Biome biome = biomes.getBiome(new BlockPos(centerX, 63, centerZ));
            return new Data(biome, chunkX, chunkZ, centerX, centerZ);
        }
    }
}
