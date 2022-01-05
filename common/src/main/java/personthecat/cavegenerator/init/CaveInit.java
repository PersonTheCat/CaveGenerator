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
import personthecat.catlib.util.HjsonUtils;
import personthecat.cavegenerator.CaveRegistries;
import personthecat.cavegenerator.model.SeedStorage;
import personthecat.cavegenerator.presets.CavePreset;
import personthecat.cavegenerator.config.Cfg;
import personthecat.cavegenerator.presets.PresetReader;
import personthecat.cavegenerator.world.GeneratorController;

import java.io.File;
import java.util.*;

import static personthecat.catlib.util.PathUtils.extension;
import static personthecat.catlib.io.FileIO.mkdirsOrThrow;
import static personthecat.cavegenerator.io.ModFolders.GENERATED_DIR;
import static personthecat.cavegenerator.io.ModFolders.IMPORT_DIR;
import static personthecat.cavegenerator.io.ModFolders.PRESET_DIR;

@Log4j2
public class CaveInit {

    /** A list of valid extensions to compare against presets. */
    private static final List<String> EXTENSIONS = Arrays.asList("hjson", "json", "cave");

    /**
     * Loads all of the enabled presets located inside of <code>cavegenerator/presets</code>.
     *
     * @return A map of preset name -> preset model.
     */
    public static Map<String, CavePreset> initPresets() {
        // Verify the folders' integrity before proceeding.
        mkdirsOrThrow(PRESET_DIR, IMPORT_DIR);
        // Handle all files in the preset directory.
        final Map<String, CavePreset> presets = PresetReader.loadPresets(PRESET_DIR, IMPORT_DIR);

        // If necessary, automatically write the expanded values.
        if (Cfg.AUTO_GENERATE.getAsBoolean()) {
            mkdirsOrThrow(GENERATED_DIR);
            presets.forEach((name, preset) -> {
                final File file = new File(GENERATED_DIR, name + ".cave");
                HjsonUtils.writeJson(preset.raw, file).unwrap();
            });
        }
        // Inform the user of which presets were loaded.
        printLoadedPresets(presets);
        return presets;
    }

    /**
     * Prints which presets are currently loaded and whether they are enabled.
     *
     * @param presets A map of preset name -> preset model.
     */
    private static void printLoadedPresets(final Map<String, CavePreset> presets) {
        for (final Map.Entry<String, CavePreset> entry : presets.entrySet()) {
            final String enabled = entry.getValue().enabled ? "enabled" : "disabled";
            log.info("{} is {}.", entry.getKey(), enabled);
        }
    }

    /**
     * Converts every cave preset model into a generator controller using the current seed info.
     *
     * @return A map of preset name -> generator controller.
     */
    public static Map<String, GeneratorController> initControllers() {
        if (CaveRegistries.PRESETS.isEmpty()) {
            return Collections.emptyMap();
        }
        final SeedStorage.Info seedInfo = CaveRegistries.CURRENT_SEED.get();
        final Map<String, GeneratorController> controllers = new TreeMap<>();
        for (final Map.Entry<String, CavePreset> entry : CaveRegistries.PRESETS.entrySet()) {
            if (entry.getValue().enabled) {
                controllers.put(entry.getKey(), entry.getValue().settings.compile(seedInfo.rand, seedInfo.seed));
            }
        }
        return controllers;
    }

    /**
     * Parses the disable feature config entries for an up to date set of disabled features at
     * this point in time.
     *
     * @return A map of registry key -> disabled ids.
     */
    public static Map<ResourceKey<?>, List<ResourceLocation>> initDisabledFeatures() {
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
            } else {
                log.error("Invalid carver id. Cannot disable: {}", id);
            }
        }
        return disabledCarvers.build();
    }

    /**
     * Generates a list of {@link ConfiguredFeature} resource locations to be disabled by the
     * mod. This list is also immutable so that the the holding registry is still safe.
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
            } else {
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
            } else {
                log.error("Invalid structure id. Cannot disable: {}", id);
            }
        }
        return disabledStructures.build();
    }

    /**
     * Determines whether the provided file is in one of the accepted formats.
     *
     * @param file A file which may or may not be a preset.
     * @return <code>true</code>, if the file can be accepted.
     */
    public static boolean validExtension(final File file) {
        return EXTENSIONS.contains(extension(file));
    }
}