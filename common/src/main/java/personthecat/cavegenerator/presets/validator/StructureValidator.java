package personthecat.cavegenerator.presets.validator;

import net.minecraft.world.level.levelgen.structure.templatesystem.BlockRotProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import personthecat.catlib.data.JsonPath;
import personthecat.cavegenerator.mixin.BlockRotProcessorAccessor;
import personthecat.cavegenerator.presets.data.StructureSettings;

import java.util.List;

import static personthecat.cavegenerator.presets.data.StructureSettings.Fields.chance;
import static personthecat.cavegenerator.presets.data.StructureSettings.Fields.count;
import static personthecat.cavegenerator.presets.data.StructureSettings.Fields.directions;
import static personthecat.cavegenerator.presets.data.StructureSettings.Fields.matchers;

public class StructureValidator {

    private static final int WARN_CHECKS = 25;

    private StructureValidator() {}

    public static void apply(final ValidationContext ctx, final StructureSettings s, final JsonPath.Stub path) {
        ConditionValidator.apply(ctx, s.conditions, path);

        for (final StructureProcessor p : s.placement.getProcessors()) {
            if (p instanceof BlockRotProcessor) {
                final double integrity = ((BlockRotProcessorAccessor) p).getIntegrity();
                CommonValidators.integrity(ctx, integrity, path.key("integrity"));
                break;
            }
        }
        if (WARN_CHECKS < count(s.airChecks, s.blockChecks, s.solidChecks, s.nonSolidChecks)) {
            ctx.warn(path, "cg.errorText.tooManyChecks");
        }
        if (s.chance != null) {
            CommonValidators.chance(ctx, s.chance, path.key(chance));
        }
        if (s.count != null && s.count < 0) {
            ctx.err(path.key(count), "cg.errorText.cantBeNegative");
        }
        if (s.directions != null) {
            DirectionValidator.between(ctx, s.directions, path.key(directions));
        }
        if (s.matchers != null) {
            CommonValidators.matchers(ctx, s.matchers, path.key(matchers));
        }
    }

    private static int count(final List<?>... arrays) {
        int count = 0;
        for (final List<?> list : arrays) {
            if (list != null) {
                count += list.size();
            }
        }
        return count;
    }
}
