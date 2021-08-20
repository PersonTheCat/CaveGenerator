package personthecat.cavegenerator.noise;

import personthecat.fastnoise.FastNoise;

public class DummyGenerator extends FastNoise {

    private final float output;

    public DummyGenerator(final float output) {
        super(0);
        this.output = output;
    }

    @Override
    public float getNoiseScaled(float x, float y) {
        return output;
    }

    @Override
    public float getNoiseScaled(float x, float y, float z) {
        return output;
    }

    @Override
    public boolean getBoolean(float x, float y) {
        return true;
    }

    @Override
    public boolean getBoolean(float x, float y, float z) {
        return true;
    }

    @Override
    public float getSingle(int i, float v) {
        return output;
    }

    @Override
    public float getSingle(int i, float v, float v1) {
        return output;
    }

    @Override
    public float getSingle(int i, float v, float v1, float v2) {
        return output;
    }

    @Override
    public float getNoise(float x, float y) {
        return 0F;
    }

    @Override
    public float getNoise(float x, float y, float z) {
        return 0F;
    }
}
