package personthecat.cavegenerator.presets.validator;

import personthecat.catlib.data.JsonPath;
import personthecat.cavegenerator.presets.data.PillarSettings;

import static personthecat.cavegenerator.presets.data.PillarSettings.Fields.count;

public class PillarValidator {

    private PillarValidator() {}

    public static void apply(final ValidationContext ctx, final PillarSettings s, final JsonPath.Stub path) {
        ConditionValidator.apply(ctx, s.conditions, path);

        if (s.count != null && s.count < 0) {
            ctx.err(path.key(count), "cg.errorText.cantBeNegative");
        }
    }
}
