package personthecat.cavegenerator.mixin;

import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.levelgen.GenerationStep.Carving;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import personthecat.catlib.exception.MissingOverrideException;
import personthecat.cavegenerator.CaveRegistries;
import personthecat.cavegenerator.config.Cfg;
import personthecat.cavegenerator.noise.CachedNoiseHelper;
import personthecat.cavegenerator.util.XoRoShiRo;
import personthecat.cavegenerator.world.BiomeSearch;
import personthecat.cavegenerator.world.GeneratorController;
import personthecat.cavegenerator.world.feature.WorldContext;
import personthecat.cavegenerator.world.generator.PrimerContext;
import personthecat.cavegenerator.world.generator.WorldCarverAdapter;

@Mixin(value = ChunkGenerator.class, priority = 2000)
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
     * optional fallback generator, which will provide a hook into this method as if
     * written by any other mod author.
     *
     * @author PersonTheCat
     * @reason Cave Generator's primary entry point for early features
     */
    @Inject(method = "applyCarvers", at = @At("HEAD"), cancellable = true)
    public void applyCarvers(final long seed, final BiomeManager biomes, final ChunkAccess chunk, final Carving step, final CallbackInfo ci) {
        if (!Cfg.fallbackCarvers()) {
            final BiomeManager withSource = biomes.withDifferentSource(this.biomeSource);
            final ChunkPos pos = chunk.getPos();
            final BiomeSearch search = BiomeSearch.in(withSource, pos.x, pos.z);
            final int seaLevel = this.getSeaLevel();
            final ProtoChunk primer = (ProtoChunk) chunk;
            final PrimerContext ctx = new PrimerContext(withSource, search, seed, seaLevel, primer, step);

            if (Cfg.enableOtherGenerators()) {
                WorldCarverAdapter.generate(ctx, this.biomeSource);
            }
            if (step == Carving.AIR) {
                ctx.primeHeightmaps();
                CaveRegistries.CURRENT_SEED.set(new XoRoShiRo(seed), seed);
                for (final GeneratorController controller : CaveRegistries.GENERATORS) {
                    controller.earlyGenerate(ctx);
                    controller.mapGenerate(ctx);
                }
            }
            CachedNoiseHelper.resetAll();
            ci.cancel();
        }
    }

    /**
     * Primary entry point for late features, as used by this mod.
     *
     * <p>Unlike {@link #applyCarvers}, this event provides a regular level
     * accessor which can be used to acquire data about foreign chunks. This
     * is ideal for features spanning from 16 to 32 blocks wide.
     *
     * @author PersonTheCat
     * @reason Cave Generator's primary entry point for late features
     */
    @Inject(at = @At("TAIL"), method = "applyBiomeDecoration")
    public void applyFeatures(final WorldGenRegion region, final StructureFeatureManager structures, final CallbackInfo ci) {
        if (!Cfg.fallbackFeatures()) {
            final WorldContext ctx = new WorldContext(region);
            CaveRegistries.CURRENT_SEED.setIfAbsent(ctx.rand, ctx.seed);

            for (final GeneratorController controller : CaveRegistries.GENERATORS) {
                controller.featureGenerate(ctx);
            }
            CachedNoiseHelper.resetAll();
        }
    }

    @Shadow
    public int getSeaLevel() {
        throw new MissingOverrideException();
    }
}

