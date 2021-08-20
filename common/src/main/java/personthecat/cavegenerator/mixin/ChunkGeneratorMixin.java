package personthecat.cavegenerator.mixin;

import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep.Carving;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(ChunkGenerator.class)
public class ChunkGeneratorMixin {

    /**
     * The primary and preferred entry point for the early generators used by this mod.
     *
     * <p>This is preferred as it allows the mod to control the biome search range and
     * other generally non-configurable elements at this stage in world generation.
     *
     * <p>If this mixin has been overwritten or cannot be reached, users may enable an
     * optional fallback generator, which will provide a hook inside of this method as
     * written by any other mod author.
     *
     * @author PersonTheCat
     */
    @Overwrite
    public void applyCarvers(final long seed, final BiomeManager biomes, final ChunkAccess chunk, final Carving step) {
        // Do nothing for now. This demonstrates that everything is configured correctly.
    }
}
