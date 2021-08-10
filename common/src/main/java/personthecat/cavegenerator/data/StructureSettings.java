package personthecat.cavegenerator.data;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.hjson.JsonObject;
import personthecat.catlib.data.Range;
import personthecat.catlib.util.HjsonMapper;
import personthecat.catlib.util.Shorthand;
import personthecat.cavegenerator.config.CavePreset;
import personthecat.cavegenerator.model.BlockCheck;
import personthecat.cavegenerator.model.Direction;

import java.util.Collections;
import java.util.List;

@Builder
@FieldNameConstants
@FieldDefaults(level = AccessLevel.PUBLIC, makeFinal = true)
public class StructureSettings {

    /** Default spawn conditions for all structure generators. */
    private static final ConditionSettings DEFAULT_CONDITIONS = ConditionSettings.builder()
        .height(Range.of(10, 50)).build();

    /** Conditions for these tunnels to spawn. */
    @Default ConditionSettings conditions = DEFAULT_CONDITIONS;

    /** Either the name of the structure file or a structure resource ID. */
    String name;

    /** Vanilla placement settings for the template being spawned. */
    @Default StructureBlockEntity placement = new StructureBlockEntity();

    /** A list of source blocks that this structure can spawn on top of. */
    @Default List<BlockState> matchers = Collections.singletonList(Blocks.STONE.defaultBlockState());

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

    /** A list of relative positions and the blocks that should be found at each one. */
    @Default List<BlockCheck> blockChecks = Collections.emptyList();

    /** Whether this should always spawn below the surface. */
    @Default boolean checkSurface = true;

    /** A 3-dimensional offset for when structure spawns. */
    @Default BlockPos offset = BlockPos.ZERO;

    /** The 0-1 chance that any spawn attempt is successful, if *also* valid. */
    @Default float chance = 1.0F;

    /** The number of spawn attempts per chunk. */
    @Default int count = 1;

    /** Whether to display the spawn coordinates of this structure in the log. */
    @Default boolean debugSpawns = false;

    /** A command to run whenever this feature is spawned in the world. */
    @Default String command = "";

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
        return new HjsonMapper<>(CavePreset.Fields.structures, StructureSettingsBuilder::build)
            .mapRequiredString(Fields.name, StructureSettingsBuilder::name)
            .mapSelf((b, o) -> b.conditions(ConditionSettings.from(o, original.conditions)))
            .mapPlacementSettings(StructureSettingsBuilder::placement)
            .mapStateList(Fields.matchers, StructureSettingsBuilder::matchers)
            .mapGenericArray(Fields.directions, v -> Shorthand.assertEnumConstant(v.asString(), Direction.class),
                (b, l) -> b.directions(Direction.Container.from(l)))
            .mapBlockPosList(Fields.airChecks, StructureSettingsBuilder::airChecks)
            .mapBlockPosList(Fields.solidChecks, StructureSettingsBuilder::solidChecks)
            .mapBlockPosList(Fields.nonSolidChecks, StructureSettingsBuilder::nonSolidChecks)
            .mapBlockPosList(Fields.waterChecks, StructureSettingsBuilder::waterChecks)
            .mapGenericArray(Fields.blockChecks, BlockCheck::fromValue, StructureSettingsBuilder::blockChecks)
            .mapBool(Fields.checkSurface, StructureSettingsBuilder::checkSurface)
            .mapBlockPos(Fields.offset, StructureSettingsBuilder::offset)
            .mapFloat(Fields.chance, StructureSettingsBuilder::chance)
            .mapInt(Fields.count, StructureSettingsBuilder::count)
            .mapBool(Fields.debugSpawns, StructureSettingsBuilder::debugSpawns)
            .mapString(Fields.command, StructureSettingsBuilder::command)
            .mapBool(Fields.rotateRandomly, StructureSettingsBuilder::rotateRandomly)
            .create(json, builder);
    }

}
