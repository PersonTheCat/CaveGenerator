package personthecat.cavegenerator.presets.validator;

import com.mojang.serialization.DataResult;
import personthecat.cavegenerator.presets.data.PillarSettings;

public class PillarValidator {

    private PillarValidator() {}

    public static DataResult<PillarSettings> apply(final PillarSettings s) {
        return DataResult.success(s);
    }
}
