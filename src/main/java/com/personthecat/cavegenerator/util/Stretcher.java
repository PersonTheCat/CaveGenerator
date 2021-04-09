package com.personthecat.cavegenerator.util;

/** Retains a list of previous values and adds them together, as needed. */
public class Stretcher {
    float value = 0.0F;

    private Stretcher() {}

    public static Stretcher withSize(int size) {
        if (size < 0 || size > 3) {
            throw new IllegalArgumentException(String.valueOf(size));
        }
        switch (size) {
            case 0: return new Stretcher();
            case 1: return new Stretcher1();
            case 2: return new Stretcher2();
            default: return new Stretcher3();
        }
    }

    public void set(float value) {
        this.value = value;
    }

    public void reset() {}

    public void shift() {}

    public float sum() {
        return value;
    }

    public static class Stretcher1 extends Stretcher {
        float valueM1 = 0.0F;

        private Stretcher1() {}

        @Override
        public void set(float value) {
            this.value = value;
        }

        @Override
        public void reset() {
            this.value = this.valueM1 = 0.0F;
        }

        @Override
        public void shift() {
            this.valueM1 = this.value;
        }

        public float sum() {
            return this.value - this.valueM1;
        }
    }

    public static class Stretcher2 extends Stretcher {
        float valueM1 = 0.0F;
        float valueM2 = 0.0F;

        private Stretcher2() {}

        @Override
        public void set(float value) {
            this.value = value;
        }

        @Override
        public void reset() {
            this.value = this.valueM1 = this.valueM2 = 0.0F;
        }

        @Override
        public void shift() {
            this.valueM2 = this.valueM1;
            this.valueM1 = this.value;
        }

        public float sum() {
            return this.value - this.valueM1 - this.valueM2;
        }
    }

    public static class Stretcher3 extends Stretcher {
        float valueM1 = 0.0F;
        float valueM2 = 0.0F;
        float valueM3 = 0.0F;

        private Stretcher3() {}

        @Override
        public void set(float value) {
            this.value = value;
        }

        @Override
        public void reset() {
            this.value = this.valueM1 = this.valueM2 = this.valueM3 = 0.0F;
        }

        @Override
        public void shift() {
            this.valueM3 = this.valueM2;
            this.valueM2 = this.valueM1;
            this.valueM1 = this.value;
        }

        public float sum() {
            return this.value - this.valueM1 - this.valueM2 - this.valueM3;
        }
    }
}
