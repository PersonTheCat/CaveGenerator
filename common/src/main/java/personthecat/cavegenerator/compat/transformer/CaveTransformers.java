package personthecat.cavegenerator.compat.transformer;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.tuple.Pair;
import org.hjson.JsonArray;
import org.hjson.JsonObject;
import org.hjson.JsonValue;
import personthecat.catlib.data.BiomePredicate;
import personthecat.catlib.util.HjsonUtils;
import personthecat.catlib.util.JsonTransformer;
import personthecat.catlib.util.JsonTransformer.ObjectResolver;
import personthecat.cavegenerator.presets.data.*;
import personthecat.cavegenerator.presets.lang.CaveLangExtension;
import personthecat.fastnoise.data.DomainWarpType;
import personthecat.fastnoise.data.FractalType;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CaveTransformers {

    // Versions where fields were removed;
    private static final String CG_1_0 = "1.0";

    // A list of deprecated field names.
    private static final String MIN_HEIGHT = "minHeight";
    private static final String MAX_HEIGHT = "maxHeight";
    private static final String MIN_LENGTH = "minLength";
    private static final String MAX_LENGTH = "maxLength";
    private static final String MIN_VAL = "minVal";
    private static final String MAX_VAL = "maxVal";
    private static final String NOISE_2D = "noise2D";
    private static final String NOISE_3D = "noise3D";
    private static final String WALL_NOISE = "wallNoise";
    private static final String ISOLATED_CHANCE = "isolatedChance";
    private static final String SCALE = "scale";
    private static final String SCALE_Y = "scaleY";
    private static final String USE_BIOME_BLACKLIST = "useBiomeBlacklist";
    private static final String USE_DIMENSION_BLACKLIST = "useDimensionBlacklist";
    private static final String ANGLE_XZ = "angleXZ";
    private static final String ANGLE_Y = "angleY";
    private static final String TWIST_XZ = "twistXZ";
    private static final String TWIST_Y = "twistY";
    private static final String LARGE_STALACTITES = "largeStalactites";
    private static final String LARGE_STALAGMITES = "largeStalagmites";
    private static final String STONE_CLUSTERS = "stoneClusters";
    private static final String PREFERENCE = "preference";
    private static final String REPLACE_MATCH = "replace_match";
    private static final String REPLACE_ORIGINAL = "replace_original";
    private static final String STONE_LAYERS = "stoneLayers";
    private static final String GIANT_PILLARS = "giantPillars";
    private static final String AIR_MATCHERS = "airMatchers";
    private static final String SOLID_MATCHERS = "solidMatchers";
    private static final String NON_SOLID_MATCHERS = "nonSolidMatchers";
    private static final String WATER_MATCHERS = "waterMatchers";
    private static final String ROOM_CHANCE = "roomChance";
    private static final String FREQUENCY = "frequency";
    private static final String ENABLED = "enabled";
    private static final String BLANK_SLATE = "blankSlate";
    private static final String RADIUS_VARIANCE = "radiusVariance";
    private static final String START_HEIGHT = "startHeight";
    private static final String HEIGHT_VARIANCE = "heightVariance";
    private static final String WIDE = "wide";
    private static final String CHANCE = "chance";
    private static final String FRACTAL = "fractal";
    private static final String PERTURB = "perturb";
    private static final String PERTURB_AMP = "perturbAmp";
    private static final String PERTURB_FREQ = "perturbFreq";
    private static final String STRETCH = "stretch";
    private static final String BLACKLIST_BIOMES = "blacklistBiomes";
    private static final String BLACKLIST_DIMENSIONS = "blacklistDimensions";

    // Other field names needed for performing updates.
    private static final String IMPORTS = CaveLangExtension.IMPORTS;
    private static final String VARIABLES = CaveLangExtension.VARIABLES;

    // Deprecated fields in defaults.cave
    private static final String REPLACE_DIRT_STONE = "REPLACE_DIRT_STONE";
    private static final String VANILLA_ROOM = "VANILLA_ROOM";
    private static final String LAVA_CAVE_BLOCK = "LAVA_CAVE_BLOCK";
    private static final String VANILLA_TUNNELS = "VANILLA_TUNNELS";
    private static final String VANILLA_RAVINES = "VANILLA_RAVINES";

    private static final Pattern DEFAULT_IMPORT = Pattern.compile(CaveLangExtension.DEFAULTS + "\\s*::\\s*(\\w+)");

    private static final Map<String, String> IMPORT_FIELD_MAP =
        ImmutableMap.<String, String>builder()
            .put(REPLACE_DIRT_STONE, DecoratorSettings.Fields.replaceableBlocks)
            .put(VANILLA_ROOM, OverrideSettings.Fields.rooms)
            .put(LAVA_CAVE_BLOCK, OverrideSettings.Fields.caveBlocks)
            .put(VANILLA_TUNNELS, CaveSettings.Fields.tunnels)
            .put(VANILLA_RAVINES, CaveSettings.Fields.ravines)
            .build();

    private static final ObjectResolver CAVE_BLOCK_COMPONENT =
        JsonTransformer.withPath(DecoratorSettings.Fields.caveBlocks)
            .toRange(MIN_HEIGHT, 0, MAX_HEIGHT, 50, CaveBlockSettings.Fields.height)
            .history(NOISE_3D, CaveBlockSettings.Fields.noise)
            .history(CHANCE, CaveBlockSettings.Fields.integrity)
            .freeze();

    private static final ObjectResolver WALL_DECORATOR_COMPONENT =
        JsonTransformer.withPath(DecoratorSettings.Fields.wallDecorators)
            .toRange(MIN_HEIGHT, 10, MAX_HEIGHT, 50, WallDecoratorSettings.Fields.height)
            .history(NOISE_3D, WallDecoratorSettings.Fields.noise)
            .history(CHANCE, WallDecoratorSettings.Fields.integrity)
            .freeze();

    private static final ObjectResolver DECORATOR_COMPONENT =
        JsonTransformer.root()
            .include(CAVE_BLOCK_COMPONENT)
            .include(WALL_DECORATOR_COMPONENT)
            .freeze();

    private static final ObjectResolver CONDITION_COMPONENT =
        JsonTransformer.root()
            .history(USE_BIOME_BLACKLIST, BLACKLIST_BIOMES)
            .history(USE_DIMENSION_BLACKLIST, BLACKLIST_DIMENSIONS)
            .freeze();

    public static final ObjectResolver ROOT_TRANSFORMER =
        JsonTransformer.root()
            .include(DECORATOR_COMPONENT)
            .include(CONDITION_COMPONENT)
            .ifPresent(IMPORTS, CaveTransformers::replaceLegacyDefaults)
            .ifPresent(OverrideSettings.Fields.rooms, CaveTransformers::updateLegacyRoomChance)
            .ifPresent(CaveSettings.Fields.clusters, CaveTransformers::updateClusterRanges)
            .ifPresent(LARGE_STALACTITES, CaveTransformers::condenseStalactites)
            .ifPresent(LARGE_STALAGMITES, CaveTransformers::condenseStalactites)
            .ifPresent(BLANK_SLATE, CaveTransformers::removeBlankSlate)
            .toRange(MIN_HEIGHT, 0, MAX_HEIGHT, 255, ConditionSettings.Fields.height)
            .history(STONE_CLUSTERS, CaveSettings.Fields.clusters)
            .history(STONE_LAYERS, CaveSettings.Fields.layers)
            .history(GIANT_PILLARS, CaveSettings.Fields.pillars)
            .reorder(Arrays.asList(IMPORTS, VARIABLES, ENABLED))
            .freeze();

    private static final ObjectResolver CAVERN_CEILING_COMPONENT =
        JsonTransformer.withPath(ConditionSettings.Fields.ceiling)
            .toRange(MIN_VAL, -17.0, MAX_VAL, -3.0, NoiseSettings.Fields.range)
            .ifPresent(NoiseSettings.Fields.type, CaveTransformers::transformNoiseType)
            .freeze();

    private static final ObjectResolver CAVERN_FLOOR_COMPONENT =
        JsonTransformer.withPath(ConditionSettings.Fields.floor)
            .toRange(MIN_VAL, 0.0, MAX_VAL, 8.0, NoiseSettings.Fields.range)
            .ifPresent(NoiseSettings.Fields.type, CaveTransformers::transformNoiseType)
            .freeze();

    private static final ObjectResolver CAVERN_GENERATORS_COMPONENT =
        JsonTransformer.withPath(CavernSettings.Fields.generators)
            .transform(SCALE, CaveTransformers::transformScale)
            .history(SCALE_Y, STRETCH)
            .transform(PERTURB, CaveTransformers::transformPerturb)
            .history(PERTURB_AMP, NoiseSettings.Fields.warpAmplitude)
            .history(PERTURB_FREQ, NoiseSettings.Fields.warpFrequency)
            .ifPresent(NoiseSettings.Fields.type, CaveTransformers::transformNoiseType)
            .freeze();

    public static final ObjectResolver CAVERN_TRANSFORMER =
        JsonTransformer.withPath(CaveSettings.Fields.caverns)
            .toRange(MIN_HEIGHT, 10, MAX_HEIGHT, 50, ConditionSettings.Fields.height)
            .history(NOISE_3D, CavernSettings.Fields.generators)
            .collapse(ConditionSettings.Fields.ceiling, NOISE_2D)
            .collapse(ConditionSettings.Fields.floor, NOISE_2D)
            .markRemoved(ENABLED, CG_1_0)
            .include(CAVERN_CEILING_COMPONENT)
            .include(CAVERN_FLOOR_COMPONENT)
            .include(CAVERN_GENERATORS_COMPONENT)
            .freeze();

    private static final ObjectResolver RAVINE_WALLS_COMPONENT =
        JsonTransformer.withPath(RavineSettings.Fields.walls)
            .toRange(MIN_VAL, 0.0, MAX_VAL, 4.0, NoiseSettings.Fields.range)
            .markRemoved(SCALE, CG_1_0)
            .freeze();

    public static final ObjectResolver RAVINE_TRANSFORMER =
        JsonTransformer.withPath(CaveSettings.Fields.ravines)
            .toRange(MIN_HEIGHT, 20, MAX_HEIGHT, 66, RavineSettings.Fields.originHeight)
            .history(ANGLE_XZ, RavineSettings.Fields.yaw)
            .history(ANGLE_Y, RavineSettings.Fields.pitch)
            .history(TWIST_XZ, RavineSettings.Fields.dYaw)
            .history(TWIST_Y, RavineSettings.Fields.dPitch)
            .history(SCALE_Y, TunnelSettings.Fields.stretch)
            .history(WALL_NOISE, RavineSettings.Fields.walls)
            .collapse(RavineSettings.Fields.walls, NOISE_2D)
            .include(RAVINE_WALLS_COMPONENT)
            .freeze();

    public static final ObjectResolver ROOM_TRANSFORMER =
        JsonTransformer.withPath(OverrideSettings.Fields.rooms)
            .history(SCALE_Y, RoomSettings.Fields.stretch)
            .freeze();

    public static final ObjectResolver TUNNEL_TRANSFORMER =
        JsonTransformer.withPath(CaveSettings.Fields.tunnels)
            .toRange(MIN_HEIGHT, 8, MAX_HEIGHT, 128, TunnelSettings.Fields.originHeight)
            .history(ISOLATED_CHANCE, TunnelSettings.Fields.chance)
            .history(FREQUENCY, TunnelSettings.Fields.count)
            .history(ANGLE_XZ, TunnelSettings.Fields.yaw)
            .history(ANGLE_Y, TunnelSettings.Fields.pitch)
            .history(TWIST_XZ, TunnelSettings.Fields.dYaw)
            .history(TWIST_Y, TunnelSettings.Fields.dPitch)
            .history(SCALE_Y, TunnelSettings.Fields.stretch)
            .freeze();

    public static final ObjectResolver CLUSTER_TRANSFORMER =
        JsonTransformer.withPath(CaveSettings.Fields.clusters)
            .history(NOISE_3D, ConditionSettings.Fields.noise)
            .freeze();

    private static final ObjectResolver LAYER_CEILING_COMPONENT =
        JsonTransformer.withPath(ConditionSettings.Fields.ceiling)
            .toRange(MIN_VAL, -7.0, MAX_VAL, 7.0, NoiseSettings.Fields.range)
            .freeze();

    public static final ObjectResolver LAYER_TRANSFORMER =
        JsonTransformer.withPath(CaveSettings.Fields.layers)
            .toRange(MIN_HEIGHT, 0, MAX_HEIGHT, 20, ConditionSettings.Fields.height)
            .history(NOISE_2D, ConditionSettings.Fields.ceiling)
            .include(LAYER_CEILING_COMPONENT)
            .freeze();

    public static final ObjectResolver STALACTITE_TRANSFORMER =
        JsonTransformer.withPath(CaveSettings.Fields.stalactites)
            .toRange(MIN_HEIGHT, 11, MAX_HEIGHT, 55, ConditionSettings.Fields.height)
            .toRange(MIN_LENGTH, 1, MAX_LENGTH, 3, StalactiteSettings.Fields.length)
            .history(NOISE_2D, ConditionSettings.Fields.region)
            .transform(WIDE, CaveTransformers::transformWide)
            .freeze();

    public static final ObjectResolver PILLAR_TRANSFORMER =
        JsonTransformer.withPath(CaveSettings.Fields.pillars)
            .history(FREQUENCY, PillarSettings.Fields.count)
            .toRange(MIN_HEIGHT, 10, MAX_HEIGHT, 50, ConditionSettings.Fields.height)
            .toRange(MIN_LENGTH, 4, MAX_LENGTH, 12, PillarSettings.Fields.length)
            .freeze();

    public static final ObjectResolver STRUCTURE_TRANSFORMER =
        JsonTransformer.withPath(CaveSettings.Fields.structures)
            .toRange(MIN_HEIGHT, 10, MAX_HEIGHT, 50, ConditionSettings.Fields.height)
            .history(FREQUENCY, StructureSettings.Fields.count)
            .history(AIR_MATCHERS, StructureSettings.Fields.airChecks)
            .history(SOLID_MATCHERS, StructureSettings.Fields.solidChecks)
            .history(NON_SOLID_MATCHERS, StructureSettings.Fields.nonSolidChecks)
            .history(WATER_MATCHERS, StructureSettings.Fields.waterChecks)
            .freeze();

    public static final ObjectResolver BURROW_TRANSFORMER =
        JsonTransformer.withPath(CaveSettings.Fields.burrows, BurrowSettings.Fields.map)
            .ifPresent(NoiseSettings.Fields.type, CaveTransformers::transformNoiseType)
            .transform(PERTURB, CaveTransformers::transformPerturb)
            .history(PERTURB_AMP, NoiseSettings.Fields.warpAmplitude)
            .history(PERTURB_FREQ, NoiseSettings.Fields.warpFrequency)
            .freeze();

    private static final ObjectResolver RECURSIVE_CEILING_COMPONENT =
        JsonTransformer.recursive(ConditionSettings.Fields.ceiling)
            .markRemoved(SCALE, CG_1_0)
            .markRemoved(NoiseSettings.Fields.threshold, CG_1_0)
            .ifPresent(NoiseSettings.Fields.type, CaveTransformers::transformNoiseType)
            .transform(PERTURB, CaveTransformers::transformPerturb)
            .history(PERTURB_AMP, NoiseSettings.Fields.warpAmplitude)
            .history(PERTURB_FREQ, NoiseSettings.Fields.warpFrequency)
            .freeze();

    private static final ObjectResolver RECURSIVE_FLOOR_COMPONENT =
        JsonTransformer.recursive(ConditionSettings.Fields.floor)
            .markRemoved(SCALE, CG_1_0)
            .markRemoved(NoiseSettings.Fields.threshold, CG_1_0)
            .ifPresent(NoiseSettings.Fields.type, CaveTransformers::transformNoiseType)
            .transform(PERTURB, CaveTransformers::transformPerturb)
            .history(PERTURB_AMP, NoiseSettings.Fields.warpAmplitude)
            .history(PERTURB_FREQ, NoiseSettings.Fields.warpFrequency)
            .freeze();

    private static final ObjectResolver RECURSIVE_NOISE_COMPONENT =
        JsonTransformer.recursive(ConditionSettings.Fields.noise)
            .transform(SCALE, CaveTransformers::transformScale)
            .ifPresent(NoiseSettings.Fields.type, CaveTransformers::transformNoiseType)
            .transform(PERTURB, CaveTransformers::transformPerturb)
            .history(PERTURB_AMP, NoiseSettings.Fields.warpAmplitude)
            .history(PERTURB_FREQ, NoiseSettings.Fields.warpFrequency)
            .freeze();

    private static final ObjectResolver RECURSIVE_REGION_COMPONENT =
        JsonTransformer.recursive(ConditionSettings.Fields.region)
            .transform(SCALE, CaveTransformers::transformScale)
            .markRemoved(MIN_VAL, CG_1_0)
            .markRemoved(MAX_VAL, CG_1_0)
            .markRemoved(NoiseSettings.Fields.range, CG_1_0)
            .ifPresent(NoiseSettings.Fields.type, CaveTransformers::transformNoiseType)
            .freeze();

    private static final ObjectResolver RECURSIVE_WALL_DECORATOR_COMPONENT =
        JsonTransformer.recursive(OverrideSettings.Fields.wallDecorators)
            .history(PREFERENCE, WallDecoratorSettings.Fields.placement)
            .renameValue(WallDecoratorSettings.Fields.placement, REPLACE_MATCH, WallDecoratorSettings.Placement.EMBED.name())
            .renameValue(WallDecoratorSettings.Fields.placement, REPLACE_ORIGINAL, WallDecoratorSettings.Placement.OVERLAY.name())
            .freeze();

    private static final ObjectResolver BLACKLIST_BIOMES_MOVER =
        JsonTransformer.containing(BLACKLIST_BIOMES)
            .relocate(BLACKLIST_BIOMES, join(ConditionSettings.Fields.biomes, BiomePredicate.Fields.blacklist))
            .freeze();

    private static final ObjectResolver BLACKLIST_DIMENSIONS_MOVER =
        JsonTransformer.containing(BLACKLIST_DIMENSIONS)
            .relocate(BLACKLIST_DIMENSIONS, join(ConditionSettings.Fields.dimensions, BiomePredicate.Fields.blacklist))
            .freeze();

    public static final ObjectResolver RECURSIVE_TRANSFORMER =
        JsonTransformer.root()
            .include(RECURSIVE_CEILING_COMPONENT)
            .include(RECURSIVE_FLOOR_COMPONENT)
            .include(RECURSIVE_NOISE_COMPONENT)
            .include(RECURSIVE_REGION_COMPONENT)
            .include(RECURSIVE_WALL_DECORATOR_COMPONENT)
            .include(BLACKLIST_BIOMES_MOVER)
            .include(BLACKLIST_DIMENSIONS_MOVER)
            .freeze();

    private static void replaceLegacyDefaults(final JsonObject root, final JsonValue importsValue) {
        final JsonArray imports = HjsonUtils.asOrToArray(importsValue);
        final JsonArray clone = new JsonArray();
        for (final JsonValue element : imports) {
            clone.add(substituteLegacyImport(element.asString()));
        }
        if (importsValue.isArray()) {
            root.set(IMPORTS, clone.setCondensed(imports.isCondensed()));
        } else {
            root.set(IMPORTS, clone.get(0));
        }
    }

    private static String substituteLegacyImport(final String val) {
        final Matcher matcher = DEFAULT_IMPORT.matcher(val);
        if (matcher.matches()) {
            final String name = matcher.group(1);
            if (IMPORT_FIELD_MAP.containsKey(name)) {
                return CaveLangExtension.DEFAULTS + "::" + IMPORT_FIELD_MAP.get(name) + " as " + name;
            }
        }
        return val;
    }

    private static void updateLegacyRoomChance(final JsonObject root, final JsonValue roomsValue) {
        final List<JsonObject> tunnels = HjsonUtils.getRegularObjects(root, CaveSettings.Fields.tunnels);
        final JsonObject rooms = roomsValue.asObject();
        boolean updated = false;

        for (final JsonObject tunnel : tunnels) {
            final JsonValue roomChance = tunnel.get(ROOM_CHANCE);
            if (roomChance != null) {
                final JsonObject tunnelRooms = HjsonUtils.getObject(tunnel, TunnelSettings.Fields.rooms)
                    .orElseGet(() -> new JsonObject().addAll(rooms))
                    .set(RoomSettings.Fields.chance, roomChance);
                tunnel.set(TunnelSettings.Fields.rooms, tunnelRooms);
                tunnel.remove(ROOM_CHANCE);
                updated = true;
            }
        }
        if (updated) {
            moveRoomsFromOverrides(root, rooms, tunnels);
        }
    }

    private static void moveRoomsFromOverrides(final JsonObject root, final JsonObject rooms, final List<JsonObject> tunnels) {
        // Copy these rooms to any tunnels which now need them.
        for (final JsonObject tunnel : tunnels) {
            if (!tunnel.has(TunnelSettings.Fields.rooms)) {
                tunnel.add(TunnelSettings.Fields.rooms, rooms);
            }
        }
        root.remove(OverrideSettings.Fields.rooms);
    }

    private static Pair<String, JsonValue> transformScale(final String name, final JsonValue scaleValue) {
        final double scale = scaleValue.asDouble();
        final double threshold = (2.0 * scale) - 1.0; // Convert to range down.
        return Pair.of(NoiseSettings.Fields.threshold, JsonValue.valueOf(threshold));
    }

    private static Pair<String, JsonValue> transformPerturb(final String name, final JsonValue perturbValue) {
        if (!perturbValue.isBoolean()) {
            return Pair.of(NoiseSettings.Fields.warp, perturbValue);
        }
        if (!perturbValue.asBoolean()) {
            return Pair.of(NoiseSettings.Fields.warp, JsonValue.valueOf(DomainWarpType.NONE.format()));
        }
        return Pair.of(NoiseSettings.Fields.warp, JsonValue.valueOf(DomainWarpType.BASIC_GRID.format()));
    }

    private static void transformNoiseType(final JsonObject cfg, final JsonValue value) {
        if (!value.isString()) {
            return;
        }
        final String type = value.asString().toLowerCase();
        if (!type.endsWith(FRACTAL)) {
            return;
        }
        int index = type.length() - FRACTAL.length();
        while (type.charAt(index) == '_') {
            index--;
        }
        cfg.set(NoiseSettings.Fields.type, type.substring(0, index));
        if (cfg.has(FRACTAL)) {
            cfg.set(FRACTAL, FractalType.FBM.format());
        }
    }

    private static void updateClusterRanges(final JsonObject root, final JsonValue clustersValue) {
        for (final JsonObject cluster : HjsonUtils.getRegularObjects(root, CaveSettings.Fields.clusters)) {
            final JsonValue radiusVariance = cluster.get(RADIUS_VARIANCE);
            if (radiusVariance != null) {
                updateClusterRadii(cluster, radiusVariance.asInt() / 2);
            }
            if (cluster.has(START_HEIGHT) || cluster.has(HEIGHT_VARIANCE)) {
                final int center = HjsonUtils.getInt(cluster, START_HEIGHT).orElse(32);
                final int variance = HjsonUtils.getInt(cluster, HEIGHT_VARIANCE).orElse(16);
                updateClusterCenter(cluster, center, variance / 2);
            }
        }
    }

    private static void updateClusterRadii(final JsonObject cluster, final int diff) {
        final String radiusXKey = ClusterSettings.Fields.radiusX;
        final String radiusYKey = ClusterSettings.Fields.radiusY;
        final String radiusZKey = ClusterSettings.Fields.radiusZ;
        final int radX = HjsonUtils.getInt(cluster, radiusXKey).orElse(16);
        final int radY = HjsonUtils.getInt(cluster, radiusYKey).orElse(12);
        final int radZ = HjsonUtils.getInt(cluster, radiusZKey).orElse(16);

        cluster.set(radiusXKey, new JsonArray().add(radX - diff).add(radX + diff).setCondensed(true))
            .set(radiusYKey, new JsonArray().add(radY - diff).add(radY + diff).setCondensed(true))
            .set(radiusZKey, new JsonArray().add(radZ - diff).add(radZ + diff).setCondensed(true));
        cluster.remove(RADIUS_VARIANCE);
    }

    private static void updateClusterCenter(final JsonObject cluster, final int center, final int diff) {
        final String centerKey = ClusterSettings.Fields.centerHeight;
        cluster.set(centerKey, new JsonArray().add(center - diff).add(center + diff).setCondensed(true));
        cluster.remove(START_HEIGHT);
        cluster.remove(HEIGHT_VARIANCE);
    }

    private static void condenseStalactites(final JsonObject json) {
        final List<JsonObject> largeStalactites = HjsonUtils.getRegularObjects(json, LARGE_STALACTITES);
        final List<JsonObject> largeStalagmites = HjsonUtils.getRegularObjects(json, LARGE_STALAGMITES);
        if (!largeStalactites.isEmpty() || !largeStalagmites.isEmpty()) {
            final JsonArray stalactites = HjsonUtils.getArrayOrNew(json, CaveSettings.Fields.stalactites);
            for (final JsonObject stalactite : largeStalactites) {
                stalactite.set(StalactiteSettings.Fields.type, StalactiteSettings.Type.STALACTITE.name());
                stalactites.add(stalactite);
            }
            for (final JsonObject stalagmite: largeStalagmites) {
                stalagmite.set(StalactiteSettings.Fields.type, StalactiteSettings.Type.STALAGMITE.name());
                stalactites.add(stalagmite);
            }
            json.set(CaveSettings.Fields.stalactites, stalactites);
            json.remove(LARGE_STALACTITES);
            json.remove(LARGE_STALAGMITES);
        }
    }

    private static Pair<String, JsonValue> transformWide(final String name, final JsonValue sizeValue) {
        final String size = sizeValue.isBoolean() && sizeValue.asBoolean()
            ? StalactiteSettings.Size.MEDIUM.name()
            : StalactiteSettings.Size.SMALL.name();
        return Pair.of(StalactiteSettings.Fields.size, JsonValue.valueOf(size));
    }

    private static void removeBlankSlate(final JsonObject json, final JsonValue blankSlateValue) {
        // User did *not* want a blank slate.
        if (!blankSlateValue.asBoolean()) {
            final JsonValue all = JsonValue.valueOf("ALL")
                .setEOLComment("Default ravines and lava settings.");
            json.set("$VANILLA", all);
        }
        json.remove(BLANK_SLATE);
    }

    private static String join(final String... path) {
        return String.join(".", path);
    }
}
