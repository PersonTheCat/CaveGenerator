package com.personthecat.cavegenerator.model;

import com.personthecat.cavegenerator.data.StructureSettings;
import com.personthecat.cavegenerator.util.HjsonMapper;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import org.hjson.JsonArray;
import org.hjson.JsonObject;
import org.hjson.JsonValue;

import java.util.List;

import static com.personthecat.cavegenerator.util.CommonMethods.runExF;

@Builder
@FieldNameConstants
@FieldDefaults(level = AccessLevel.PUBLIC, makeFinal = true)
public class BlockCheck {

    /** The name of this feature to be used globally in serialization. */
    private static final String FEATURE_NAME = StructureSettings.Fields.blockChecks;

    /** The specific blocks to find. */
    List<IBlockState> matchers;

    /** The positions where this block should be found. */
    List<BlockPos> positions;

    public static BlockCheck fromValue(JsonValue json) {
        if (json.isObject()) {
            return from(json.asObject());
        } else if (json.isArray()) {
            return from(json.asArray());
        }
        throw runExF("Expected object or array: {}", json);
    }

    public static BlockCheck from(JsonObject json) {
        final BlockCheckBuilder builder = builder();
        return new HjsonMapper(json)
            .mapRequiredStateList(Fields.matchers, FEATURE_NAME, builder::matchers)
            .mapRequiredBlockPosList(Fields.positions, FEATURE_NAME, builder::positions)
            .release(builder::build);
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
                throw runExF("Expected array or string: {}", value);
            }
        }
        // This allows all other error messages and requirements to be consistent.
        return from(new JsonObject().add(Fields.matchers, matchers).add(Fields.positions, positions));
    }
}
