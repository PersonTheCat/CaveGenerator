package personthecat.cavegenerator.presets.validator;

import com.mojang.serialization.DataResult;
import personthecat.cavegenerator.presets.data.ShellSettings;

public class ShellValidator {

    private ShellValidator() {}

    public static DataResult<ShellSettings> apply(final ShellSettings s) {
        return DataResult.success(s);
    }
}
