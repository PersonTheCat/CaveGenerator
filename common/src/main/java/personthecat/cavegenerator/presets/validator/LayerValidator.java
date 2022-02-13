package personthecat.cavegenerator.presets.validator;

import personthecat.catlib.data.JsonPath;
import personthecat.cavegenerator.presets.data.LayerSettings;

import static personthecat.cavegenerator.presets.data.LayerSettings.Fields.matchers;

public class LayerValidator {

    private LayerValidator() {}

    public static void apply(final ValidationContext ctx, final LayerSettings s, final JsonPath.Stub path) {
        ConditionValidator.apply(ctx, s.conditions, path);

        if (s.matchers != null) {
            CommonValidators.matchers(ctx, s.matchers, path.key(matchers));
        }
    }
}
