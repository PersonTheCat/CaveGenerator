package personthecat.cavegenerator.presets.validator;

import personthecat.catlib.data.JsonPath;
import personthecat.catlib.data.Range;
import personthecat.cavegenerator.presets.data.WallDecoratorSettings;

import java.util.List;

import static personthecat.cavegenerator.presets.data.WallDecoratorSettings.Fields.directions;
import static personthecat.cavegenerator.presets.data.WallDecoratorSettings.Fields.integrity;
import static personthecat.cavegenerator.presets.data.WallDecoratorSettings.Fields.noise;
import static personthecat.cavegenerator.presets.data.WallDecoratorSettings.Fields.states;

public class WallDecoratorValidator {

    private WallDecoratorValidator() {}

    public static void apply(final ValidationContext ctx, final WallDecoratorSettings s, final JsonPath.Stub path) {
        if (s.integrity != null) {
            CommonValidators.integrity(ctx, s.integrity, path.key(integrity));
        }
        if (s.noise != null) {
            NoiseValidator.apply(ctx, s.noise, path.key(noise));
        }
        if (s.directions != null) {
            DirectionValidator.between(ctx, s.directions, path.key(directions));
        }
        if (s.states.isEmpty()) {
            ctx.err(path.key(states), "cg.errorText.noEntries");
        }
    }

    public static void between(final ValidationContext ctx, final List<WallDecoratorSettings> a, final JsonPath.Stub path) {
        int lastMinHeight = -1;
        int lastMaxHeight = -1;
        boolean foundGuaranteed = false;

        for (int i = 0; i < a.size(); i++) {
            final WallDecoratorSettings s = a.get(i);
            if (foundGuaranteed) {
                ctx.err(path.index(i), "cg.errorText.unreachableGenerator");
                foundGuaranteed = false;
            }
            final Range height = s.height != null ? s.height : Range.of(0, 50);
            final int minY = height.min;
            final int maxY = height.max;
            if (s.integrity != null && s.integrity >= 1.0 && lastMinHeight == minY && lastMaxHeight == maxY) {
                foundGuaranteed = true;
            }
            lastMinHeight = minY;
            lastMaxHeight = maxY;
        }
    }
}
