package com.personthecat.cavegenerator.data;

import com.personthecat.cavegenerator.config.CavePreset;
import com.personthecat.cavegenerator.model.Direction;
import com.personthecat.cavegenerator.model.Range;
import com.personthecat.cavegenerator.util.HjsonMapper;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.structure.template.PlacementSettings;
import org.hjson.JsonObject;

import java.util.Collections;
import java.util.List;

@FieldNameConstants
@Builder(toBuilder = true)
@FieldDefaults(level = AccessLevel.PUBLIC, makeFinal = true)
public class StructureSettings {

    /** The name of this feature to be used globally in serialization. */
    private static final String FEATURE_NAME = CavePreset.Fields.structures;

    /** Default spawn conditions for all structure generators. */
    private static final ConditionSettings DEFAULT_CONDITIONS = ConditionSettings.builder()
        .height(Range.of(10, 50)).build();

    /** Conditions for these tunnels to spawn. */
    @Default ConditionSettings conditions = DEFAULT_CONDITIONS;

    /** Either the name of the structure file or a structure resource ID. */
    String name;

    /** Vanilla placement settings for the template being spawned. */
    @Default PlacementSettings placement = new PlacementSettings().setReplacedBlock(Blocks.STONE);

    /** A list of source blocks that this structure can spawn on top of. */
    @Default List<IBlockState> matchers = Collections.singletonList(Blocks.STONE.getDefaultState());

    /** A list of potential surface directions for this structure to spawn. */
    @Default Direction.Container directions = new Direction.Container();

    /** A list of relative coordinates which must be air. */
    @Default List<BlockPos> airChecks = Collections.emptyList();

    /** A list of relative coordinates which must be solid. */
    @Default List<BlockPos> solidChecks = Collections.emptyList();

    /** A list of relative coordinates which must be non-solid. */
    @Default List<BlockPos> nonSolidChecks = Collections.emptyList();

    /** A list of relative coordinates which must be water. */
    @Default List<BlockPos> waterChecks = Collections.emptyList();

    /** A 3-dimensional offset for when structure spawns. */
    @Default BlockPos offset = BlockPos.ORIGIN;

    /** The 0-1 chance that any spawn attempt is successful, if *also* valid. */
    @Default float chance = 1.0F;

    /** The number of spawn attempts per chunk. */
    @Default int count = 1;

    /** Whether to display the spawn coordinates of this structure in the log. */
    @Default boolean debugSpawns = false;

    /** Whether to rotate this structure randomly. */
    @Default boolean rotateRandomly = false;

    public static StructureSettings from(JsonObject json, OverrideSettings overrides) {
        final ConditionSettings conditions = overrides.apply(DEFAULT_CONDITIONS.toBuilder()).build();
        return copyInto(json, builder().conditions(conditions));
    }

    public static StructureSettings from(JsonObject json) {
        return copyInto(json, builder());
    }

    private static StructureSettings copyInto(JsonObject json, StructureSettingsBuilder builder) {
        final StructureSettings original = builder.build();
        return new HjsonMapper(json)
            .mapRequiredString(Fields.name, FEATURE_NAME, builder::name)
            .mapSelf(o -> builder.conditions(ConditionSettings.from(o, original.conditions)))
            .mapPlacementSettings(builder::placement)
            .mapStateList(Fields.matchers, builder::matchers)
            .mapDirectionList(Fields.directions, l -> builder.directions(Direction.Container.from(l)))
            .mapBlockPosList(Fields.airChecks, builder::airChecks)
            .mapBlockPosList(Fields.solidChecks, builder::solidChecks)
            .mapBlockPosList(Fields.nonSolidChecks, builder::nonSolidChecks)
            .mapBlockPosList(Fields.waterChecks, builder::waterChecks)
            .mapBlockPos(Fields.offset, builder::offset)
            .mapFloat(Fields.chance, builder::chance)
            .mapInt(Fields.count, builder::count)
            .mapBool(Fields.debugSpawns, builder::debugSpawns)
            .mapBool(Fields.rotateRandomly, builder::rotateRandomly)
            .release(builder::build);
    }

}
