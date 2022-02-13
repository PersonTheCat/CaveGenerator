package personthecat.cavegenerator.presets.validator;

import personthecat.catlib.data.JsonPath;
import personthecat.cavegenerator.presets.data.StalactiteSettings;

import static personthecat.cavegenerator.presets.data.StalactiteSettings.Fields.chance;
import static personthecat.cavegenerator.presets.data.StalactiteSettings.Fields.matchers;

public class StalactiteValidator {

    private StalactiteValidator() {}

    public static void apply(final ValidationContext ctx, final StalactiteSettings s, final JsonPath.Stub path) {
        ConditionValidator.apply(ctx, s.conditions, path);

        if (s.chance != null) {
            CommonValidators.chance(ctx, s.chance, path.key(chance));
        }
        if (s.matchers != null) {
            CommonValidators.matchers(ctx, s.matchers, path.key(matchers));
        }
    }
}
