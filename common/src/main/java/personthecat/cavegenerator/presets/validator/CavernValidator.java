package personthecat.cavegenerator.presets.validator;

import com.mojang.serialization.DataResult;
import personthecat.cavegenerator.presets.data.CavernSettings;

public class CavernValidator {

    private CavernValidator() {}

    public static DataResult<CavernSettings> apply(final CavernSettings s) {
        return DataResult.success(s);
    }
}
