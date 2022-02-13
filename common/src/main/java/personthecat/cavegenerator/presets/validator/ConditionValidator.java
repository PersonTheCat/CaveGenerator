package personthecat.cavegenerator.presets.validator;

import net.minecraft.resources.ResourceLocation;
import personthecat.catlib.data.BiomePredicate;
import personthecat.catlib.data.DimensionPredicate;
import personthecat.catlib.data.JsonPath;
import personthecat.catlib.data.Range;
import personthecat.catlib.event.registry.DynamicRegistries;
import personthecat.catlib.util.McUtils;
import personthecat.cavegenerator.presets.data.ConditionSettings;

import java.util.List;

import static personthecat.cavegenerator.presets.data.ConditionSettings.Fields.biomes;
import static personthecat.cavegenerator.presets.data.ConditionSettings.Fields.ceiling;
import static personthecat.cavegenerator.presets.data.ConditionSettings.Fields.dimensions;
import static personthecat.cavegenerator.presets.data.ConditionSettings.Fields.floor;
import static personthecat.cavegenerator.presets.data.ConditionSettings.Fields.height;
import static personthecat.cavegenerator.presets.data.ConditionSettings.Fields.noise;
import static personthecat.cavegenerator.presets.data.ConditionSettings.Fields.region;
import static personthecat.catlib.data.BiomePredicate.Fields.mods;

public class ConditionValidator {

    private static final Range HEIGHT_BOUNDS = Range.of(0, 256);

    private ConditionValidator() {}

    public static void apply(final ValidationContext ctx, final ConditionSettings s, final JsonPath.Stub path) {
        if (s.biomes != null) {
            biomes(ctx, s.biomes, path.key(biomes));
        }
        if (s.ceiling != null) {
            NoiseValidator.apply(ctx, s.ceiling, path.key(ceiling));
        }
        if (s.dimensions != null) {
            dims(ctx, s.dimensions, path.key(dimensions));
        }
        if (s.floor != null) {
            NoiseValidator.apply(ctx, s.floor, path.key(floor));
        }
        if (s.height != null) {
            height(ctx, s.height, path.key(height));
        }
        if (s.noise != null) {
            NoiseValidator.apply(ctx, s.noise, path.key(noise));
        }
        if (s.region != null) {
            NoiseValidator.apply(ctx, s.region, path.key(region));
        }
    }

    private static void biomes(final ValidationContext ctx, final BiomePredicate biomes, final JsonPath.Stub path) {
        for (final ResourceLocation name : biomes.getNames()) {
            if (!DynamicRegistries.BIOMES.isRegistered(name)) {
                ctx.warn(path, "cfg.errorText.unknownBiome", name);
            }
        }
        mods(ctx, biomes.getMods(), path.key(mods));
    }

    private static void dims(final ValidationContext ctx, final DimensionPredicate dims, final JsonPath.Stub path) {
        for (final ResourceLocation name : dims.getNames()) {
            if (!DynamicRegistries.DIMENSION_TYPES.isRegistered(name)) {
                ctx.warn(path, "cfg.errorText.unknownDimension", name);
            }
        }
        mods(ctx, dims.getMods(), path.key(mods));
    }

    private static void height(final ValidationContext ctx, final Range height, final JsonPath.Stub path) {
        if (!HEIGHT_BOUNDS.contains(height.min) && HEIGHT_BOUNDS.contains(height.max)) {
            ctx.err(path, "cfg.errorText.outOfBounds", height, HEIGHT_BOUNDS);
        }
    }

    private static void mods(final ValidationContext ctx, final List<String> mods, final JsonPath.Stub path) {
        for (final String mod : mods) {
            if (!McUtils.isModLoaded(mod)) {
                ctx.warn(path, "cfg.errorText.unknownMod", mod);
            }
        }
    }
}
