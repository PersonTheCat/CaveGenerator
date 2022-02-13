package personthecat.cavegenerator.presets.validator;

import personthecat.catlib.data.JsonPath;
import personthecat.cavegenerator.model.Direction;

import java.util.List;

public class DirectionValidator {

    public static void between(final ValidationContext ctx, final List<Direction> a, final JsonPath.Stub path) {
        if (a.contains(Direction.ALL) && a.size() > 1) {
            ctx.warn(path, "cg.errorText.directionsCovered", Direction.ALL);
        } else if (a.contains(Direction.SIDE) && containsSide(a)) {
            ctx.warn(path, "cg.errorText.directionsCovered", Direction.SIDE);
        }
    }

    private static boolean containsSide(final List<Direction> a) {
        for (final Direction d : a) {
            if (d == Direction.NORTH || d == Direction.SOUTH || d == Direction.EAST || d == Direction.WEST) {
                return true;
            }
        }
        return false;
    }
}
