package personthecat.cavegenerator.presets.data;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.ArrayUtils;
import org.hjson.JsonObject;
import personthecat.catlib.data.Range;
import personthecat.catlib.util.HjsonMapper;
import personthecat.catlib.util.HjsonUtils;
import personthecat.catlib.util.JsonTransformer;
import personthecat.cavegenerator.presets.CavePreset;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static java.util.Optional.empty;
import static personthecat.catlib.util.Shorthand.full;

/**
 * Any settings that can be written at the top level to serve as default values.
 *
 * Todo: this should be handled directly in the JSON.
 */
@Builder
@FieldNameConstants
@FieldDefaults(level = AccessLevel.PUBLIC, makeFinal = true)
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class OverrideSettings {

    /** Whether the biomes list should be treated as a blacklist. */
    @Default Optional<Boolean> blacklistBiomes = empty();

    /** A list of biomes in which this feature can spawn. */
    @Default Optional<List<Biome>> biomes = empty();

    /** Whether the dimension list should be treated as a blacklist. */
    @Default Optional<Boolean> blacklistDimensions = empty();

    /** A list of dimensions in which this feature can spawn.. */
    @Default Optional<List<Integer>> dimensions = empty();

    /** Height restrictions for the current feature. */
    @Default Optional<Range> height = empty();

    /** Settings used for augmenting the maximum height level. */
    @Default Optional<NoiseMapSettings> floor = empty();

    /** Settings used for augmenting the minimum height level. */
    @Default Optional<NoiseMapSettings> ceiling = empty();

    /** Settings used to determine whether this feature can spawn when given 2 coordinates. */
    @Default Optional<NoiseRegionSettings> region = empty();

    /** Settings used to control 3-dimensional placement of this feature. */
    @Default Optional<NoiseSettings> noise = empty();

    /** All of the blocks which can be replaced by this decorator. */
    @Default Optional<List<BlockState>> replaceableBlocks = empty();

    /** Whether to include the blocks from various other features in this list. */
    @Default Optional<Boolean> replaceDecorators = empty();

    /** A list of blocks for this carver to place instead of air. */
    @Default Optional<List<CaveBlockSettings>> caveBlocks = empty();

    /** A list of blocks to replace the walls of this carver with. */
    @Default Optional<List<WallDecoratorSettings>> wallDecorators = empty();

    /** A variant of wall decorators which can spawn multiple levels deep in the ground only. */
    @Default Optional<List<PondSettings>> ponds = empty();

    /** Variant of wall decorators which can spawn multiple levels deep without directionality. */
    @Default Optional<ShellSettings> shell = empty();

    /** Optional branch overrides for all tunnels. */
    @Default Optional<TunnelSettings> branches = empty();

    /** Optional room overrides for all tunnels. For backwards compatibility. */
    @Default Optional<RoomSettings> rooms = empty();

    /** An internal-only list of decorator blocks at <em>every level</em>. */
    @Default List<BlockState> globalDecorators = Collections.emptyList();

    public static OverrideSettings from(final JsonObject json) {
        final OverrideSettingsBuilder builder = builder().globalDecorators(getAllDecorators(json));
        return new HjsonMapper<>("<>", OverrideSettingsBuilder::build)
            .mapBool(Fields.blacklistBiomes, (s, b) -> s.blacklistBiomes(full(b)))
            .mapBiomes(Fields.biomes, (b, l) -> b.biomes(full(l)))
            .mapBool(Fields.blacklistDimensions, (s, b) -> s.blacklistBiomes(full(b)))
            .mapIntList(Fields.dimensions, (b, l) -> b.dimensions(full(l)))
            .mapRange(Fields.height, (b, r) -> b.height(full(r)))
            .mapObject(Fields.floor, (b, o) -> b.floor(full(NoiseMapSettings.from(o))))
            .mapObject(Fields.ceiling, (b, o) -> b.ceiling(full(NoiseMapSettings.from(o))))
            .mapObject(Fields.noise, (b, o) -> b.noise(full(NoiseSettings.from(o))))
            .mapStateList(Fields.replaceableBlocks, (b, l) -> b.replaceableBlocks(full(l)))
            .mapBool(Fields.replaceDecorators, (s, b) -> s.replaceDecorators(full(b)))
            .mapArray(Fields.caveBlocks, CaveBlockSettings::from, (b, l) -> b.caveBlocks(full(l)))
            .mapArray(Fields.wallDecorators, WallDecoratorSettings::from, (b, l) -> b.wallDecorators(full(l)))
            .mapArray(Fields.ponds, PondSettings::from, (b, l) -> b.ponds(full(l)))
            .mapObject(Fields.shell, (b, s) -> b.shell(full(ShellSettings.from(s))))
            .mapObject(Fields.branches, (s, b) -> s.branches(full(TunnelSettings.from(b))))
            .mapObject(Fields.rooms, (b, r) -> b.rooms(full(RoomSettings.from(r))))
            .create(builder, json);
    }

    public ConditionSettings.ConditionSettingsBuilder apply(final ConditionSettings.ConditionSettingsBuilder builder) {
        this.blacklistBiomes.ifPresent(builder::blacklistBiomes);
        this.biomes.ifPresent(builder::biomes);
        this.blacklistDimensions.ifPresent(builder::blacklistDimensions);
        this.dimensions.ifPresent(builder::dimensions);
        this.height.ifPresent(builder::height);
        this.floor.ifPresent(n -> builder.floor(full(n)));
        this.ceiling.ifPresent(n -> builder.ceiling(full(n)));
        this.region.ifPresent(n -> builder.region(full(n)));
        this.noise.ifPresent(n -> builder.noise(full(n)));
        return builder;
    }

    public DecoratorSettings.DecoratorSettingsBuilder apply(final DecoratorSettings.DecoratorSettingsBuilder builder) {
        builder.globalDecorators(this.globalDecorators);
        this.replaceableBlocks.ifPresent(builder::replaceableBlocks);
        this.replaceDecorators.ifPresent(builder::replaceDecorators);
        this.caveBlocks.ifPresent(builder::caveBlocks);
        this.wallDecorators.ifPresent(builder::wallDecorators);
        this.ponds.ifPresent(builder::ponds);
        this.shell.ifPresent(builder::shell);
        return builder;
    }

    public TunnelSettings.TunnelSettingsBuilder apply(final TunnelSettings.TunnelSettingsBuilder builder) {
        this.branches.ifPresent(b -> builder.branches(full(b)));
        this.rooms.ifPresent(r -> builder.rooms(full(r)));
        return builder;
    }

    private static List<BlockState> getAllDecorators(final JsonObject json) {
        final List<BlockState> decorators = new ArrayList<>();
        addAllDecorators(decorators, json); // Top-level overrides
        addAllDecorators(decorators, json, Fields.branches);
        addAllDecorators(decorators, json, Fields.rooms);
        addAllDecorators(decorators, json, CavePreset.Fields.tunnels);
        addAllDecorators(decorators, json, CavePreset.Fields.tunnels, TunnelSettings.Fields.branches);
        addAllDecorators(decorators, json, CavePreset.Fields.ravines);
        addAllDecorators(decorators, json, CavePreset.Fields.caverns);
        addAll(decorators, json, ClusterSettings.Fields.states, CavePreset.Fields.clusters);
        addAll(decorators, json, LayerSettings.Fields.state, CavePreset.Fields.layers);
        return decorators;
    }

    private static void addAllDecorators(final List<BlockState> decorators, final JsonObject json, final String... path) {
        JsonTransformer.withPath(ArrayUtils.add(path, DecoratorSettings.Fields.wallDecorators))
            .forEach(json, j -> addAll(decorators, j, WallDecoratorSettings.Fields.states));
        JsonTransformer.withPath(ArrayUtils.add(path, DecoratorSettings.Fields.caveBlocks))
            .forEach(json, j -> addAll(decorators, j, CaveBlockSettings.Fields.states));
        JsonTransformer.withPath(ArrayUtils.add(path, DecoratorSettings.Fields.ponds))
            .forEach(json, j -> addAll(decorators, j, PondSettings.Fields.states));
        JsonTransformer.withPath(ArrayUtils.addAll(path, DecoratorSettings.Fields.shell, ShellSettings.Fields.decorators))
            .forEach(json, j -> addAll(decorators, j, ShellSettings.Decorator.Fields.states));
    }

    private static void addAll(final List<BlockState> decorators, final JsonObject json, final String field, final String... path) {
        JsonTransformer.withPath(path).forEach(json, j ->
            HjsonUtils.getStateList(j, field).ifPresent(decorators::addAll));
    }
}
