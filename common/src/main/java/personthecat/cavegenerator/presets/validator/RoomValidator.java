package personthecat.cavegenerator.presets.validator;

import personthecat.catlib.data.JsonPath;
import personthecat.cavegenerator.presets.data.RoomSettings;

import static personthecat.cavegenerator.presets.data.RoomSettings.Fields.scale;

public class RoomValidator {

    private static final float WARN_SCALE = 10.0F;

    private RoomValidator() {}

    public static void apply(final ValidationContext ctx, final RoomSettings s, final JsonPath.Stub path) {
        DecoratorValidator.apply(ctx, s.decorators, path);

        if (s.scale != null && s.scale > WARN_SCALE) {
            ctx.warn(path.key(scale), "cg.errorText.largeValue", s.scale);
        }
    }
}
