package personthecat.cavegenerator.presets.validator;

import com.mojang.serialization.DataResult;
import personthecat.cavegenerator.presets.data.RavineSettings;

public class RavineValidator {

    private RavineValidator() {}

    public static DataResult<RavineSettings> apply(final RavineSettings s) {
        return DataResult.success(s);
    }
}
