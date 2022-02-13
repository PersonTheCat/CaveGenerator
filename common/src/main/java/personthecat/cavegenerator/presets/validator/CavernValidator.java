package personthecat.cavegenerator.presets.validator;

import personthecat.catlib.data.JsonPath;
import personthecat.cavegenerator.presets.data.CavernSettings;

import static personthecat.cavegenerator.presets.data.CavernSettings.Fields.branches;
import static personthecat.cavegenerator.presets.data.CavernSettings.Fields.generators;
import static personthecat.cavegenerator.presets.data.CavernSettings.Fields.offset;
import static personthecat.cavegenerator.presets.data.CavernSettings.Fields.wallOffset;
import static personthecat.cavegenerator.presets.data.CavernSettings.Fields.walls;

import static personthecat.cavegenerator.presets.validator.ValidationFunction.validate;

public class CavernValidator {

    private CavernValidator() {}

    public static void apply(final ValidationContext ctx, final CavernSettings s, final JsonPath.Stub path) {
        ConditionValidator.apply(ctx, s.conditions, path);
        DecoratorValidator.apply(ctx, s.decorators, path);

        if (s.branches != null) {
            TunnelValidator.apply(ctx, s.branches, path.key(branches));
        }
        if (s.generators != null) {
            validate(ctx, s.generators, path.key(generators), NoiseValidator::apply);
        }
        if (s.offset != null) {
            NoiseValidator.apply(ctx, s.offset, path.key(offset));
        }
        if (s.wallOffset != null) {
            NoiseValidator.apply(ctx, s.wallOffset, path.key(wallOffset));
        }
        if (s.walls != null) {
            NoiseValidator.apply(ctx, s.walls, path.key(walls));
        }
    }
}
