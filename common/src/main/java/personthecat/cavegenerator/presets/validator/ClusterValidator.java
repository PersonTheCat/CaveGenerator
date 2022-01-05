package personthecat.cavegenerator.presets.validator;

import com.mojang.serialization.DataResult;
import personthecat.cavegenerator.presets.data.ClusterSettings;

public class ClusterValidator {

    private ClusterValidator() {}

    public static DataResult<ClusterSettings> apply(final ClusterSettings s) {
        return DataResult.success(s);
    }
}
