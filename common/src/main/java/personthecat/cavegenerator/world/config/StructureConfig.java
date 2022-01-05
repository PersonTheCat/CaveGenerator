package personthecat.cavegenerator.world.config;

import lombok.AllArgsConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import personthecat.cavegenerator.model.BlockCheck;
import personthecat.cavegenerator.model.Direction;

import java.util.List;
import java.util.Set;

@AllArgsConstructor
public class StructureConfig {
    public final ConditionConfig conditions;
    public final String name;
    public final StructurePlaceSettings placement;
    public final Set<BlockState> matchers;
    public final Direction.Container directions;
    public final List<BlockPos> airChecks;
    public final List<BlockPos> solidChecks;
    public final List<BlockPos> nonSolidChecks;
    public final List<BlockPos> waterChecks;
    public final List<BlockCheck> blockChecks;
    public final boolean checkSurface;
    public final BlockPos offset;
    public final float chance;
    public final int count;
    public final boolean debugSpawns;
    public final String command;
    public final boolean rotateRandomly;
}
