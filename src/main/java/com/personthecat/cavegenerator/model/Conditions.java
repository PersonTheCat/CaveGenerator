package com.personthecat.cavegenerator.model;

import com.personthecat.cavegenerator.data.ConditionSettings;
import com.personthecat.cavegenerator.util.DummyGenerator;
import fastnoise.FastNoise;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.experimental.FieldDefaults;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

import java.util.List;
import java.util.function.Predicate;

@Builder(toBuilder = true)
@FieldDefaults(level = AccessLevel.PUBLIC, makeFinal = true)
public class Conditions {

    /** Any conditions for spawning this feature according to the current biome. */
    @Default Predicate<Biome> biomes = b -> true;

    /** Any conditions for spawning this feature according to the current dimension. */
    @Default Predicate<Integer> dimensions = d -> true;

    /** Height restrictions for the current feature. */
    @Default Range height = Range.of(0, 255);

    /** The value produced by this generator will augment the maximum height level. */
    @Default FastNoise floor = new DummyGenerator(0F);

    /** The value produced by this generator will augment the minimum height level. */
    @Default FastNoise ceiling = new DummyGenerator(0F);

    /** 2-dimensional noise constraints for this feature. */
    @Default FastNoise region = new DummyGenerator(0F);

    /** 3-dimensional noise constraints for this feature. */
    @Default FastNoise noise = new DummyGenerator(0F);

    public static Conditions compile(ConditionSettings settings, World world) {
        final ConditionsBuilder builder = builder()
            .biomes(compileBiomes(settings))
            .dimensions(compileDimensions(settings))
            .height(settings.height);

        settings.floor.ifPresent(c -> builder.floor(c.getGenerator(world)));
        settings.ceiling.ifPresent(c -> builder.ceiling(c.getGenerator(world)));
        settings.region.ifPresent(c -> builder.region(c.getGenerator(world)));
        settings.noise.ifPresent(c -> builder.noise(c.getGenerator(world)));
        return builder.build();
    }

    private static Predicate<Biome> compileBiomes(ConditionSettings settings) {
        final List<Biome> list = settings.biomes;
        if (list.isEmpty()) {
            return b -> true;
        } else if (list.size() == 1) {
            final Biome listed = list.get(0);
            return settings.blacklistBiomes ? b -> !listed.equals(b) : listed::equals;
        }
        return settings.blacklistBiomes ? b -> !settings.biomes.contains(b) : settings.biomes::contains;
    }

    private static Predicate<Integer> compileDimensions(ConditionSettings settings) {
        final List<Integer> list = settings.dimensions;
        if (list.isEmpty()) {
            return d -> true;
        } else if (list.size() == 1) {
            final int listed = list.get(0);
            return settings.blacklistDimensions ? d -> listed != d : d -> listed == d;
        }
        return settings.blacklistDimensions ? d -> !list.contains(d) : list::contains;
    }

    /** Get the current height range when given two absolute coordinates. */
    public Range getColumn(int x, int z) {
        if (region.GetBoolean(x, z)) {
            final int min = height.min + (int) floor.GetAdjustedNoise((float) x, (float) z);
            final int max = height.max + (int) ceiling.GetAdjustedNoise((float) x, (float) z);
            return Range.of(min, max);
        }
        return Range.of(0);
    }

    /** Intended for any generator that does relatively few checks. Will verify everything but the dimension. */
    public boolean checkSingle(Biome b, int x, int y, int z) {
        return biomes.test(b) && getColumn(x, z).contains(y) && noise.GetBoolean(x, y, z);
    }
}
