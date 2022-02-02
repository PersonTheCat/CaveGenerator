package personthecat.cavegenerator.mixin;

import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep.Carving;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import personthecat.cavegenerator.config.Cfg;
import personthecat.cavegenerator.world.hook.CaveHooks;

@Mixin(value = ChunkGenerator.class, priority = 2000)
public abstract class ChunkGeneratorMixin {

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
            CaveHooks.injectCarvers(seed, biomes, chunk, step, this.biomeSource, this.getSeaLevel());
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
            CaveHooks.injectFeatures(region);
        }
    }

    @Shadow
    public abstract int getSeaLevel();
}

