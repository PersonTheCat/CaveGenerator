package com.personthecat.cavegenerator.world.generator;

import com.personthecat.cavegenerator.data.ConditionSettings;
import com.personthecat.cavegenerator.model.Conditions;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkPrimer;
import org.apache.commons.lang3.tuple.Pair;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class ListGenerator<T> {

    protected final WeakReference<World> world;
    protected final List<Pair<T, Conditions>> features;

    public ListGenerator(List<T> features, Function<T, ConditionSettings> getter, World world) {
        Objects.requireNonNull(world, "Nullable world types are not yet supported.");
        this.world = new WeakReference<>(world);
        this.features = features.stream()
            .map(t -> Pair.of(t, Conditions.compile(getter.apply(t), world)))
            .collect(Collectors.toList());
    }

    protected final World getWorld() {
        return Objects.requireNonNull(world.get(), "World reference has been culled.");
    }

    public void generate(World world, Random rand, int destChunkX, int destChunkZ, int chunkX, int chunkZ, ChunkPrimer primer) {
        // Stubbed for other global conditions.
        generateChecked(world, rand, destChunkX, destChunkZ, chunkX, chunkZ, primer);
    }

    protected abstract void generateChecked(World world, Random rand, int destChunkX, int destChunkZ, int chunkX, int chunkZ, ChunkPrimer primer);

    protected void forEachFeature(BiConsumer<T, Conditions> fn) {
        for (Pair<T, Conditions> feature : features) {
            fn.accept(feature.getLeft(), feature.getRight());
        }
    }
}
