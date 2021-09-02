package personthecat.cavegenerator.world.event;

import net.minecraft.data.BuiltinRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.GenerationStep.Carving;
import net.minecraft.world.level.levelgen.GenerationStep.Decoration;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.ConfiguredStructureFeature;
import net.minecraftforge.common.world.BiomeGenerationSettingsBuilder;
import net.minecraftforge.event.world.BiomeLoadingEvent;
import personthecat.overwritevalidator.annotations.Overwrite;
import personthecat.overwritevalidator.annotations.OverwriteClass;

@OverwriteClass
public class CaveModificationContext {

    private final ResourceLocation biomeName;
    private final BiomeGenerationSettingsBuilder builder;

    public CaveModificationContext(final BiomeLoadingEvent event) {
        this.biomeName = event.getName();
        this.builder = event.getGeneration();
    }

    @Overwrite
    public ResourceLocation getBiomeName() {
        return this.biomeName;
    }

    @Overwrite
    public boolean removeFeature(final ResourceLocation id) {
        final ConfiguredFeature<?, ?> feature = BuiltinRegistries.CONFIGURED_FEATURE.get(id);
        if (feature == null) {
            return false;
        }

        boolean anyRemoved = false;
        for (final Decoration step : Decoration.values()) {
            anyRemoved |= this.builder.getFeatures(step).removeIf(f -> feature.equals(f.get()));
        }
        return anyRemoved;
    }

    @Overwrite
    public boolean removeCarver(final ResourceLocation id) {
        final ConfiguredWorldCarver<?> carver = BuiltinRegistries.CONFIGURED_CARVER.get(id);
        if (carver == null) {
            return false;
        }

        boolean anyRemoved = false;
        for (final Carving step : Carving.values()) {
            anyRemoved |= this.builder.getCarvers(step).removeIf(c -> carver.equals(c.get()));
        }
        return anyRemoved;
    }

    @Overwrite
    public boolean removeStructure(final ResourceLocation id) {
        final ConfiguredStructureFeature<?, ?> structure = BuiltinRegistries.CONFIGURED_STRUCTURE_FEATURE.get(id);
        if (structure == null) {
            return false;
        }
        return this.builder.getStructures().removeIf(s -> structure.equals(s.get()));
    }

    @Overwrite
    public void addFeature(final Decoration step, final ConfiguredFeature<?, ?> feature) {
        this.builder.addFeature(step, feature);
    }

    @Overwrite
    public void addCarver(final Carving step, final ConfiguredWorldCarver<?> carver) {
        this.builder.addCarver(step, carver);
    }
}
