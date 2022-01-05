package personthecat.cavegenerator.presets.validator;

import com.mojang.serialization.DataResult;
import personthecat.cavegenerator.presets.data.WallDecoratorSettings;

public class WallDecoratorValidator {

    private WallDecoratorValidator() {}

    public static DataResult<WallDecoratorSettings> apply(final WallDecoratorSettings s) {
        return DataResult.success(s);
    }
}
