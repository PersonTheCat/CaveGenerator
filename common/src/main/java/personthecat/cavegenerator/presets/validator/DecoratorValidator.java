package personthecat.cavegenerator.presets.validator;

import com.mojang.serialization.DataResult;
import personthecat.cavegenerator.presets.data.DecoratorSettings;

public class DecoratorValidator {

    private DecoratorValidator() {}

    public static DataResult<DecoratorSettings> apply(final DecoratorSettings s) {
        return DataResult.success(s);
    }
}
