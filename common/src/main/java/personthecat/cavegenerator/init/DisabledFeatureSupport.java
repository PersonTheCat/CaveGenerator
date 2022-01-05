package personthecat.cavegenerator.init;

import com.google.common.collect.ImmutableList;
import lombok.extern.log4j.Log4j2;
import net.minecraft.core.Registry;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.ConfiguredStructureFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import personthecat.cavegenerator.config.Cfg;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Log4j2
public class DisabledFeatureSupport {

    /**
     * Parses the disable feature config entries for an updated set of disabled features at
     * this point in time.
     *
     * @return A map of registry key -> disabled ids.
     */
    public static Map<ResourceKey<?>, List<ResourceLocation>> setupDisabledFeatures() {
        final Map<ResourceKey<?>, List<ResourceLocation>> ids = new HashMap<>();
        ids.put(Registry.CONFIGURED_CARVER_REGISTRY, loadDisabledCarvers());
        ids.put(Registry.CONFIGURED_FEATURE_REGISTRY, loadDisabledFeatures());
        ids.put(Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY, loadDisabledStructures());
        return ids;
    }

    /**
     * Generates a list of {@link ConfiguredWorldCarver} resource locations to be disabled by
     * the mod. Note that this list is immutable so that ultimately the holding registry itself
     * remains immutable.
     *
     * @return A list of {@link ResourceLocation}s corresponding to disabled world carvers.
     */
    private static List<ResourceLocation> loadDisabledCarvers() {
        final ImmutableList.Builder<ResourceLocation> disabledCarvers = ImmutableList.builder();
        for (final String id : Cfg.DISABLED_CARVERS.get()) {
            final ResourceLocation key = new ResourceLocation(id);
            if (BuiltinRegistries.CONFIGURED_CARVER.get(key) != null) {
                disabledCarvers.add(key);
            } else { // Todo: error menu
                log.error("Invalid carver id. Cannot disable: {}", id);
            }
        }
        return disabledCarvers.build();
    }

    /**
     * Generates a list of {@link ConfiguredFeature} resource locations to be disabled by the
     * mod. This list is also immutable so that the holding registry is still safe.
     *
     * @return A list of {@link ResourceLocation}s corresponding to disabled configured features.
     */
    private static List<ResourceLocation> loadDisabledFeatures() {
        final ImmutableList.Builder<ResourceLocation> disabledFeatures = ImmutableList.builder();
        for (final String id : Cfg.DISABLED_FEATURES.get()) {
            final ResourceLocation key = new ResourceLocation(id);
            final Feature<?> feature = Registry.FEATURE.get(key);
            if (feature != null) {
                BuiltinRegistries.CONFIGURED_FEATURE.stream()
                    .filter(c -> c.getFeatures().anyMatch(f -> feature.equals(f.feature)))
                    .map(c -> Objects.requireNonNull(BuiltinRegistries.CONFIGURED_FEATURE.getKey(c)))
                    .forEach(disabledFeatures::add);
            } else if (BuiltinRegistries.CONFIGURED_FEATURE.get(key) != null) {
                disabledFeatures.add(key);
            } else { // Todo: error menu
                log.error("Invalid feature id. Cannot disable: {}", id);
            }
        }
        return disabledFeatures.build();
    }

    /**
     * Generates a list of {@link ConfiguredStructureFeature} resource locations to be disabled
     * by the mod. This list is also immutable so that the holding registry is still safe.
     *
     * @return A list of {@link ResourceLocation}s corresponding to disabled configured structures.
     */
    private static List<ResourceLocation> loadDisabledStructures() {
        final ImmutableList.Builder<ResourceLocation> disabledStructures = ImmutableList.builder();
        for (final String id : Cfg.DISABLED_STRUCTURES.get()) {
            final ResourceLocation key = new ResourceLocation(id);
            if (BuiltinRegistries.CONFIGURED_STRUCTURE_FEATURE.get(key) != null) {
                disabledStructures.add(key);
            } else { // Todo: error menu
                log.error("Invalid structure id. Cannot disable: {}", id);
            }
        }
        return disabledStructures.build();
    }
}
