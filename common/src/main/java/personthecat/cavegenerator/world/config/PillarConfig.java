package personthecat.cavegenerator.world.config;

import lombok.AllArgsConstructor;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import personthecat.catlib.data.Range;

import javax.annotation.Nullable;

@AllArgsConstructor
public class PillarConfig {
    public final ConditionConfig conditions;
    public final BlockState state;
    public final int count;
    public final Range length;
    public final @Nullable StairBlock stairBlock;
}
