package personthecat.cavegenerator.presets.data;

import com.mojang.serialization.Codec;
import lombok.Builder;
import lombok.experimental.FieldNameConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import personthecat.catlib.data.InvertibleSet;
import personthecat.catlib.data.Range;
import personthecat.catlib.serialization.EasyStateCodec;
import personthecat.cavegenerator.model.BlockCheck;
import personthecat.cavegenerator.model.Direction;
import personthecat.cavegenerator.presets.reader.StructureSettingsReader;
import personthecat.cavegenerator.world.config.ConditionConfig;
import personthecat.cavegenerator.world.config.StructureConfig;

import javax.annotation.Nullable;
import java.util.*;

import static personthecat.catlib.serialization.CodecUtils.dynamic;
import static personthecat.catlib.serialization.CodecUtils.easyList;
import static personthecat.catlib.serialization.CodecUtils.easySet;
import static personthecat.catlib.serialization.DynamicField.extend;
import static personthecat.catlib.serialization.DynamicField.field;
import static personthecat.catlib.serialization.DynamicField.required;

@Builder(toBuilder = true)
@FieldNameConstants
public class StructureSettings implements ConfigProvider<StructureSettings, StructureConfig> {
    @Nullable public final ConditionSettings conditions;
    @Nullable public final String name;
    @Nullable public final StructurePlaceSettings placement;
    @Nullable public final Set<BlockState> matchers;
    @Nullable public final List<Direction> directions;
    @Nullable public final List<BlockPos> airChecks;
    @Nullable public final List<BlockPos> solidChecks;
    @Nullable public final List<BlockPos> nonSolidChecks;
    @Nullable public final List<BlockPos> waterChecks;
    @Nullable public final List<BlockCheck> blockChecks;
    @Nullable public final Boolean checkSurface;
    @Nullable public final BlockPos offset;
    @Nullable public final Float chance;
    @Nullable public final Integer count;
    @Nullable public final Boolean debugSpawns;
    @Nullable public final String command;
    @Nullable public final Boolean rotateRandomly;

    private static final ConditionSettings DEFAULT_CONDITIONS =
        ConditionSettings.builder().height(Range.of(10, 50)).build();

    public static final Codec<StructureSettings> CODEC = dynamic(StructureSettings::builder, StructureSettingsBuilder::build).create(
        extend(ConditionSettings.CODEC, Fields.conditions, s -> s.conditions, (s, c) -> s.conditions = c),
        required(Codec.STRING, Fields.name, s -> s.name, (s, n) -> s.name = n),
        extend(StructureSettingsReader.CODEC, Fields.placement, s -> s.placement, (s, p) -> s.placement = p),
        field(easySet(EasyStateCodec.INSTANCE), Fields.matchers, s -> s.matchers, (s, m) -> s.matchers = m),
        field(easyList(Direction.CODEC), Fields.directions, s -> s.directions, (s, d) -> s.directions = d),
        field(easyList(BlockPos.CODEC), Fields.airChecks, s -> s.airChecks, (s, c) -> s.airChecks = c),
        field(easyList(BlockPos.CODEC), Fields.solidChecks, s -> s.solidChecks, (s, c) -> s.solidChecks = c),
        field(easyList(BlockPos.CODEC), Fields.nonSolidChecks, s -> s.nonSolidChecks, (s, c) -> s.nonSolidChecks = c),
        field(easyList(BlockPos.CODEC), Fields.waterChecks, s -> s.waterChecks, (s, c) -> s.waterChecks = c),
        field(Codec.BOOL, Fields.checkSurface, s -> s.checkSurface, (s, c) -> s.checkSurface = c),
        field(BlockPos.CODEC, Fields.offset, s -> s.offset, (s, o) -> s.offset = o),
        field(Codec.FLOAT, Fields.chance, s -> s.chance, (s, c) -> s.chance = c),
        field(Codec.INT, Fields.count, s -> s.count, (s, c) -> s.count = c),
        field(Codec.BOOL, Fields.debugSpawns, s -> s.debugSpawns, (s, d) -> s.debugSpawns = d),
        field(Codec.STRING, Fields.command, s -> s.command, (s, c) -> s.command = c),
        field(Codec.BOOL, Fields.rotateRandomly, s -> s.rotateRandomly, (s, r) -> s.rotateRandomly = r)
    );

    public Codec<StructureSettings> codec() {
        return CODEC;
    }

    public StructureSettings withOverrides(final OverrideSettings o) {
        if (this.conditions == null) return this;
        return this.toBuilder().conditions(this.conditions.withOverrides(o)).build();
    }

    public StructureConfig compile(final Random rand, final long seed) {
        Objects.requireNonNull(this.name, "name not populated by codec");
        final ConditionSettings conditionsCfg = this.conditions != null ? this.conditions : ConditionSettings.EMPTY;
        final StructurePlaceSettings placement = this.placement != null ? this.placement : new StructurePlaceSettings();
        final Set<BlockState> matchersCfg = this.matchers != null
            ? this.matchers : Collections.singleton(Blocks.STONE.defaultBlockState());
        final List<Direction> directionsCfg = this.directions != null ? this.directions : Collections.emptyList();
        final List<BlockPos> airChecks = this.airChecks != null ? this.airChecks : Collections.emptyList();
        final List<BlockPos> solidChecks = this.solidChecks != null ? this.solidChecks : Collections.emptyList();
        final List<BlockPos> nonSolidChecks = this.nonSolidChecks != null ? this.nonSolidChecks : Collections.emptyList();
        final List<BlockPos> waterChecks = this.waterChecks != null ? this.waterChecks : Collections.emptyList();
        final List<BlockCheck> blockChecks = this.blockChecks != null ? this.blockChecks : Collections.emptyList();
        final boolean checkSurface = this.checkSurface != null ? this.checkSurface : true;
        final BlockPos offset = this.offset != null ? this.offset : BlockPos.ZERO;
        final float chance = this.chance != null ? this.chance : 1.0F;
        final int count = this.count != null ? this.count : 1;
        final boolean debugSpawns = this.debugSpawns != null ? this.debugSpawns : false;
        final String command = this.command != null ? this.command : "";
        final boolean rotateRandomly = this.rotateRandomly != null ? this.rotateRandomly : false;

        final ConditionConfig conditions = conditionsCfg.withDefaults(DEFAULT_CONDITIONS).compile(rand, seed);
        final Set<BlockState> matchers = new InvertibleSet<>(matchersCfg, false).optimize(Collections.emptyList());
        final Direction.Container directions = Direction.Container.from(directionsCfg);

        return new StructureConfig(conditions, this.name, placement, matchers, directions, airChecks,
            solidChecks, nonSolidChecks, waterChecks, blockChecks, checkSurface, offset, chance, count,
            debugSpawns, command, rotateRandomly);
    }
}
