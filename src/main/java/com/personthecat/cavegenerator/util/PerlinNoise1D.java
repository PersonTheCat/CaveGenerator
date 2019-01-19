package com.personthecat.cavegenerator.util;

public class PerlinNoise1D extends SimplexNoise3D {
    public PerlinNoise1D(long seed) {
        super(seed);
    }

    private double fade(double t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    private double lerp(double t, double a, double b) {
        return a + t * (b - a);
    }

    private double grad(int seed, double x) {
        return (seed & 1) == 0 ? x : -x;
    }

    public double getNoise(double x) {
        int X = floor(x) & 255;
        x -= floor(x);
        double u = fade(x);
        double before = grad(permutations[X], x);
        double after = grad(permutations[X + 1], x - 1);
        return lerp(u, before, after);
    }

    public double getFractalNoise(double x, int octaves, double frq, double amp) {
        double gain = 1.0, sum = 0.0f;

        for (int i = 0; i < octaves; i++) {
            sum += getNoise(x * gain / frq) * amp / gain;
            gain *= 2.0;
        }
        return sum;
    }
}