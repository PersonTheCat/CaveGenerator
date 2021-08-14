package personthecat.cavegenerator.data;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.hjson.JsonObject;
import personthecat.catlib.data.Range;
import personthecat.catlib.util.HjsonMapper;
import personthecat.cavegenerator.config.CavePreset;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Optional;

import static java.util.Optional.empty;
import static personthecat.catlib.exception.Exceptions.jsonFormatEx;
import static personthecat.catlib.util.Shorthand.full;

@Builder
@FieldNameConstants
@ParametersAreNonnullByDefault
@FieldDefaults(level = AccessLevel.PUBLIC, makeFinal = true)
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class PillarSettings {

    /** Default spawn conditions for all pillar generators. */
    private static final ConditionSettings DEFAULT_CONDITIONS = ConditionSettings.builder()
        .height(Range.of(10, 50)).build();

    /** Conditions for these pillars to spawn. */
    @Default ConditionSettings conditions = DEFAULT_CONDITIONS;

    /** The block making up the body of this pillar. */
    BlockState state;

    /** A number of pillars to attempt spawning per chunk. */
    @Default int count = 15;

    /** How much vertical space is required to spawn this structure. */
    @Default Range length = Range.of(5, 12);

    /** An optional Block to place and upper and lower corners of this pillar. */
    @Default Optional<StairBlock> stairBlock = empty();

    public static PillarSettings from(final JsonObject json, final OverrideSettings overrides) {
        final ConditionSettings conditions = overrides.apply(DEFAULT_CONDITIONS.toBuilder()).build();
        return copyInto(json, builder().conditions(conditions));
    }

    public static PillarSettings from(final JsonObject json) {
        return copyInto(json, builder());
    }

    private static PillarSettings copyInto(final JsonObject json, final PillarSettingsBuilder builder) {
        final PillarSettings original = builder.build();
        return new HjsonMapper<>(CavePreset.Fields.pillars, PillarSettingsBuilder::build)
            .mapRequiredState(Fields.state, PillarSettingsBuilder::state)
            .mapSelf((b, o) -> b.conditions(ConditionSettings.from(o, original.conditions)))
            .mapInt(Fields.count, PillarSettingsBuilder::count)
            .mapRange(Fields.length, PillarSettingsBuilder::length)
            .mapState(Fields.stairBlock, (b, s) -> b.stairBlock(full(toStairBlock(s))))
            .create(builder, json);
    }

    private static StairBlock toStairBlock(final BlockState state) {
        final Block block = state.getBlock();
        if (block instanceof StairBlock) {
            return (StairBlock) block;
        }
        throw jsonFormatEx("Error: the input block, {}, is not a valid stair block.", block);
    }

}
