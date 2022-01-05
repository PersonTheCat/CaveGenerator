package personthecat.cavegenerator.mixin;

import net.minecraft.world.level.levelgen.structure.templatesystem.BlockRotProcessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BlockRotProcessor.class)
public interface BlockRotProcessorAccessor {

    @Accessor
    float getIntegrity();
}
