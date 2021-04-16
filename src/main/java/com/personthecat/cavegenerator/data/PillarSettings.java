package com.personthecat.cavegenerator.data;

import com.personthecat.cavegenerator.config.CavePreset;
import com.personthecat.cavegenerator.model.Range;
import com.personthecat.cavegenerator.util.HjsonMapper;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import net.minecraft.block.Block;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.state.IBlockState;
import org.hjson.JsonObject;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Optional;

import static com.personthecat.cavegenerator.util.CommonMethods.*;

@Builder
@FieldNameConstants
@ParametersAreNonnullByDefault
@FieldDefaults(level = AccessLevel.PUBLIC, makeFinal = true)
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class PillarSettings {

    /** The name of this feature to be used globally in serialization. */
    private static final String FEATURE_NAME = CavePreset.Fields.pillars;

    /** Default spawn conditions for all pillar generators. */
    private static final ConditionSettings DEFAULT_CONDITIONS = ConditionSettings.builder()
        .height(Range.of(10, 50)).build();

    /** Conditions for these pillars to spawn. */
    @Default ConditionSettings conditions = DEFAULT_CONDITIONS;

    /** The block making up the body of this pillar. */
    IBlockState state;

    /** A number of pillars to attempt spawning per chunk. */
    @Default int count = 15;

    /** How much vertical space is required to spawn this structure. */
    @Default Range length = Range.of(5, 12);

    /** An optional Block to place and upper and lower corners of this pillar. */
    @Default Optional<BlockStairs> stairBlock = empty();

    public static PillarSettings from(JsonObject json, OverrideSettings overrides) {
        final ConditionSettings conditions = overrides.apply(DEFAULT_CONDITIONS.toBuilder()).build();
        return copyInto(json, builder().conditions(conditions));
    }

    public static PillarSettings from(JsonObject json) {
        return copyInto(json, builder());
    }

    private static PillarSettings copyInto(JsonObject json, PillarSettingsBuilder builder) {
        final PillarSettings original = builder.build();
        return new HjsonMapper(json)
            .mapRequiredState(Fields.state, FEATURE_NAME, builder::state)
            .mapSelf(o -> builder.conditions(ConditionSettings.from(o, original.conditions)))
            .mapInt(Fields.count, builder::count)
            .mapRange(Fields.length, builder::length)
            .mapState(Fields.stairBlock, s -> builder.stairBlock(full(toStairBlock(s))))
            .release(builder::build);
    }

    private static BlockStairs toStairBlock(IBlockState state) {
        final Block block = state.getBlock();
        if (block instanceof BlockStairs) {
            return (BlockStairs) block;
        }
        throw runExF("Error: the input block, {}, is not a valid stair block.", block.toString());
    }

}
