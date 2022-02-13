package personthecat.cavegenerator.presets.validator;

import personthecat.catlib.data.FloatRange;
import personthecat.catlib.data.JsonPath;
import personthecat.catlib.data.Range;
import personthecat.cavegenerator.model.ScalableFloat;

public class ScalableFloatValidator {

    private static final FloatRange FACTOR_BOUNDS = Range.of(-2.0F, 2.0F);
    private static final FloatRange ANGLE_BOUNDS = Range.of(0.0F, 6.0F);

    public static void scale(final ValidationContext ctx, final ScalableFloat s, final JsonPath.Stub path) {
        if (s.factor > FACTOR_BOUNDS.max || s.factor < FACTOR_BOUNDS.min) {
            ctx.warn(path, "cg.errorText.largeValue", s.factor);
        }
        if (s.exponent > 1.5) {
            ctx.warn(path, "cg.errorText.largeValue", s.exponent);
        }
    }

    public static void angle(final ValidationContext ctx, final ScalableFloat s, final JsonPath.Stub path) {
        if (s.startVal > ANGLE_BOUNDS.max || s.startVal < ANGLE_BOUNDS.min) {
            ctx.warn(path, "cg.errorText.outOfBounds", s.startVal, ANGLE_BOUNDS);
        }
    }
}
