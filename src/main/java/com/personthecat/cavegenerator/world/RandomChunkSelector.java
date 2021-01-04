package com.personthecat.cavegenerator.world;

public class RandomChunkSelector {

    /** The hasher to be used for selecting chunks. */
    private final HashGenerator noise;

    public RandomChunkSelector(Long worldSeed) {
        this.noise = new HashGenerator(worldSeed);
    }

    /**
     * Obtain a random value from the three inputs using HashGenerator.
     * The threshold reflects the probability of selection.
     */
    public boolean getBooleanForCoordinates(int ID, int x, int y, double threshold) {
        return noise.getHash(ID, x, y) > threshold;
    }
}