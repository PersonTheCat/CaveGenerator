package personthecat.cavegenerator.mixin;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.levelgen.GenerationStep.Carving;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import personthecat.catlib.exception.MissingOverrideException;
import personthecat.cavegenerator.CaveRegistries;
import personthecat.cavegenerator.util.XoRoShiRo;
import personthecat.cavegenerator.world.BiomeSearch;
import personthecat.cavegenerator.world.GeneratorController;
import personthecat.cavegenerator.world.generator.PrimerContext;

@Mixin(ChunkGenerator.class)
public class ChunkGeneratorMixin {

    @Final
    @Shadow
    protected BiomeSource biomeSource;

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
     * Todo: Moving early generate to an even earlier hook could be a great idea.
     * Todo: Implement WorldCarverAdapters to support other cave generators.
     *
     * @author PersonTheCat
     */
    @Overwrite
    public void applyCarvers(final long seed, final BiomeManager biomes, final ChunkAccess chunk, final Carving step) {
        // Todo: find a better place for this (looking for world load event)
        CaveRegistries.CURRENT_SEED.set(new XoRoShiRo(seed), seed);

        final BiomeManager withSource = biomes.withDifferentSource(this.biomeSource);
        final ChunkPos pos = chunk.getPos();
        final BiomeSearch search = BiomeSearch.in(withSource, pos.x, pos.z);
        final int seaLevel = this.getSeaLevel();
        final ProtoChunk primer = (ProtoChunk) chunk;
        final PrimerContext ctx = new PrimerContext(withSource, search, seed, seaLevel, primer);

        ctx.primeHeightmaps();
        for (final GeneratorController controller : CaveRegistries.GENERATORS) {
            controller.earlyGenerate(ctx);
            controller.mapGenerate(ctx);
        }
    }

    @Shadow
    public int getSeaLevel() {
        throw new MissingOverrideException();
    }
}
