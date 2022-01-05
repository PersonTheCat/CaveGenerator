package personthecat.cavegenerator.presets;

import lombok.AllArgsConstructor;
import org.hjson.JsonObject;
import personthecat.catlib.util.HjsonUtils;
import personthecat.cavegenerator.presets.data.CaveSettings;
import personthecat.cavegenerator.presets.data.OverrideSettings;
import personthecat.cavegenerator.presets.resolver.DecoratorStateResolver;

@AllArgsConstructor
public class CavePreset {

    public final boolean enabled;
    public final CaveSettings settings;
    public final String name;
    public final JsonObject raw;

    public static final String ENABLED_KEY = "enabled";

    public static CavePreset from(final String name, final JsonObject json) {
        if (isPresetEnabled(json)) {
            try {
                final CaveSettings settings = HjsonUtils.readThrowing(CaveSettings.CODEC, json)
                    .withOverrides(HjsonUtils.readThrowing(OverrideSettings.CODEC, json)
                    .withGlobalDecorators(DecoratorStateResolver.resolveBlockStates(json)));
                return new CavePreset(true, settings, name, json);
            } catch (final RuntimeException e) {
                throw new UnsupportedOperationException("todo");
            }
        }
        return new CavePreset(false, CaveSettings.EMPTY, name, json);
    }

    public static boolean isPresetEnabled(final JsonObject json) {
        return HjsonUtils.getBool(json, ENABLED_KEY).orElse(true);
    }
}
