package com.personthecat.cavegenerator.world;

import com.personthecat.cavegenerator.config.ConfigFile;
import com.personthecat.cavegenerator.util.Lazy;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeProvider;

import java.util.function.Predicate;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BiomeSearch {
    public final Lazy<Biome[]> current;
    public final Lazy<Data[]> surrounding;

    /** Checks the surrounding biomes for the first match when given a predicate. */
    public boolean anyMatches(Predicate<Biome> predicate) {
        for (Biome b : current.get()) {
            if (predicate.test(b)) {
                return true;
            }
        }
        return false;
    }

    /** A public accessor to calculate the current biome array size. */
    public static int size() {
        final int d = ConfigFile.biomeRange * 2 + 1;
        return d * d;
    }

    /** Acquires biomes at the four corners of this chunk. */
    public static BiomeSearch in(World world, int x, int z) {
        final Lazy<Biome[]> current = Lazy.of(() -> inner(world, x, z));
        final Lazy<Data[]> surrounding = Lazy.of(() -> outer(world, x, z, ConfigFile.biomeRange));
        return new BiomeSearch(current, surrounding);
    }

    /** Accumulates a list of biomes at the four corners of this chunk. */
    private static Biome[] inner(World world, int x, int z) {
        final BiomeProvider provider = world.getBiomeProvider();
        final int actualX = x << 4;
        final int actualZ = z << 4;
        // This is only used for early generators, at which point the current
        // chunk does not yet exist. As a result, this is more direct.
        return new Biome[] {
            provider.getBiome(new BlockPos(actualX + 1, 0, actualZ + 1)),
            provider.getBiome(new BlockPos(actualX + 1, 0, actualZ + 14)),
            provider.getBiome(new BlockPos(actualX + 14, 0, actualZ + 1)),
            provider.getBiome(new BlockPos(actualX + 14, 0, actualZ + 14))
        };
    }

    /** Checks outward in a range of <code>r</code> for surrounding center biomes. */
    private static Data[] outer(World world, int x, int z, int r) {
        final int d = r * 2 + 1;
        final Data[] biomes = new Data[d * d];
        int index = 0;
        for (int cX = x - r; cX <= x + r; cX++) {
            for (int cZ = z - r; cZ <= z + r; cZ++) {
                biomes[index++] = Data.create(world, cX, cZ);
            }
        }
        return biomes;
    }

    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Data {
        public final Biome biome;
        public final int chunkX;
        public final int chunkZ;
        public final int centerX;
        public final int centerZ;

        private static Data create(World world, int chunkX, int chunkZ) {
            final int centerX = (chunkX << 4) + 8;
            final int centerZ = (chunkZ << 4) + 8;
            final Biome biome = world.getBiomeProvider().getBiome(new BlockPos(centerX, 0, centerZ));
            return new Data(biome, chunkX, chunkZ, centerX, centerZ);
        }
    }
}
