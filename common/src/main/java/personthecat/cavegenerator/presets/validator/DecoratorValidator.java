package personthecat.cavegenerator.presets.validator;

import personthecat.catlib.data.JsonPath;
import personthecat.cavegenerator.presets.data.DecoratorSettings;

import static personthecat.cavegenerator.presets.data.DecoratorSettings.Fields.caveBlocks;
import static personthecat.cavegenerator.presets.data.DecoratorSettings.Fields.ponds;
import static personthecat.cavegenerator.presets.data.DecoratorSettings.Fields.shell;
import static personthecat.cavegenerator.presets.data.DecoratorSettings.Fields.wallDecorators;

import static personthecat.cavegenerator.presets.validator.ValidationFunction.validate;

public class DecoratorValidator {

    private DecoratorValidator() {}

    public static void apply(final ValidationContext ctx, final DecoratorSettings s, final JsonPath.Stub path) {
        if (s.caveBlocks != null) {
            final JsonPath.Stub cb = path.key(caveBlocks);
            validate(ctx, s.caveBlocks, cb, CaveBlockValidator::apply);
            CaveBlockValidator.between(ctx, s.caveBlocks, cb);
        }
        if (s.ponds != null) {
            validate(ctx, s.ponds, path.key(ponds), PondValidator::apply);
        }
        if (s.wallDecorators != null) {
            final JsonPath.Stub wd = path.key(wallDecorators);
            validate(ctx, s.wallDecorators, wd, WallDecoratorValidator::apply);
            WallDecoratorValidator.between(ctx, s.wallDecorators, wd);
        }
        if (s.shell != null) {
            ShellValidator.apply(ctx, s.shell, path.key(shell));
        }
    }
}
