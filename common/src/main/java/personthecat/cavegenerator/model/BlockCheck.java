package personthecat.cavegenerator.model;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.hjson.JsonArray;
import org.hjson.JsonObject;
import org.hjson.JsonValue;
import personthecat.catlib.util.HjsonMapper;
import personthecat.cavegenerator.data.StructureSettings;

import java.util.List;

import static personthecat.catlib.exception.Exceptions.jsonFormatEx;

@Builder
@FieldNameConstants
@FieldDefaults(level = AccessLevel.PUBLIC, makeFinal = true)
public class BlockCheck {

    /** The specific blocks to find. */
    List<BlockState> matchers;

    /** The positions where this block should be found. */
    List<BlockPos> positions;

    public static BlockCheck fromValue(JsonValue json) {
        if (json.isObject()) {
            return from(json.asObject());
        } else if (json.isArray()) {
            return from(json.asArray());
        }
        throw jsonFormatEx("Expected object or array: {}", json);
    }

    public static BlockCheck from(JsonObject json) {
        return new HjsonMapper<>(StructureSettings.Fields.blockChecks, BlockCheckBuilder::build)
            .mapRequiredStateList(Fields.matchers, BlockCheckBuilder::matchers)
            .mapRequiredBlockPosList(Fields.positions, BlockCheckBuilder::positions)
            .create(json, builder());
    }

    public static BlockCheck from(JsonArray json) {
        final JsonArray matchers = new JsonArray();
        final JsonArray positions = new JsonArray();
        for (JsonValue value : json) {
            if (value.isString()) {
                matchers.add(value);
            } else if (value.isArray()) {
                positions.add(value);
            } else {
                throw jsonFormatEx("Expected array or string: {}", value);
            }
        }
        // This allows all other error messages and requirements to be consistent.
        return from(new JsonObject().add(Fields.matchers, matchers).add(Fields.positions, positions));
    }
}
