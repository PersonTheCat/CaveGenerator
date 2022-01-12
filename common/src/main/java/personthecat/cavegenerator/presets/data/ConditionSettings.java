package personthecat.cavegenerator.presets.data;

import com.mojang.serialization.Codec;
import lombok.Builder;
import lombok.experimental.FieldNameConstants;
import personthecat.catlib.data.BiomePredicate;
import personthecat.catlib.data.DimensionPredicate;
import personthecat.catlib.data.Range;
import personthecat.cavegenerator.world.config.ConditionConfig;
import personthecat.fastnoise.FastNoise;

import javax.annotation.Nullable;

import java.util.Random;

import static personthecat.catlib.serialization.CodecUtils.codecOf;
import static personthecat.catlib.serialization.FieldDescriptor.nullable;

@Builder(toBuilder = true)
@FieldNameConstants
public class ConditionSettings implements ConfigProvider<ConditionSettings, ConditionConfig> {
    @Nullable public final BiomePredicate biomes;
    @Nullable public final DimensionPredicate dimensions;
    @Nullable public final Range height;
    @Nullable public final NoiseSettings floor;
    @Nullable public final NoiseSettings ceiling;
    @Nullable public final NoiseSettings region;
    @Nullable public final NoiseSettings noise;

    public static final ConditionSettings EMPTY = new ConditionSettings(null, null, null, null, null, null, null);

    public static final Codec<ConditionSettings> CODEC = codecOf(
        nullable(BiomePredicate.CODEC, Fields.biomes, c -> c.biomes),
        nullable(DimensionPredicate.CODEC, Fields.dimensions, c -> c.dimensions),
        nullable(Range.CODEC, Fields.height, c -> c.height),
        nullable(NoiseSettings.MAP, Fields.floor, c -> c.floor),
        nullable(NoiseSettings.MAP, Fields.ceiling, c -> c.ceiling),
        nullable(NoiseSettings.REGION, Fields.region, c -> c.region),
        nullable(NoiseSettings.NOISE, Fields.noise, c -> c.noise),
        ConditionSettings::new
    );

    @Override
    public Codec<ConditionSettings> codec() {
        return CODEC;
    }

    @Override
    public ConditionSettings withOverrides(final OverrideSettings o) {
        return this.toBuilder()
            .biomes(this.biomes != null ? this.biomes : o.biomes)
            .dimensions(this.dimensions != null ? this.dimensions : o.dimensions)
            .height(this.height != null ? this.height : o.height)
            .floor(this.floor != null ? this.floor : o.floor)
            .ceiling(this.ceiling != null ? this.ceiling : o.ceiling)
            .region(this.region != null ? this.region : o.region)
            .noise(this.noise != null ? this.noise : o.noise)
            .build();
    }

    @Override
    public ConditionSettings withDefaults(final ConditionSettings defaults) {
        return this.toBuilder()
            .biomes(this.biomes != null ? this.biomes : defaults.biomes)
            .dimensions(this.dimensions != null ? this.dimensions : defaults.dimensions)
            .height(this.height != null ? this.height : defaults.height)
            .floor(defaults.floor != null ? NoiseSettings.withDefaults(this.floor, defaults.floor) : this.floor)
            .ceiling(defaults.ceiling != null ? NoiseSettings.withDefaults(this.ceiling, defaults.ceiling) : this.ceiling)
            .region(defaults.region != null ? NoiseSettings.withDefaults(this.region, defaults.region) : this.region)
            .noise(defaults.noise != null ? NoiseSettings.withDefaults(this.noise, defaults.noise) : this.noise)
            .build();
    }

    public ConditionSettings withDefaultFloor(final NoiseSettings floor) {
        if (this.floor == null) return this;
        return this.toBuilder().floor(this.floor.withDefaults(floor)).build();
    }

    public ConditionSettings withDefaultCeiling(final NoiseSettings ceiling) {
        if (this.ceiling == null) return this;
        return this.toBuilder().ceiling(this.ceiling.withDefaults(ceiling)).build();
    }

    public ConditionSettings withDefaultRegion(final NoiseSettings region) {
        if (this.region == null) return this;
        return this.toBuilder().region(this.region.withDefaults(region)).build();
    }

    public ConditionSettings withDefaultNoise(final NoiseSettings noise) {
        if (this.noise == null) return this;
        return this.toBuilder().noise(this.noise.withDefaults(noise)).build();
    }

    @Override
    public ConditionConfig compile(final Random rand, final long seed) {
        final BiomePredicate biomes = this.biomes != null ? this.biomes : BiomePredicate.ALL_BIOMES;
        final DimensionPredicate dimensions = this.dimensions != null ? this.dimensions : DimensionPredicate.ALL_DIMENSIONS;
        final Range height = this.height != null ? this.height : Range.of(0, 255);
        final FastNoise floor = NoiseSettings.compile(this.floor, rand, seed);
        final FastNoise ceiling = NoiseSettings.compile(this.ceiling, rand, seed);
        final FastNoise region = NoiseSettings.compile(this.region, rand, seed);
        final FastNoise noise = NoiseSettings.compile(this.noise, rand, seed);
        final boolean hasBiomes = this.biomes != null && this.biomes != BiomePredicate.ALL_BIOMES;
        final boolean hasRegion = this.region != null;

        return new ConditionConfig(biomes, dimensions, height, floor, ceiling, region, noise, hasBiomes, hasRegion);
    }
}
