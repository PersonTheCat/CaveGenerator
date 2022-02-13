package personthecat.cavegenerator.presets.validator;

import personthecat.catlib.data.JsonPath;
import personthecat.cavegenerator.presets.data.TunnelSettings;

import static personthecat.cavegenerator.presets.data.TunnelSettings.Fields.branches;
import static personthecat.cavegenerator.presets.data.TunnelSettings.Fields.count;
import static personthecat.cavegenerator.presets.data.TunnelSettings.Fields.distance;
import static personthecat.cavegenerator.presets.data.TunnelSettings.Fields.pitch;
import static personthecat.cavegenerator.presets.data.TunnelSettings.Fields.resolution;
import static personthecat.cavegenerator.presets.data.TunnelSettings.Fields.scale;
import static personthecat.cavegenerator.presets.data.TunnelSettings.Fields.systemDensity;
import static personthecat.cavegenerator.presets.data.TunnelSettings.Fields.yaw;

public class TunnelValidator {

    private TunnelValidator() {}

    public static void apply(final ValidationContext ctx, final TunnelSettings s, final JsonPath.Stub path) {
        ConditionValidator.apply(ctx, s.conditions, path);
        DecoratorValidator.apply(ctx, s.decorators, path);

        if (s.branches != null) {
            apply(ctx, s.branches, path.key(branches));
        }
        if (s.count != null && s.count < 0) {
            ctx.err(path.key(count), "cg.errorText.cantBeNegative");
        }
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
        if (s.systemDensity != null && s.systemDensity < 0) {
            ctx.err(path.key(systemDensity), "cg.errorText.cantBeNegative");
        }
        if (s.yaw != null) {
            ScalableFloatValidator.angle(ctx, s.yaw, path.key(yaw));
        }
    }
}
