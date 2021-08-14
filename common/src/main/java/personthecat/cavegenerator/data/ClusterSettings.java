package personthecat.cavegenerator.data;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import org.apache.commons.lang3.tuple.Pair;
import org.hjson.JsonObject;
import personthecat.catlib.data.Range;
import personthecat.catlib.util.HjsonMapper;
import personthecat.cavegenerator.config.CavePreset;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static personthecat.catlib.util.Shorthand.map;

/** Data used for spawning giant clusters of stone through ChunkPrimer. */
@Builder
@FieldNameConstants
@FieldDefaults(level = AccessLevel.PUBLIC, makeFinal = true)
public class ClusterSettings {

    /** The name of this feature to be used globally in serialization. */
    private static final String FEATURE_NAME = CavePreset.Fields.clusters;

    /** Default spawn conditions for all cluster generators. */
    private static final ConditionSettings DEFAULT_CONDITIONS = ConditionSettings.builder().build();

    /** Conditions for these clusters to spawn. */
    @Default ConditionSettings conditions = DEFAULT_CONDITIONS;

    /** A list of states to spawn with equivalent settings. */
    List<Pair<BlockState, Integer>> states;

    /** A value from 0.0 to 92.0 which determines this cluster's frequency. */
    @Default double selectionThreshold = 78.2;

    /** The original value used for indicating spawn rates. */
    @Default double chance = 0.15;

    /** The chance that any individual block will spawn in this cluster. */
    @Default double integrity = 1.0;

    /** Radius on the x-axis. */
    @Default Range radiusX = Range.of(13, 19);

    /** Radius on the y-axis. */
    @Default Range radiusY = Range.of(9, 15);

    /** Radius on the z-axis. */
    @Default Range radiusZ = Range.of(13, 19);

    /** Determines the range of y-coordinates where the center of this cluster may spawn. */
    @Default Range centerHeight = Range.of(24, 40);

    /** An optional set of blocks which must be present (another cluster or layer). */
    @Default List<BlockState> matchers = Collections.emptyList();

    /** Default values for cluster noise. */
    public static final NoiseSettings DEFAULT_NOISE =
        NoiseSettings.builder().frequency(0.0143f).threshold(Range.of(-0.6F)).stretch(0.5f).octaves(1).build();

    public static ClusterSettings from(final JsonObject json, final OverrideSettings overrides) {
        final ConditionSettings conditions = overrides.apply(DEFAULT_CONDITIONS.toBuilder()).build();
        return copyInto(json, builder().conditions(conditions));
    }

    public static ClusterSettings from(final JsonObject json) {
        return copyInto(json, builder());
    }

    private static ClusterSettings copyInto(final JsonObject json, final ClusterSettingsBuilder builder) {
        final ClusterSettings original = builder.build();
        return new HjsonMapper<>(FEATURE_NAME, ClusterSettingsBuilder::build)
            .mapRequiredStateList(Fields.states, ClusterSettings::copyStates)
            .mapSelf((b, o) -> b.conditions(ConditionSettings.from(o, original.conditions)))
            .mapFloat(Fields.chance, ClusterSettings::copyChance)
            .mapFloat(Fields.integrity, ClusterSettingsBuilder::integrity)
            .mapRange("radius", ClusterSettings::copyRadius)
            .mapRange(Fields.radiusX, ClusterSettingsBuilder::radiusX)
            .mapRange(Fields.radiusY, ClusterSettingsBuilder::radiusY)
            .mapRange(Fields.radiusZ, ClusterSettingsBuilder::radiusZ)
            .mapRange(Fields.centerHeight, ClusterSettingsBuilder::centerHeight)
            .mapStateList(Fields.matchers, ClusterSettingsBuilder::matchers)
            .mapInt("seed", ClusterSettings::copySeed)
            .create(builder, json);
    }

    private static void copyStates(final ClusterSettingsBuilder builder, final List<BlockState> states) {
        builder.states(states.stream().map(s -> Pair.of(s, Block.getId(s))).collect(Collectors.toList()));
    }

    private static void copyChance(final ClusterSettingsBuilder builder, final float chance) {
        builder.chance(chance).selectionThreshold((1.0F - chance) * 92.0F);
    }

    private static void copyRadius(final ClusterSettingsBuilder builder, final Range radius) {
        builder.radiusX(radius).radiusY(radius).radiusZ(radius);
    }

    private static void copySeed(final ClusterSettingsBuilder builder, final int seed) {
        // Seed must already be set at this point. The order matters here.
        final List<Pair<BlockState, Integer>> states = Objects.requireNonNull(builder.states, "Out of order");
        builder.states(map(states, p -> Pair.of(p.getLeft(), seed)));
    }

    /** Returns whether this cluster is valid at these coordinates. */
    public boolean canSpawn(final BlockState state) {
        // Todo: add setting to control air spawning
        if (Blocks.AIR.equals(state.getBlock())) {
            return false;
        }
        if (matchers.isEmpty()) {
            // By default, only replace stone blocks.
            return !Blocks.BEDROCK.equals(state.getBlock()) && Material.STONE.equals(state.getMaterial());
        }
        return matchers.contains(state);
    }
}