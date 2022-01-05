package personthecat.cavegenerator.presets.validator;

import com.mojang.serialization.DataResult;
import personthecat.cavegenerator.presets.data.RoomSettings;

public class RoomValidator {

    private RoomValidator() {}

    public static DataResult<RoomSettings> apply(final RoomSettings s) {
        return DataResult.success(s);
    }
}
