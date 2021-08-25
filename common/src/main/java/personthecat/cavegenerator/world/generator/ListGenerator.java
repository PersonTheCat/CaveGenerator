package personthecat.cavegenerator.world.generator;

import org.apache.commons.lang3.tuple.Pair;
import personthecat.cavegenerator.model.Conditions;
import personthecat.cavegenerator.presets.data.ConditionSettings;

import java.util.List;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static personthecat.catlib.util.Shorthand.map;

public abstract class ListGenerator<T> {
    protected final List<Pair<T, Conditions>> features;
    protected final long worldSeed;

    public ListGenerator(List<T> features, Function<T, ConditionSettings> getter, Random rand, long seed) {
        this.features = map(features, f -> Pair.of(f, Conditions.compile(getter.apply(f), rand, seed)));
        this.worldSeed = seed;
    }

    public void generate(final PrimerContext ctx) {
        // Stubbed for other global conditions.
        if (!features.isEmpty()) {
            this.generateChecked(ctx);
        }
    }

    protected abstract void generateChecked(final PrimerContext ctx);

    protected void forEachFeature(final BiConsumer<T, Conditions> fn) {
        for (final Pair<T, Conditions> feature : features) {
            fn.accept(feature.getLeft(), feature.getRight());
        }
    }
}
