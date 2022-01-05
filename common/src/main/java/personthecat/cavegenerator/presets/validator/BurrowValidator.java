package personthecat.cavegenerator.presets.validator;

import com.mojang.serialization.DataResult;
import personthecat.cavegenerator.presets.data.BurrowSettings;

public class BurrowValidator {

    private BurrowValidator() {}

    public static DataResult<BurrowSettings> apply(final BurrowSettings s) {
        return DataResult.success(s);
    }
}
