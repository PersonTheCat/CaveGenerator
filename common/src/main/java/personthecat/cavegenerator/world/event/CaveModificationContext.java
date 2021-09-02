package personthecat.cavegenerator.world.event;

import net.minecraft.resources.ResourceLocation;
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
}
