package com.personthecat.cavegenerator.data;

import com.personthecat.cavegenerator.model.Range;
import com.personthecat.cavegenerator.util.HjsonMapper;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import net.minecraft.block.Block;
import net.minecraft.block.BlockVine;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import org.hjson.JsonObject;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.personthecat.cavegenerator.util.CommonMethods.runExF;

@Builder
@FieldNameConstants
@FieldDefaults(level = AccessLevel.PUBLIC, makeFinal = true)
public class VineSettings {

    /** A list of the properties required for <code>vineBlock</code> */
    private static final List<IProperty<?>> REQUIRED_PROPERTIES =
        Arrays.asList(BlockVine.NORTH, BlockVine.SOUTH, BlockVine.EAST, BlockVine.WEST);

    /** Default spawn conditions for all vine generators. */
    private static final ConditionSettings DEFAULT_CONDITIONS = ConditionSettings.builder()
        .height(Range.of(30, 50)).build();

    /** The type of block being placed by this generator. Must be a vine. */
    @Default Block vineBlock = Blocks.VINE;

    /** Conditions for these tunnels to spawn. */
    @Default ConditionSettings conditions = DEFAULT_CONDITIONS;

    /** A list of blocks these vines can be placed on, or else anywhere. */
    @Default List<IBlockState> matchers = Collections.emptyList();

    /** How long these vines should be when they initially spawn. */
    @Default Range length = Range.of(1, 5);

    /** The number of tries to spawn per chunk. */
    @Default int count = 10;

    public static VineSettings from(JsonObject json, OverrideSettings overrides) {
        final ConditionSettings conditions = overrides.apply(DEFAULT_CONDITIONS.toBuilder()).build();
        return copyInto(json, builder().conditions(conditions));
    }

    public static VineSettings from(JsonObject json) {
        return copyInto(json, builder());
    }

    public static VineSettings copyInto(JsonObject json, VineSettingsBuilder builder) {
        final VineSettings original = builder.build();
        return new HjsonMapper(json)
            .mapSelf(o -> builder.conditions(ConditionSettings.from(o, original.conditions)))
            .mapState(Fields.vineBlock, s -> builder.vineBlock(checkStates(s)))
            .mapStateList(Fields.matchers, builder::matchers)
            .mapRange(Fields.length, builder::length)
            .mapInt(Fields.count, builder::count)
            .release(builder::build);
    }

    private static Block checkStates(IBlockState state) {
        final Block b = state.getBlock();
        if (state.getPropertyKeys().containsAll(REQUIRED_PROPERTIES)) {
            return b;
        }
        throw runExF("{} ({}) must contain cardinal states (nsew)", b.getRegistryName(), b.getClass().getSimpleName());
    }
}
