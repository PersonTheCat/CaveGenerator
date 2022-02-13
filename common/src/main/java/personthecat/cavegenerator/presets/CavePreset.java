package personthecat.cavegenerator.presets;

import lombok.AllArgsConstructor;
import org.hjson.JsonObject;
import personthecat.catlib.data.JsonPath;
import personthecat.catlib.event.error.LibErrorContext;
import personthecat.catlib.util.HjsonUtils;
import personthecat.cavegenerator.config.Cfg;
import personthecat.cavegenerator.exception.InvalidPresetArgumentException;
import personthecat.cavegenerator.presets.data.CaveSettings;
import personthecat.cavegenerator.presets.data.OverrideSettings;
import personthecat.cavegenerator.presets.resolver.DecoratorStateResolver;
import personthecat.cavegenerator.presets.validator.CavePresetValidator;
import personthecat.cavegenerator.presets.validator.RequiredFieldLocator;
import personthecat.cavegenerator.presets.validator.UnusedFieldLocator;
import personthecat.cavegenerator.presets.validator.ValidationContext;
import personthecat.cavegenerator.util.Reference;
import personthecat.cavegenerator.world.GeneratorController;

import java.util.Optional;
import java.util.Random;

@AllArgsConstructor
public class CavePreset {

    public final CaveSettings settings;
    public final String name;
    public final JsonObject raw;

    public static final String ENABLED_KEY = "enabled";
    public static final String INNER_KEY = "inner";

    public static Optional<CavePreset> from(final String name, final CaveOutput output) {
        if (!isEnabled(output.generated)) {
            return Optional.empty();
        }
        try {
            final CaveSettings raw = HjsonUtils.readThrowing(CaveSettings.CODEC, output.generated);
            final OverrideSettings overrides = HjsonUtils.readThrowing(OverrideSettings.CODEC, output.generated)
                .withGlobalDecorators(DecoratorStateResolver.resolveBlockStates(output.generated));

            final UnusedFieldLocator unusedFields = new UnusedFieldLocator(output.generated);
            if (unusedFields.hasErrors()) {
                LibErrorContext.register(
                    Cfg.warnSeverity(), Reference.MOD, unusedFields.createScreen(name));
            }
            final RequiredFieldLocator requiredFields = new RequiredFieldLocator(output.generated);
            if (requiredFields.hasErrors()) {
                LibErrorContext.register(
                    Cfg.errorSeverity(), Reference.MOD, requiredFields.createScreen(name));
            }
            final ValidationContext ctx = CavePresetValidator.start(raw, overrides, createPath(name));
            if (ctx.hasWarnings()) {
                LibErrorContext.register(
                    Cfg.warnSeverity(), Reference.MOD, ctx.createWarningScreen(name, output));
            }
            if (ctx.hasErrors()) {
                LibErrorContext.register(
                    Cfg.errorSeverity(), Reference.MOD, ctx.createErrorScreen(name, output));
            } else if (!requiredFields.hasErrors()) {
                return Optional.of(new CavePreset(raw.withOverrides(overrides), name, output.generated));
            }
        } catch (final RuntimeException e) {
            LibErrorContext.error(Reference.MOD, new InvalidPresetArgumentException(name, e));
        }
        return Optional.empty();
    }

    public static boolean isEnabled(final JsonObject json) {
        return HjsonUtils.getBool(json, ENABLED_KEY).orElse(true);
    }

    private static JsonPath.Stub createPath(final String name) {
        final JsonPath.Stub path = JsonPath.stub();
        final int index = getIndex(name);
        return index < 0 ? path : path.key(INNER_KEY).index(index);
    }

    private static int getIndex(final String name) {
        final int bracket = name.lastIndexOf('[');
        if (bracket < 0) return bracket;

        final int bracketEnd = name.lastIndexOf(']');
        if (bracketEnd < 0) return bracketEnd;

        try {
            return Integer.parseInt(name.substring(bracket + 1, bracketEnd));
        } catch (final NumberFormatException ignored) {
            return -1;
        }
    }

    public GeneratorController setupController(final Random rand, final long seed) {
        return this.settings.compile(rand, seed);
    }
}
