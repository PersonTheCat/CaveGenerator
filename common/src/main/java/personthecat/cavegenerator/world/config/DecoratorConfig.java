package personthecat.cavegenerator.world.config;

import lombok.AllArgsConstructor;
import net.minecraft.world.level.block.state.BlockState;
import personthecat.cavegenerator.model.WallDecoratorMap;

import java.util.List;
import java.util.function.Predicate;

@AllArgsConstructor
public class DecoratorConfig {
    public final Predicate<BlockState> canReplace;
    public final List<CaveBlockConfig> caveBlocks;
    public final List<WallDecoratorConfig> wallDecorators;
    public final WallDecoratorMap wallMap;
    public final List<PondConfig> ponds;
    public final ShellConfig shell;
    public final List<BlockState> globalDecorators;
}
