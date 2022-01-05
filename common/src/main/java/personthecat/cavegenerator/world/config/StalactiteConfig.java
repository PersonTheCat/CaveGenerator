package personthecat.cavegenerator.world.config;

import lombok.AllArgsConstructor;
import net.minecraft.world.level.block.state.BlockState;
import personthecat.catlib.data.Range;
import personthecat.cavegenerator.presets.data.StalactiteSettings;

import java.util.Set;

@AllArgsConstructor
public class StalactiteConfig {
    public final ConditionConfig conditions;
    public final BlockState state;
    public final StalactiteSettings.Type type;
    public final StalactiteSettings.Size size;
    public final double chance;
    public final Range length;
    public final int space;
    public final boolean symmetrical;
    public final Set<BlockState> matchers;
}
