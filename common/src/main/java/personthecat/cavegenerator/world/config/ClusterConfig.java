package personthecat.cavegenerator.world.config;

import lombok.AllArgsConstructor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import org.apache.commons.lang3.tuple.Pair;
import personthecat.catlib.data.Range;

import java.util.List;
import java.util.Set;

@AllArgsConstructor
public class ClusterConfig {
    public final ConditionConfig conditions;
    public final List<Pair<BlockState, Integer>> states;
    public final double selectionThreshold;
    public final double chance;
    public final double integrity;
    public final Range radiusX;
    public final Range radiusY;
    public final Range radiusZ;
    public final Range centerHeight;
    public final Set<BlockState> matchers;
    public final boolean spawnInAir;

    public boolean canSpawn(final BlockState state) {
        if (Blocks.AIR.equals(state.getBlock())) {
            return this.spawnInAir;
        }
        if (this.matchers.isEmpty()) {
            // By default, only replace stone blocks.
            return !Blocks.BEDROCK.equals(state.getBlock()) && Material.STONE.equals(state.getMaterial());
        }
        return this.matchers.contains(state);
    }
}
