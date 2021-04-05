package com.personthecat.cavegenerator.noise;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CachedNoiseHelper {

    private static final Map<Integer, Cache> DATA = new ConcurrentHashMap<>();

    public static Cache getOrCreate(int hash) {
        final Cache pull = DATA.get(hash);
        if (pull != null) {
            return pull;
        }
        final Cache push = new Cache();
        DATA.put(hash, push);
        return push;
    }

    public static void resetAll() {
        DATA.forEach((i, c) -> c.reset());
    }

    public static void removeAll() {
        DATA.clear();
    }

    /**
     * Callers must be careful to ensure boundaries are not exceeded.
     *
     * It is also important to understand that this is <em>not a thread-safe implementation
     * </em>. Additional work would be needed to ensure that concurrent write operations do
     * not corrupt the data. This feature is left out here because concurrent writes never
     * have the chance to occur.
     */
    public static class Cache {
        private final float[] output3 = new float[16 * 16 * 256];
        private final float[] output2 = new float[16 * 16];

        public float getNoise(int x, int y, int z) {
            return output3[x << 12 | z << 8 | y];
        }

        public float getNoise(int x, int z) {
            return output2[x << 4 | z];
        }

        public void writeNoise(int x, int y, int z, float noise) {
            output3[x << 12 | z << 8 | y] = noise;
        }

        public void writeNoise(int x, int z, float noise) {
            output2[x << 4 | z] = noise;
        }

        private void reset() {
            Arrays.fill(output3, 0F);
            Arrays.fill(output2, 0F);
        }
    }

}
