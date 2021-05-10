package com.personthecat.cavegenerator.config;

import com.google.common.collect.ImmutableMap;
import com.personthecat.cavegenerator.data.*;
import com.personthecat.cavegenerator.io.JarFiles;
import com.personthecat.cavegenerator.util.HjsonTools;
import com.personthecat.cavegenerator.util.Result;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.Pair;
import org.hjson.JsonArray;
import org.hjson.JsonObject;
import org.hjson.JsonValue;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.personthecat.cavegenerator.util.HjsonTools.getArrayOrNew;
import static com.personthecat.cavegenerator.util.HjsonTools.getIntOr;
import static com.personthecat.cavegenerator.util.HjsonTools.getRegularObjects;
import static com.personthecat.cavegenerator.util.HjsonTools.getObject;
import static com.personthecat.cavegenerator.util.HjsonTools.setOrAdd;
import static com.personthecat.cavegenerator.util.HjsonTools.writeJson;

/**
 * This is a temporary class designed to extend compatibility of deprecated fields and notations
 * until they can safely be phased out. It will handle updating these fields to their new format
 * for the next few updates until sufficient time has passed to remove them safely.
 */
@Log4j2
class PresetCompat {

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

    // Other field names needed for performing updates.
    private static final String IMPORTS = PresetExpander.IMPORTS;
    private static final String VARIABLES = PresetExpander.VARIABLES;

    // Deprecated fields in defaults.cave
    private static final String VANILLA = PresetExpander.VANILLA;
    private static final String REPLACE_DIRT_STONE = "REPLACE_DIRT_STONE";
    private static final String VANILLA_ROOM = "VANILLA_ROOM";
    private static final String LAVA_CAVE_BLOCK = "LAVA_CAVE_BLOCK";
    private static final String VANILLA_TUNNELS = "VANILLA_TUNNELS";
    private static final String VANILLA_RAVINES = "VANILLA_RAVINES";

    private static final Pattern DEFAULT_IMPORT = Pattern.compile(PresetExpander.DEFAULTS + "\\s*::\\s*(\\w+)");

    private static final Map<String, String> IMPORT_FIELD_MAP = ImmutableMap.<String, String>builder()
        .put(REPLACE_DIRT_STONE, DecoratorSettings.Fields.replaceableBlocks)
        .put(VANILLA_ROOM, OverrideSettings.Fields.rooms)
        .put(LAVA_CAVE_BLOCK, OverrideSettings.Fields.caveBlocks)
        .put(VANILLA_TUNNELS, CavePreset.Fields.tunnels)
        .put(VANILLA_RAVINES, CavePreset.Fields.ravines)
        .build();

    /**
     * This function takes care of any operation related to renaming and updating old variables,
     * as well as enforcing that imports be moved to the top of each file.
     *
     * @param json The parsed JSON object to be updated.
     * @param file The file source of this object.
     * @return Whether an exception took place when writing the file.
     */
    static Result<IOException> updatePreset(JsonObject json, File file) {
        final int hash = json.hashCode();
        updateRegularValues(json);
        enforceValueOrder(json);
        // Only write if changes were made.
        return ConfigFile.autoFormat || json.hashCode() != hash ? writeJson(json, file) : Result.ok();
    }

    static Result<IOException> updateImport(JsonObject json, File file) {
        final int hash = json.hashCode();
        // defaults.cave::VANILLA was changed to be easier to update.
        if (file.getName().equals(PresetExpander.DEFAULTS) && json.has(PresetExpander.VANILLA)) {
            updateDefaults(json);
        } else if (ConfigFile.updateImports) {
            updateRegularValues(json);
        }
        // Only write if changes were made.
        return ConfigFile.autoFormat || json.hashCode() != hash ? writeJson(json, file) : Result.ok();
    }

    private static void updateRegularValues(JsonObject json) {
        updateInner(json);
        updateRoot(json);
        updateImports(json);
        updateCaveBlocks(json);
        updateWallDecorators(json);
        updateRooms(json);
        updateTunnels(json);
        updateRavines(json);
        updateCaverns(json);
        updateClusters(json);
        updateLayers(json);
        updateStalactites(json);
        updatePillars(json);
        updateStructures(json);
        updateRecursive(json);
        removeBlankSlate(json);
    }

    private static void updateDefaults(JsonObject json) {
        // Swap old and new defaults, preserving extraneous data.
        json.remove(VANILLA)
            .remove(REPLACE_DIRT_STONE)
            .remove(VANILLA_ROOM)
            .remove(LAVA_CAVE_BLOCK)
            .remove(VANILLA_RAVINES)
            .remove(VANILLA_TUNNELS)
            .addAll(JarFiles.getDefaults());
    }

    private static void updateInner(JsonObject json) {
        for (JsonObject inner : HjsonTools.getRegularObjects(json, PresetReader.INNER_KEY)) {
            updateRegularValues(inner);
        }
    }

    private static void updateRoot(JsonObject json) {
        FieldHistory.withPath()
            .toRange(MIN_HEIGHT, 0, MAX_HEIGHT, 255, ConditionSettings.Fields.height)
            .history(USE_BIOME_BLACKLIST, OverrideSettings.Fields.blacklistBiomes)
            .history(USE_DIMENSION_BLACKLIST, OverrideSettings.Fields.blacklistDimensions)
            .history(STONE_CLUSTERS, CavePreset.Fields.clusters)
            .history(STONE_LAYERS, CavePreset.Fields.layers)
            .history(GIANT_PILLARS, CavePreset.Fields.pillars)
            .updateAll(json);
    }

    private static void updateImports(JsonObject json) {
        final JsonValue rawImports = json.get(IMPORTS);
        if (rawImports != null) {
            final JsonArray imports = HjsonTools.asOrToArray(rawImports);
            final JsonArray clone = new JsonArray();
            for (JsonValue value : imports) {
                clone.add(replaceImportVal(value.asString()));
            }
            if (rawImports.isArray()) {
                json.set(IMPORTS, clone.setCondensed(imports.isCondensed()));
            } else {
                json.set(IMPORTS, clone.get(0));
            }
        }
    }

    private static String replaceImportVal(String val) {
        final Matcher matcher = DEFAULT_IMPORT.matcher(val);
        if (matcher.matches()) {
            final String name = matcher.group(1);
            if (IMPORT_FIELD_MAP.containsKey(name)) {
                return PresetExpander.DEFAULTS + "::" + IMPORT_FIELD_MAP.get(name) + " as " + name;
            }
        }
        return val;
    }

    private static void updateCaveBlocks(JsonObject json) {
        FieldHistory.withPath(OverrideSettings.Fields.caveBlocks)
            .toRange(MIN_HEIGHT, 0, MAX_HEIGHT, 50, CaveBlockSettings.Fields.height)
            .history(NOISE_3D, CaveBlockSettings.Fields.noise)
            .history(CHANCE, CaveBlockSettings.Fields.integrity)
            .updateAll(json);
    }

    private static void updateWallDecorators(JsonObject json) {
        FieldHistory.withPath(OverrideSettings.Fields.wallDecorators)
            .toRange(MIN_HEIGHT, 10, MAX_HEIGHT, 50, WallDecoratorSettings.Fields.height)
            .history(NOISE_3D, WallDecoratorSettings.Fields.noise)
            .history(CHANCE, WallDecoratorSettings.Fields.integrity)
            .updateAll(json);
    }

    private static void updateRooms(JsonObject json) {
        FieldHistory.withPath(OverrideSettings.Fields.rooms)
            .history(SCALE_Y, RoomSettings.Fields.stretch)
            .updateAll(json);
        updateRoomChance(json);
    }

    private static void updateTunnels(JsonObject json) {
        FieldHistory.withPath(CavePreset.Fields.tunnels)
            .toRange(MIN_HEIGHT, 8, MAX_HEIGHT, 128, TunnelSettings.Fields.originHeight)
            .history(ISOLATED_CHANCE, TunnelSettings.Fields.chance)
            .history(FREQUENCY, TunnelSettings.Fields.count)
            .history(ANGLE_XZ, TunnelSettings.Fields.yaw)
            .history(ANGLE_Y, TunnelSettings.Fields.pitch)
            .history(TWIST_XZ, TunnelSettings.Fields.dYaw)
            .history(TWIST_Y, TunnelSettings.Fields.dPitch)
            .history(SCALE_Y, TunnelSettings.Fields.stretch)
            .updateAll(json);
    }

    private static void updateRavines(JsonObject json) {
        FieldHistory.withPath(CavePreset.Fields.ravines)
            .toRange(MIN_HEIGHT, 20, MAX_HEIGHT, 66, RavineSettings.Fields.originHeight)
            .history(ANGLE_XZ, RavineSettings.Fields.yaw)
            .history(ANGLE_Y, RavineSettings.Fields.pitch)
            .history(TWIST_XZ, RavineSettings.Fields.dYaw)
            .history(TWIST_Y, RavineSettings.Fields.dPitch)
            .history(SCALE_Y, TunnelSettings.Fields.stretch)
            .history(WALL_NOISE, RavineSettings.Fields.walls)
            .collapse(RavineSettings.Fields.walls, NOISE_2D)
            .updateAll(json);
        FieldHistory.withPath(CavePreset.Fields.ravines, RavineSettings.Fields.walls)
            .toRange(MIN_VAL, 0.0, MAX_VAL, 4.0, NoiseMapSettings.Fields.range)
            .markRemoved(SCALE, CG_1_0)
            .updateAll(json);
    }

    private static void updateCaverns(JsonObject json) {
        FieldHistory.withPath(CavePreset.Fields.caverns)
            .toRange(MIN_HEIGHT, 10, MAX_HEIGHT, 50, ConditionSettings.Fields.height)
            .history(NOISE_3D, CavernSettings.Fields.generators)
            .collapse(ConditionSettings.Fields.ceiling, NOISE_2D)
            .collapse(ConditionSettings.Fields.floor, NOISE_2D)
            .markRemoved(ENABLED, CG_1_0)
            .updateAll(json);
        FieldHistory.withPath(CavePreset.Fields.caverns, ConditionSettings.Fields.ceiling)
            .toRange(MIN_VAL, -17.0, MAX_VAL, -3.0, NoiseMapSettings.Fields.range)
            .updateAll(json);
        FieldHistory.withPath(CavePreset.Fields.caverns, ConditionSettings.Fields.floor)
            .toRange(MIN_VAL, 0.0, MAX_VAL, 8.0, NoiseMapSettings.Fields.range)
            .updateAll(json);
        FieldHistory.withPath(CavePreset.Fields.caverns, CavernSettings.Fields.generators)
            .transform(SCALE, PresetCompat::transformScale)
            .history(SCALE_Y, NoiseSettings.Fields.stretch)
            .updateAll(json);
    }

    private static void updateClusters(JsonObject json) {
        FieldHistory.withPath(CavePreset.Fields.clusters)
            .history(NOISE_3D, ConditionSettings.Fields.noise)
            .updateAll(json);
        updateClusterRanges(json);
    }

    private static void updateLayers(JsonObject json) {
        FieldHistory.withPath(CavePreset.Fields.layers)
            .toRange(MIN_HEIGHT, 0, MAX_HEIGHT, 20, ConditionSettings.Fields.height)
            .history(NOISE_2D, ConditionSettings.Fields.ceiling)
            .updateAll(json);
        FieldHistory.withPath(CavePreset.Fields.layers, ConditionSettings.Fields.ceiling)
            .toRange(MIN_VAL, -7.0, MAX_VAL, 7.0, NoiseMapSettings.Fields.range)
            .updateAll(json);
    }

    private static void updateStalactites(JsonObject json) {
        condenseStalactites(json);
        FieldHistory.withPath(CavePreset.Fields.stalactites)
            .toRange(MIN_HEIGHT, 11, MAX_HEIGHT, 55, ConditionSettings.Fields.height)
            .toRange(MIN_LENGTH, 1, MAX_LENGTH, 3, StalactiteSettings.Fields.length)
            .history(NOISE_2D, ConditionSettings.Fields.region)
            .transform(WIDE, PresetCompat::transformSize)
            .updateAll(json);
    }

    private static void updatePillars(JsonObject json) {
        FieldHistory.withPath(CavePreset.Fields.pillars)
            .history(FREQUENCY, PillarSettings.Fields.count)
            .toRange(MIN_HEIGHT, 10, MAX_HEIGHT, 50, ConditionSettings.Fields.height)
            .toRange(MIN_LENGTH, 4, MAX_LENGTH, 12, PillarSettings.Fields.length)
            .updateAll(json);
    }

    private static void updateStructures(JsonObject json) {
        FieldHistory.withPath(CavePreset.Fields.structures)
            .toRange(MIN_HEIGHT, 10, MAX_HEIGHT, 50, ConditionSettings.Fields.height)
            .history(FREQUENCY, StructureSettings.Fields.count)
            .history(AIR_MATCHERS, StructureSettings.Fields.airChecks)
            .history(SOLID_MATCHERS, StructureSettings.Fields.solidChecks)
            .history(NON_SOLID_MATCHERS, StructureSettings.Fields.nonSolidChecks)
            .history(WATER_MATCHERS, StructureSettings.Fields.waterChecks)
            .updateAll(json);
    }

    private static void updateRecursive(JsonObject json) {
        FieldHistory.recursive(ConditionSettings.Fields.ceiling)
            .markRemoved(SCALE, CG_1_0)
            .markRemoved(NoiseSettings.Fields.threshold, CG_1_0)
            .updateAll(json);
        FieldHistory.recursive(ConditionSettings.Fields.floor)
            .markRemoved(SCALE, CG_1_0)
            .markRemoved(NoiseSettings.Fields.threshold, CG_1_0)
            .updateAll(json);
        FieldHistory.recursive(ConditionSettings.Fields.noise)
            .transform(SCALE, PresetCompat::transformScale)
            .updateAll(json);
        FieldHistory.recursive(ConditionSettings.Fields.region)
            .transform(SCALE, PresetCompat::transformScale)
            .markRemoved(MIN_VAL, CG_1_0)
            .markRemoved(MAX_VAL, CG_1_0)
            .markRemoved(NoiseMapSettings.Fields.range, CG_1_0)
            .updateAll(json);
        FieldHistory.recursive(OverrideSettings.Fields.wallDecorators)
            .history(PREFERENCE, WallDecoratorSettings.Fields.placement)
            .renameValue(WallDecoratorSettings.Fields.placement, REPLACE_MATCH, WallDecoratorSettings.Placement.EMBED.name())
            .renameValue(WallDecoratorSettings.Fields.placement, REPLACE_ORIGINAL, WallDecoratorSettings.Placement.OVERLAY.name())
            .updateAll(json);
    }

    /**
     * For replacing <code>largeStalactites</code> and <code>largeStalagmites</code> with the
     * updated <code>stalactites</code> object syntax.
     *
     * @param json The root JSON object containing these fields.
     */
    private static void condenseStalactites(JsonObject json) {
        final List<JsonObject> largeStalactites = getRegularObjects(json, LARGE_STALACTITES);
        final List<JsonObject> largeStalagmites = getRegularObjects(json, LARGE_STALAGMITES);
        if (!largeStalactites.isEmpty() || !largeStalagmites.isEmpty()) {
            final JsonArray stalactites = getArrayOrNew(json, CavePreset.Fields.stalactites);
            for (JsonObject stalactite : largeStalactites) {
                stalactite.set(StalactiteSettings.Fields.type, StalactiteSettings.Type.STALACTITE.name());
                stalactites.add(stalactite);
            }
            for (JsonObject stalagmite: largeStalagmites) {
                stalagmite.set(StalactiteSettings.Fields.type, StalactiteSettings.Type.STALAGMITE.name());
                stalactites.add(stalagmite);
            }
            json.set(CavePreset.Fields.stalactites, stalactites);
            json.remove(LARGE_STALACTITES);
            json.remove(LARGE_STALAGMITES);
        }
    }

    private static void updateRoomChance(JsonObject json) {
        final List<JsonObject> tunnels = getRegularObjects(json, CavePreset.Fields.tunnels);
        final JsonObject rooms = getObject(json, OverrideSettings.Fields.rooms)
            .orElseGet(PresetCompat::getDefaultRooms);
        boolean updated = false;

        for (JsonObject tunnel : tunnels) {
            final JsonValue roomChance = tunnel.get(ROOM_CHANCE);
            if (roomChance != null) {
                final JsonObject tunnelRooms = getObject(tunnel, TunnelSettings.Fields.rooms)
                    .orElseGet(() -> new JsonObject().addAll(rooms))
                    .set(RoomSettings.Fields.chance, roomChance);
                tunnel.set(TunnelSettings.Fields.rooms, tunnelRooms);
                tunnel.remove(ROOM_CHANCE);
                updated = true;
            }
        }
        if (updated) {
            moveRoomsFromOverrides(json, rooms, tunnels);
        }
    }

    private static void moveRoomsFromOverrides(JsonObject json, JsonObject rooms, List<JsonObject> tunnels) {
        // Copy these rooms to any tunnels which now need them.
        for (JsonObject tunnel : tunnels) {
            if (!tunnel.has(TunnelSettings.Fields.rooms)) {
                tunnel.add(TunnelSettings.Fields.rooms, rooms);
            }
        }
        json.remove(OverrideSettings.Fields.rooms);
    }

    private static JsonObject getDefaultRooms() {
        final RoomSettings settings = RoomSettings.builder().build();
        return new JsonObject().add(RoomSettings.Fields.scale, settings.scale)
            .add(RoomSettings.Fields.stretch, settings.stretch);
    }

    private static void updateClusterRanges(JsonObject json) {
        for (JsonObject cluster : getRegularObjects(json, CavePreset.Fields.clusters)) {
            final JsonValue radiusVariance = cluster.get(RADIUS_VARIANCE);
            if (radiusVariance != null) {
                updateClusterRadii(cluster, radiusVariance.asInt() / 2);
            }
            if (cluster.has(START_HEIGHT) || cluster.has(HEIGHT_VARIANCE)) {
                final int center = getIntOr(cluster, START_HEIGHT, 32);
                final int variance = getIntOr(cluster, HEIGHT_VARIANCE, 16);
                updateClusterCenter(cluster, center, variance / 2);
            }
        }
    }

    private static void updateClusterRadii(JsonObject cluster, int diff) {
        final String radiusXKey = ClusterSettings.Fields.radiusX;
        final String radiusYKey = ClusterSettings.Fields.radiusY;
        final String radiusZKey = ClusterSettings.Fields.radiusZ;
        final int radX = getIntOr(cluster, radiusXKey, 16);
        final int radY = getIntOr(cluster, radiusYKey, 12);
        final int radZ = getIntOr(cluster, radiusZKey, 16);

        cluster.set(radiusXKey, new JsonArray().add(radX - diff).add(radX + diff).setCondensed(true))
            .set(radiusYKey, new JsonArray().add(radY - diff).add(radY + diff).setCondensed(true))
            .set(radiusZKey, new JsonArray().add(radZ - diff).add(radZ + diff).setCondensed(true));
        cluster.remove(RADIUS_VARIANCE);
    }

    private static void updateClusterCenter(JsonObject cluster, int center, int diff) {
        final String centerKey = ClusterSettings.Fields.centerHeight;
        cluster.set(centerKey, new JsonArray().add(center - diff).add(center + diff).setCondensed(true));
        cluster.remove(START_HEIGHT);
        cluster.remove(HEIGHT_VARIANCE);
    }

    private static Pair<String, JsonValue> transformScale(String name, JsonValue value) {
        final double scale = value.asDouble();
        final double threshold = (2.0 * scale) - 1.0; // Convert to range down.
        return Pair.of(NoiseSettings.Fields.threshold, JsonValue.valueOf(threshold));
    }

    private static Pair<String, JsonValue> transformSize(String name, JsonValue value) {
        final String size = value.isBoolean() && value.asBoolean()
            ? StalactiteSettings.Size.MEDIUM.name()
            : StalactiteSettings.Size.SMALL.name();
        return Pair.of(StalactiteSettings.Fields.size, JsonValue.valueOf(size));
    }

    /**
     * Replaces any instance of <code>blankSlate: false</code> with the following
     * values: <code>
     *   $VANILLA: ALL
     * </code>
     * <p>
     *  If <code>blankSlate</code> is set to <code>true</code>, it is simply removed,
     *  as this is the default behavior.
     * </p>
     *
     * @param json The JSON object to be updated.
     */
    private static void removeBlankSlate(JsonObject json) {
        final JsonValue blankSlate = json.get(BLANK_SLATE);
        if (blankSlate != null) {
            // User did *not* want a blank slate.
            if (!blankSlate.asBoolean()) {
                final JsonValue all = JsonValue.valueOf("ALL")
                    .setEOLComment("Default ravines and lava settings.");
                setOrAdd(json, "$VANILLA", all);
            }
            json.remove(BLANK_SLATE);
        }
    }

    /**
     * Ensures that any imports array be placed at the top of a file, and any field
     * being merged at the root level go just below it.
     *
     * @param json The JSON object to be updated.
     */
    private static void enforceValueOrder(JsonObject json) {
        final JsonObject top = new JsonObject();
        final JsonObject bottom = new JsonObject();

        moveValue(IMPORTS, json, top);
        moveValue(VARIABLES, json, top);
        for (JsonObject.Member member : json) {
            if (member.getName().startsWith("$")) {
                top.add(member.getName(), member.getValue());
            } else {
                bottom.add(member.getName(), member.getValue());
            }
        }
        json.clear();
        json.addAll(top);
        json.addAll(bottom);
    }

    /**
     * Moves a JSON member from one object to another.
     *
     * @param field The field key being moved.
     * @param from The JSON source being copied from.
     * @param to The JSON target being copied into.
     */
    private static void moveValue(String field, JsonObject from, JsonObject to) {
        if (from.has(field)) {
            to.add(field, from.get(field));
            from.remove(field);
        }
    }
}
