package com.personthecat.cavegenerator.data;

import com.personthecat.cavegenerator.model.Range;
import com.personthecat.cavegenerator.util.HjsonMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.experimental.FieldNameConstants;
import net.minecraft.block.state.IBlockState;
import net.minecraft.world.biome.Biome;
import org.hjson.JsonObject;

import java.util.List;
import java.util.Optional;

import static com.personthecat.cavegenerator.util.CommonMethods.empty;
import static com.personthecat.cavegenerator.util.CommonMethods.full;

/** Any settings that can be written at the top level to serve as default values. */
@FieldNameConstants
@AllArgsConstructor
@Builder(toBuilder = true)
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
    @Default Optional<List<IBlockState>> replaceableBlocks = empty();

    /** Whether to include the blocks from various other features in this list. */
    @Default Optional<Boolean> replaceDecorators = empty();

    /** A list of blocks for this carver to place instead of air. */
    @Default Optional<List<CaveBlockSettings>> caveBlocks = empty();

    /** A list of blocks to replace the walls of this carver with. */
    @Default Optional<List<WallDecoratorSettings>> wallDecorators = empty();

    /** Optional branch overrides for all tunnels. */
    @Default Optional<TunnelSettings> branches = empty();

    /** Optional room overrides for all tunnels. For backwards compatibility. */
    @Default Optional<RoomSettings> rooms = empty();

    public static OverrideSettings from(JsonObject json) {
        final OverrideSettingsBuilder builder = builder();
        return new HjsonMapper(json)
            .mapBool(Fields.blacklistBiomes, b -> builder.blacklistBiomes(full(b)))
            .mapBiomes(Fields.biomes, l -> builder.biomes(full(l)))
            .mapBool(Fields.blacklistDimensions, b -> builder.blacklistBiomes(full(b)))
            .mapIntList(Fields.dimensions, l -> builder.dimensions(full(l)))
            .mapRange(Fields.height, r -> builder.height(full(r)))
            .mapObject(Fields.floor, o -> builder.floor(full(NoiseMapSettings.from(o))))
            .mapObject(Fields.ceiling, o -> builder.ceiling(full(NoiseMapSettings.from(o))))
            .mapObject(Fields.noise, o -> builder.noise(full(NoiseSettings.from(o))))
            .mapStateList(Fields.replaceableBlocks, l -> builder.replaceableBlocks(full(l)))
            .mapBool(Fields.replaceDecorators, b -> builder.replaceDecorators(full(b)))
            .mapArray(Fields.caveBlocks, CaveBlockSettings::from, l -> builder.caveBlocks(full(l)))
            .mapArray(Fields.wallDecorators, WallDecoratorSettings::from, l -> builder.wallDecorators(full(l)))
            .mapObject(Fields.branches, b -> builder.branches(full(TunnelSettings.from(b))))
            .mapObject(Fields.rooms, r -> builder.rooms(full(RoomSettings.from(r))))
            .release(builder::build);
    }

    public ConditionSettings.ConditionSettingsBuilder apply(ConditionSettings.ConditionSettingsBuilder builder) {
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

    public DecoratorSettings.DecoratorSettingsBuilder apply(DecoratorSettings.DecoratorSettingsBuilder builder) {
        this.replaceableBlocks.ifPresent(builder::replaceableBlocks);
        this.replaceDecorators.ifPresent(builder::replaceDecorators);
        this.caveBlocks.ifPresent(builder::caveBlocks);
        this.wallDecorators.ifPresent(builder::wallDecorators);
        return builder;
    }

    public TunnelSettings.TunnelSettingsBuilder apply(TunnelSettings.TunnelSettingsBuilder builder) {
        this.branches.ifPresent(b -> builder.branches(full(b)));
        this.rooms.ifPresent(builder::rooms);
        return builder;
    }

}
