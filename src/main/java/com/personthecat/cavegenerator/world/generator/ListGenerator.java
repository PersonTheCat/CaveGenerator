package com.personthecat.cavegenerator.world.generator;

import com.personthecat.cavegenerator.data.ConditionSettings;
import com.personthecat.cavegenerator.model.Conditions;
import net.minecraft.world.World;
import org.apache.commons.lang3.tuple.Pair;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static com.personthecat.cavegenerator.util.CommonMethods.map;

public abstract class ListGenerator<T> {

    protected final WeakReference<World> world;
    protected final List<Pair<T, Conditions>> features;

    public ListGenerator(List<T> features, Function<T, ConditionSettings> getter, World world) {
        Objects.requireNonNull(world, "Nullable world types are not yet supported.");
        this.world = new WeakReference<>(world);
        this.features = map(features, f -> Pair.of(f, Conditions.compile(getter.apply(f), world)));
    }

    protected final World getWorld() {
        return Objects.requireNonNull(world.get(), "World reference has been culled.");
    }

    public void generate(PrimerContext ctx) {
        // Stubbed for other global conditions.
        this.generateChecked(ctx);
    }

    protected abstract void generateChecked(PrimerContext ctx);

    protected void forEachFeature(BiConsumer<T, Conditions> fn) {
        for (Pair<T, Conditions> feature : features) {
            fn.accept(feature.getLeft(), feature.getRight());
        }
    }
}
