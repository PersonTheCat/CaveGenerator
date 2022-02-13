package personthecat.cavegenerator.presets.validator;

import personthecat.catlib.data.JsonPath;
import personthecat.cavegenerator.presets.CavePreset;
import personthecat.cavegenerator.presets.data.CaveSettings;
import personthecat.cavegenerator.presets.data.OverrideSettings;

import static personthecat.cavegenerator.presets.data.CaveSettings.Fields.burrows;
import static personthecat.cavegenerator.presets.data.CaveSettings.Fields.caverns;
import static personthecat.cavegenerator.presets.data.CaveSettings.Fields.clusters;
import static personthecat.cavegenerator.presets.data.CaveSettings.Fields.layers;
import static personthecat.cavegenerator.presets.data.CaveSettings.Fields.pillars;
import static personthecat.cavegenerator.presets.data.CaveSettings.Fields.ravines;
import static personthecat.cavegenerator.presets.data.CaveSettings.Fields.stalactites;
import static personthecat.cavegenerator.presets.data.CaveSettings.Fields.structures;
import static personthecat.cavegenerator.presets.data.CaveSettings.Fields.tunnels;

import static personthecat.cavegenerator.presets.validator.ValidationFunction.validate;

public class CavePresetValidator {

    private CavePresetValidator() {}

    public static ValidationContext start(
            final CaveSettings cfg, final OverrideSettings overrides, final JsonPath.Stub path) {

        final ValidationContext ctx = new ValidationContext();
        OverrideValidator.apply(ctx, overrides);
        validate(ctx, cfg.burrows, path.key(burrows), BurrowValidator::apply);
        validate(ctx, cfg.caverns, path.key(caverns), CavernValidator::apply);
        validate(ctx, cfg.clusters, path.key(clusters), ClusterValidator::apply);
        validate(ctx, cfg.layers, path.key(layers), LayerValidator::apply);
        validate(ctx, cfg.pillars, path.key(pillars), PillarValidator::apply);
        validate(ctx, cfg.ravines, path.key(ravines), RavineValidator::apply);
        validate(ctx, cfg.stalactites, path.key(stalactites), StalactiteValidator::apply);
        validate(ctx, cfg.structures, path.key(structures), StructureValidator::apply);
        validate(ctx, cfg.tunnels, path.key(tunnels), TunnelValidator::apply);

        return ctx;
    }
}
