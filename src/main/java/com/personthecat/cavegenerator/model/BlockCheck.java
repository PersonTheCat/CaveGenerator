package com.personthecat.cavegenerator.model;

import com.personthecat.cavegenerator.data.StructureSettings;
import com.personthecat.cavegenerator.util.HjsonMapper;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import org.hjson.JsonObject;

import java.util.List;

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

    public static BlockCheck from(JsonObject json) {
        final BlockCheckBuilder builder = builder();
        return new HjsonMapper(json)
            .mapRequiredStateList(Fields.matchers, FEATURE_NAME, builder::matchers)
            .mapRequiredBlockPosList(Fields.positions, FEATURE_NAME, builder::positions)
            .release(builder::build);
    }
}
