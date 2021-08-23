package personthecat.cavegenerator.mixin;

import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.BitSet;
import java.util.Map;

@Mixin(ProtoChunk.class)
public interface PrimerAccessor extends ChunkAccess {

    @Accessor("heightmaps")
    Map<Heightmap.Types, Heightmap> heightmaps();

    @Accessor("carvingMasks")
    Map<GenerationStep.Carving, BitSet> carvingMasks();
}
