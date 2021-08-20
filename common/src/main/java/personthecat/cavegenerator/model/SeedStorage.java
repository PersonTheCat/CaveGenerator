package personthecat.cavegenerator.model;

import lombok.AllArgsConstructor;

import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

public class SeedStorage {

    private final AtomicReference<Info> data =
        new AtomicReference<>(new Info(new Random(0L), 0L));

    public synchronized Info get() {
        return this.data.get();
    }

    public synchronized void set(final Random rand, final long seed) {
        this.data.set(new Info(rand, seed));
    }

    @AllArgsConstructor
    public static class Info {
        public final Random rand;
        public final long seed;
    }
}
