package personthecat.cavegenerator.presets.validator;

import personthecat.catlib.data.JsonPath;
import personthecat.cavegenerator.presets.data.PondSettings;

import static personthecat.cavegenerator.presets.data.PondSettings.Fields.depth;
import static personthecat.cavegenerator.presets.data.PondSettings.Fields.integrity;
import static personthecat.cavegenerator.presets.data.PondSettings.Fields.matchers;
import static personthecat.cavegenerator.presets.data.PondSettings.Fields.noise;

public class PondValidator {

    private PondValidator() {}

    public static void apply(final ValidationContext ctx, final PondSettings s, final JsonPath.Stub path) {
        if (s.depth != null && s.depth < 1) {
            ctx.err(path.key(depth), "cg.errorText.cantBeNegative");
        }
        if (s.integrity != null) {
            CommonValidators.integrity(ctx, s.integrity, path.key(integrity));
        }
        if (s.matchers != null) {
            CommonValidators.matchers(ctx, s.matchers, path.key(matchers));
        }
        if (s.noise != null) {
            NoiseValidator.apply(ctx, s.noise, path.key(noise));
        }
    }
}
