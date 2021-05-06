package com.personthecat.cavegenerator.config;

import com.personthecat.cavegenerator.data.*;
import com.personthecat.cavegenerator.util.HjsonMapper;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.hjson.JsonObject;

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

    /** Regular vines that can spawn anywhere underground. */
    @Default List<VineSettings> vines = Collections.emptyList();

    /** Regular NBT structures which can be placed throughout the world. */
    @Default List<StructureSettings> structures = Collections.emptyList();

    JsonObject raw;

    public static CavePreset from(JsonObject json) {
        final OverrideSettings overrides = OverrideSettings.from(json);
        final CavePresetBuilder builder = builder().overrides(overrides).raw(json);
        return new HjsonMapper(json)
            .mapBool(Fields.enabled, builder::enabled)
            .mapArray(Fields.tunnels, o -> TunnelSettings.from(o, overrides), builder::tunnels)
            .mapArray(Fields.ravines, o -> RavineSettings.from(o, overrides), builder::ravines)
            .mapArray(Fields.caverns, o -> CavernSettings.from(o, overrides), builder::caverns)
            .mapArray(Fields.burrows, o -> BurrowSettings.from(o, overrides), builder::burrows)
            .mapArray(Fields.layers, o -> LayerSettings.from(o, overrides), builder::layers)
            .mapArray(Fields.clusters, o -> ClusterSettings.from(o, overrides), builder::clusters)
            .mapArray(Fields.stalactites, o -> StalactiteSettings.from(o, overrides), builder::stalactites)
            .mapArray(Fields.pillars, o -> PillarSettings.from(o, overrides), builder::pillars)
            .mapArray(Fields.vines, o -> VineSettings.from(o, overrides), builder::vines)
            .mapArray(Fields.structures, o -> StructureSettings.from(o, overrides), builder::structures)
            .release(builder::build);
    }
}
