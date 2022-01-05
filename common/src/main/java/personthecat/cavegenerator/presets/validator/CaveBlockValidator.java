package personthecat.cavegenerator.presets.validator;

import com.mojang.serialization.DataResult;
import personthecat.cavegenerator.presets.data.CaveBlockSettings;

public class CaveBlockValidator {

    private CaveBlockValidator() {}

    public static DataResult<CaveBlockSettings> apply(final CaveBlockSettings s) {
        return DataResult.success(s);
    }
}
