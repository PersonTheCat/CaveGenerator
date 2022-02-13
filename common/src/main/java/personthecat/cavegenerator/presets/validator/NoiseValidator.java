package personthecat.cavegenerator.presets.validator;

import personthecat.catlib.data.FloatRange;
import personthecat.catlib.data.JsonPath;
import personthecat.catlib.data.Range;
import personthecat.cavegenerator.presets.data.NoiseSettings;

import static personthecat.cavegenerator.presets.data.NoiseSettings.Fields.octaves;
import static personthecat.cavegenerator.presets.data.NoiseSettings.Fields.threshold;

public class NoiseValidator {

    private static final FloatRange THRESHOLD_BOUNDS = Range.of(-1.0F, 1.0F);

    private NoiseValidator() {}

    public static void apply(final ValidationContext ctx, final NoiseSettings s, final JsonPath.Stub path) {
        if (s.octaves != null) {
            octaves(ctx, s.octaves, path.key(octaves));
        }
        if (s.threshold != null) {
            threshold(ctx, s.threshold, path.key(threshold));
        }
    }

    private static void octaves(final ValidationContext ctx, final int octaves, final JsonPath.Stub path) {
        if (octaves < 0) {
            ctx.warn(path, "cg.errorText.negativeOctaves", octaves);
        } else if (octaves <= 1) {
            ctx.warn(path, "cg.errorText.oneOctave", octaves);
        } else if (octaves > 5) {
            ctx.warn(path, "cg.errorText.tooManyOctaves", octaves);
        }
    }

    private static void threshold(final ValidationContext ctx, final FloatRange s, final JsonPath.Stub path) {
        if (!(isInBounds(s.min) && isInBounds(s.max))) {
            ctx.warn(path, "cg.errorText.outOfBounds", s, THRESHOLD_BOUNDS);
        }
    }

    private static boolean isInBounds(final float s) {
        return s >= THRESHOLD_BOUNDS.min && s <= THRESHOLD_BOUNDS.max;
    }
}
