package personthecat.cavegenerator.world.event;

import net.fabricmc.fabric.api.biome.v1.BiomeModificationContext;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectionContext;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.ConfiguredStructureFeature;
import personthecat.overwritevalidator.annotations.Overwrite;
import personthecat.overwritevalidator.annotations.OverwriteClass;

@OverwriteClass
@SuppressWarnings("deprecation")
public class CaveModificationContext {

    private final BiomeSelectionContext selectionContext;
    private final BiomeModificationContext modificationContext;

    public CaveModificationContext(final BiomeSelectionContext selection, final BiomeModificationContext modification) {
        this.selectionContext = selection;
        this.modificationContext = modification;
    }

    @Overwrite
    public ResourceLocation getBiomeName() {
        return this.selectionContext.getBiomeKey().location();
    }

    @Overwrite
    public boolean removeFeature(final ResourceLocation id) {
        final ResourceKey<ConfiguredFeature<?, ?>> key = ResourceKey.create(Registry.CONFIGURED_FEATURE_REGISTRY, id);
        return this.modificationContext.getGenerationSettings().removeFeature(key);
    }

    @Overwrite
    public boolean removeCarver(final ResourceLocation id) {
        final ResourceKey<ConfiguredWorldCarver<?>> key = ResourceKey.create(Registry.CONFIGURED_CARVER_REGISTRY, id);
        return this.modificationContext.getGenerationSettings().removeCarver(key);
    }

    @Overwrite
    public boolean removeStructure(final ResourceLocation id) {
        final ResourceKey<ConfiguredStructureFeature<?, ?>> key = ResourceKey.create(Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY, id);
        return this.modificationContext.getGenerationSettings().removeStructure(key);
    }
}
