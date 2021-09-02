package personthecat.cavegenerator.world.event;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.GenerationStep.Carving;
import net.minecraft.world.level.levelgen.GenerationStep.Decoration;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import personthecat.catlib.exception.MissingOverrideException;
import personthecat.overwritevalidator.annotations.OverwriteTarget;
import personthecat.overwritevalidator.annotations.PlatformMustOverwrite;

// Todo: Also consider a Mixin-based event in CatLib.
@OverwriteTarget(required = true)
public class CaveModificationContext {

    @PlatformMustOverwrite
    public ResourceLocation getBiomeName() {
        throw new MissingOverrideException();
    }

    @PlatformMustOverwrite
    public boolean removeFeature(final ResourceLocation id) {
        throw new MissingOverrideException();
    }

    @PlatformMustOverwrite
    public boolean removeCarver(final ResourceLocation id) {
        throw new MissingOverrideException();
    }

    @PlatformMustOverwrite
    public boolean removeStructure(final ResourceLocation id) {
        throw new MissingOverrideException();
    }

    @PlatformMustOverwrite
    public void addFeature(final Decoration step, final ConfiguredFeature<?, ?> feature) {
        throw new MissingOverrideException();
    }

    @PlatformMustOverwrite
    public void addCarver(final Carving step, final ConfiguredWorldCarver<?> carver) {
        throw new MissingOverrideException();
    }
}
