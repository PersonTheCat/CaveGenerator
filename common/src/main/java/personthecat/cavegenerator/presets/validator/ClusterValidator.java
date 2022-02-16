package personthecat.cavegenerator.presets.validator;

import personthecat.catlib.data.JsonPath;
import personthecat.cavegenerator.presets.data.ClusterSettings;

import static personthecat.cavegenerator.presets.data.ClusterSettings.Fields.chance;
import static personthecat.cavegenerator.presets.data.ClusterSettings.Fields.integrity;
import static personthecat.cavegenerator.presets.data.ClusterSettings.Fields.matchers;
import static personthecat.cavegenerator.presets.data.ClusterSettings.Fields.states;

public class ClusterValidator {

    private ClusterValidator() {}

    public static void apply(final ValidationContext ctx, final ClusterSettings s, final JsonPath.Stub path) {
        ConditionValidator.apply(ctx, s.conditions, path);

        if (s.chance != null) {
            CommonValidators.chance(ctx, s.chance, path.key(chance));
        }
        if (s.integrity != null) {
            CommonValidators.integrity(ctx, s.integrity, path.key(integrity));
        }
        if (s.matchers != null) {
            CommonValidators.matchers(ctx, s.matchers, path.key(matchers));
        }
        if (s.states.isEmpty()) {
            ctx.err(path.key(states), "cg.errorText.noEntries");
        }
    }
}
