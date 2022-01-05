package personthecat.cavegenerator.presets.validator;

import com.mojang.serialization.DataResult;
import personthecat.cavegenerator.presets.data.NoiseSettings;

public class NoiseValidator {

    private NoiseValidator() {}

    public static DataResult<NoiseSettings> apply(final NoiseSettings s) {
        return DataResult.success(s);
    }
}
