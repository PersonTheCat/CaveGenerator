package personthecat.cavegenerator.world.config;

import lombok.AllArgsConstructor;
import personthecat.catlib.data.BiomePredicate;
import personthecat.catlib.data.DimensionPredicate;
import personthecat.catlib.data.Range;
import personthecat.cavegenerator.world.generator.PrimerContext;
import personthecat.fastnoise.FastNoise;

@AllArgsConstructor
public class ConditionConfig {
    public final BiomePredicate biomes;
    public final DimensionPredicate dimensions;
    public final Range height;
    public final FastNoise floor;
    public final FastNoise ceiling;
    public final FastNoise region;
    public final FastNoise noise;
    public final boolean hasBiomes;
    public final boolean hasRegion;

    public Range getColumn(final int x, final int z) {
        final int min = this.height.min + (int) this.floor.getNoiseScaled((float) x, (float) z);
        final int max = this.height.max + (int) this.ceiling.getNoiseScaled((float) x, (float) z);
        return Range.checkedOrEmpty(min, max);
    }

    public Range getColumn(final PrimerContext ctx, final int x, final int z) {
        final int surface = ctx.getHeight(x, z);
        final int min = this.height.min + (int) this.floor.getNoiseScaled((float) x, (float) z);
        final int max = Math.min(this.height.max, surface) + (int) this.ceiling.getNoiseScaled((float) x, (float) z);
        return Range.checkedOrEmpty(min, max);
    }
}
