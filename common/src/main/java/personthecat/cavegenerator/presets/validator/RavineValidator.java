package personthecat.cavegenerator.presets.validator;

import personthecat.catlib.data.JsonPath;
import personthecat.cavegenerator.presets.data.RavineSettings;

import static personthecat.cavegenerator.presets.data.RavineSettings.Fields.distance;
import static personthecat.cavegenerator.presets.data.RavineSettings.Fields.pitch;
import static personthecat.cavegenerator.presets.data.RavineSettings.Fields.resolution;
import static personthecat.cavegenerator.presets.data.RavineSettings.Fields.scale;
import static personthecat.cavegenerator.presets.data.RavineSettings.Fields.walls;
import static personthecat.cavegenerator.presets.data.RavineSettings.Fields.yaw;

public class RavineValidator {

    private RavineValidator() {}

    public static void apply(final ValidationContext ctx, final RavineSettings s, final JsonPath.Stub path) {
        ConditionValidator.apply(ctx, s.conditions, path);
        DecoratorValidator.apply(ctx, s.decorators, path);

        if (s.distance != null && s.distance < 0) {
            ctx.err(path.key(distance), "cg.errorText.cantBeNegative");
        }
        if (s.pitch != null) {
            ScalableFloatValidator.angle(ctx, s.pitch, path.key(pitch));
        }
        if (s.resolution != null && s.resolution < 0) {
            ctx.err(path.key(resolution), "cg.errorText.cantBeNegative");
        }
        if (s.scale != null) {
            ScalableFloatValidator.scale(ctx, s.scale, path.key(scale));
        }
        if (s.walls != null) {
            NoiseValidator.apply(ctx, s.walls, path.key(walls));
        }
        if (s.yaw != null) {
            ScalableFloatValidator.angle(ctx, s.yaw, path.key(yaw));
        }
    }
}
