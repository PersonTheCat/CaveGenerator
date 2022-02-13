package personthecat.cavegenerator.presets.validator;

import net.minecraft.world.level.block.state.BlockState;
import personthecat.catlib.data.FloatRange;
import personthecat.catlib.data.JsonPath;
import personthecat.catlib.data.Range;

import java.util.Set;

public class CommonValidators {

    private static final FloatRange INTEGRITY_BOUNDS = Range.of(-1.0F, 1.0F);
    private static final int WARN_MATCHERS = 20;

    public static void chance(final ValidationContext ctx, final double chance, final JsonPath.Stub path) {
        integrity(ctx, chance, path);
    }

    public static void integrity(final ValidationContext ctx, final double integrity, final JsonPath.Stub path) {
        if (integrity > INTEGRITY_BOUNDS.max || integrity < INTEGRITY_BOUNDS.min) {
            ctx.warn(path, "cg.errorText.outOfBounds", integrity, INTEGRITY_BOUNDS);
        }
    }

    public static void matchers(final ValidationContext ctx, final Set<BlockState> matchers, final JsonPath.Stub path) {
        if (matchers.isEmpty()) {
            ctx.err(path, "cg.errorText.noEntries");
        } else if (matchers.size() > WARN_MATCHERS) {
            ctx.warn(path, "cg.errorText.tooManyEntries");
        }
    }
}
