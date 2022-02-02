package personthecat.cavegenerator.mixin;

import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep.Carving;
import net.minecraft.world.level.levelgen.StructureSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import personthecat.cavegenerator.config.Cfg;
import personthecat.cavegenerator.world.hook.CaveHooks;

@Pseudo
@Mixin(targets = "com.terraforged.mod.chunk.TFChunkGenerator")
public abstract class TerraForgedChunkGeneratorMixin extends ChunkGenerator {

    public TerraForgedChunkGeneratorMixin(final BiomeSource biomes, final StructureSettings structures) {
        super(biomes, structures);
    }

    @Inject(at = @At("HEAD"), method = "applyCarvers(JLnet/minecraft/world/level/biome/BiomeManager;Lnet/minecraft/world/level/chunk/ChunkAccess;Lnet/minecraft/world/level/levelgen/GenerationStep$Carving;)V", cancellable = true)
    public void applyCarvers(final long seed, final BiomeManager biomes, final ChunkAccess chunk, final Carving step, final CallbackInfo ci) {
        if (!Cfg.fallbackCarvers()) {
            CaveHooks.injectCarvers(seed, biomes, chunk, step, this.biomeSource, this.getSeaLevel());
            ci.cancel();
        }
    }

    @Inject(at = @At("TAIL"), method = "applyBiomeDecoration(Lnet/minecraft/server/level/WorldGenRegion;Lnet/minecraft/world/level/StructureFeatureManager;)V")
    public void applyFeatures(final WorldGenRegion region, final StructureFeatureManager structures, final CallbackInfo ci) {
        if (!Cfg.fallbackFeatures()) {
            CaveHooks.injectFeatures(region);
        }
    }
}

