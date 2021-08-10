package personthecat.cavegenerator.config;

import personthecat.catlib.util.HjsonMapper;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.hjson.JsonObject;
import personthecat.cavegenerator.data.CavernSettings;
import personthecat.cavegenerator.data.ClusterSettings;
import personthecat.cavegenerator.data.BurrowSettings;
import personthecat.cavegenerator.data.LayerSettings;
import personthecat.cavegenerator.data.OverrideSettings;
import personthecat.cavegenerator.data.PillarSettings;
import personthecat.cavegenerator.data.RavineSettings;
import personthecat.cavegenerator.data.StalactiteSettings;
import personthecat.cavegenerator.data.StructureSettings;
import personthecat.cavegenerator.data.TunnelSettings;

import java.util.Collections;
import java.util.List;

@FieldNameConstants
@Builder(toBuilder = true)
@FieldDefaults(level = AccessLevel.PUBLIC, makeFinal = true)
public class CavePreset {

    /** Whether this preset is enabled at all. */
    @Default boolean enabled = true;

    /** A series of conditions, decorators, and miscellaneous features that override other default settings. */
    @Default OverrideSettings overrides = OverrideSettings.builder().build();

    /** Regular vanilla tunnels with various overrides. */
    @Default List<TunnelSettings> tunnels = Collections.emptyList();

    /** Regular vanilla ravines with various overrides. */
    @Default List<RavineSettings> ravines = Collections.emptyList();

    /** Various noise generators which can be configured in extensively. */
    @Default List<CavernSettings> caverns = Collections.emptyList();

    /** Map generator pairs used to produce a sort of noise-based tunnels. */
    @Default List<BurrowSettings> burrows = Collections.emptyList();

    /** A series of layers designed to spawn upward throughout the world in sequence. */
    @Default List<LayerSettings> layers = Collections.emptyList();

    /** Giant spheres with various regular noise restrictions. */
    @Default List<ClusterSettings> clusters = Collections.emptyList();

    /** Large stalactites and stalagmites spawned by this preset. */
    @Default List<StalactiteSettings> stalactites = Collections.emptyList();

    /** Giant pillars with optional corner blocks made from stairs. */
    @Default List<PillarSettings> pillars = Collections.emptyList();

    /** Regular NBT structures which can be placed throughout the world. */
    @Default List<StructureSettings> structures = Collections.emptyList();

    JsonObject raw;

    public static CavePreset from(final JsonObject json) {
        final OverrideSettings overrides = OverrideSettings.from(json);
        final CavePresetBuilder builder = builder().overrides(overrides).raw(json);
        return new HjsonMapper<>("", CavePresetBuilder::build)
            .mapBool(Fields.enabled, CavePresetBuilder::enabled)
            .mapArray(Fields.tunnels, o -> TunnelSettings.from(o, overrides), CavePresetBuilder::tunnels)
            .mapArray(Fields.ravines, o -> RavineSettings.from(o, overrides), CavePresetBuilder::ravines)
            .mapArray(Fields.caverns, o -> CavernSettings.from(o, overrides), CavePresetBuilder::caverns)
            .mapArray(Fields.burrows, o -> BurrowSettings.from(o, overrides), CavePresetBuilder::burrows)
            .mapArray(Fields.layers, o -> LayerSettings.from(o, overrides), CavePresetBuilder::layers)
            .mapArray(Fields.clusters, o -> ClusterSettings.from(o, overrides), CavePresetBuilder::clusters)
            .mapArray(Fields.stalactites, o -> StalactiteSettings.from(o, overrides), CavePresetBuilder::stalactites)
            .mapArray(Fields.pillars, o -> PillarSettings.from(o, overrides), CavePresetBuilder::pillars)
            .mapArray(Fields.structures, o -> StructureSettings.from(o, overrides), CavePresetBuilder::structures)
            .create(json, builder);
    }
}
