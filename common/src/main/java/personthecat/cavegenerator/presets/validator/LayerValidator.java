package personthecat.cavegenerator.presets.validator;

import com.mojang.serialization.DataResult;
import personthecat.cavegenerator.presets.data.LayerSettings;

public class LayerValidator {

    private LayerValidator() {}

    public static DataResult<LayerSettings> apply(final LayerSettings s) {
        return DataResult.success(s);
    }
}
