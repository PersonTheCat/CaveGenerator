package personthecat.cavegenerator.presets.validator;


import personthecat.catlib.data.JsonPath;
import personthecat.cavegenerator.presets.data.ShellSettings;

import static personthecat.cavegenerator.presets.data.ShellSettings.Decorator.Fields.integrity;
import static personthecat.cavegenerator.presets.data.ShellSettings.Decorator.Fields.matchers;
import static personthecat.cavegenerator.presets.data.ShellSettings.Decorator.Fields.noise;
import static personthecat.cavegenerator.presets.data.ShellSettings.Decorator.Fields.states;
import static personthecat.cavegenerator.presets.data.ShellSettings.Fields.decorators;
import static personthecat.cavegenerator.presets.data.ShellSettings.Fields.sphereResolution;

public class ShellValidator {

    private ShellValidator() {}

    public static void apply(final ValidationContext ctx, final ShellSettings s, final JsonPath.Stub path) {
        if (s.sphereResolution != null && s.sphereResolution < 0) {
            ctx.err(path.key(sphereResolution), "cg.errorText.cantBeNegative");
        }
        if (s.decorators != null) {
            final JsonPath.Stub d = path.key(decorators);
            for (int i = 0; i < s.decorators.size(); i++) {
                decorator(ctx, s.decorators.get(i), d.index(i));
            }
        }
    }

    private static void decorator(final ValidationContext ctx, final ShellSettings.Decorator s, JsonPath.Stub path) {
        if (s.integrity != null) {
            CommonValidators.integrity(ctx, s.integrity, path.key(integrity));
        }
        if (s.matchers != null) {
            CommonValidators.matchers(ctx, s.matchers, path.key(matchers));
        }
        if (s.noise != null) {
            NoiseValidator.apply(ctx, s.noise, path.key(noise));
        }
        if (s.states.isEmpty()) {
            ctx.err(path.key(states), "cg.errorText.noEntries");
        }
    }
}
