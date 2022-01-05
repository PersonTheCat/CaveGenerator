package personthecat.cavegenerator.presets.validator;

import com.mojang.serialization.DataResult;
import personthecat.cavegenerator.presets.data.ConditionSettings;

public class ConditionValidator {

    private ConditionValidator() {}

    public static DataResult<ConditionSettings> apply(final ConditionSettings s) {
        return DataResult.success(s);
    }
}
