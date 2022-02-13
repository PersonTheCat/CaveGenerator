package personthecat.cavegenerator.presets.validator;

import personthecat.catlib.data.JsonPath;
import personthecat.cavegenerator.presets.data.BurrowSettings;

import static personthecat.cavegenerator.presets.data.BurrowSettings.Fields.branches;
import static personthecat.cavegenerator.presets.data.BurrowSettings.Fields.map;
import static personthecat.cavegenerator.presets.data.BurrowSettings.Fields.offset;

public class BurrowValidator {

    private BurrowValidator() {}

    public static void apply(final ValidationContext ctx, final BurrowSettings s, final JsonPath.Stub path) {
        ConditionValidator.apply(ctx, s.conditions, path);
        DecoratorValidator.apply(ctx, s.decorators, path);

        if (s.branches != null) {
            TunnelValidator.apply(ctx, s.branches, path.key(branches));
        }
        if (s.map != null) {
            NoiseValidator.apply(ctx, s.map, path.key(map));
        }
        if (s.offset != null) {
            NoiseValidator.apply(ctx, s.offset, path.key(offset));
        }
    }
}
