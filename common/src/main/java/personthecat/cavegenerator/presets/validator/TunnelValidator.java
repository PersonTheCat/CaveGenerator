package personthecat.cavegenerator.presets.validator;

import com.mojang.serialization.DataResult;
import personthecat.cavegenerator.presets.data.TunnelSettings;

public class TunnelValidator {

    private TunnelValidator() {}

    public static DataResult<TunnelSettings> apply(final TunnelSettings s) {
        return DataResult.success(s);
    }
}
