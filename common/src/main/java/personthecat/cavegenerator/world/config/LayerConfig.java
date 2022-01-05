package personthecat.cavegenerator.world.config;

import lombok.AllArgsConstructor;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Set;

@AllArgsConstructor
public class LayerConfig {
    public final ConditionConfig conditions;
    public final BlockState state;
    public final Set<BlockState> matchers;
}
