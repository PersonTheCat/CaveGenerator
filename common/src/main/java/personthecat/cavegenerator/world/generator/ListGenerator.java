package personthecat.cavegenerator.world.generator;

import org.apache.commons.lang3.tuple.Pair;
import personthecat.cavegenerator.world.config.ConditionConfig;

import java.util.List;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static personthecat.catlib.util.Shorthand.map;

public abstract class ListGenerator<T> {
    protected final List<Pair<T, ConditionConfig>> features;
    protected final long worldSeed;

    public ListGenerator(List<T> features, Function<T, ConditionConfig> getter, Random rand, long seed) {
        this.features = map(features, f -> Pair.of(f, getter.apply(f)));
        this.worldSeed = seed;
    }

    public void generate(final PrimerContext ctx) {
        // Stubbed for other global conditions.
        if (!features.isEmpty()) {
            this.generateChecked(ctx);
        }
    }

    protected abstract void generateChecked(final PrimerContext ctx);

    protected void forEachFeature(final BiConsumer<T, ConditionConfig> fn) {
        for (final Pair<T, ConditionConfig> feature : features) {
            fn.accept(feature.getLeft(), feature.getRight());
        }
    }
}
