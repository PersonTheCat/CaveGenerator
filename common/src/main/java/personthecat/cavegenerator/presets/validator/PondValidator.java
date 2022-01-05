package personthecat.cavegenerator.presets.validator;

import com.mojang.serialization.DataResult;
import personthecat.cavegenerator.presets.data.PondSettings;

public class PondValidator {

    private PondValidator() {}

    public static DataResult<PondSettings> apply(final PondSettings s) {
        return DataResult.success(s);
    }
}
