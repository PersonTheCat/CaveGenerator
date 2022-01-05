package personthecat.cavegenerator.presets.validator;

import com.mojang.serialization.DataResult;
import personthecat.cavegenerator.presets.data.StructureSettings;

public class StructureValidator {

    private StructureValidator() {}

    public static DataResult<StructureSettings> apply(final StructureSettings s) {
        return DataResult.success(s);
    }
}
