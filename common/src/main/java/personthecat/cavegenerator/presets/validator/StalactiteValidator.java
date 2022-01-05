package personthecat.cavegenerator.presets.validator;

import com.mojang.serialization.DataResult;
import personthecat.cavegenerator.presets.data.StalactiteSettings;

public class StalactiteValidator {

    private StalactiteValidator() {}

    public static DataResult<StalactiteSettings> apply(final StalactiteSettings s) {
        return DataResult.success(s);
    }
}
