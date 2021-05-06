package com.personthecat.cavegenerator.config;

import com.personthecat.cavegenerator.data.*;
import com.personthecat.cavegenerator.model.Direction;
import com.personthecat.cavegenerator.model.FloatRange;
import com.personthecat.cavegenerator.model.Range;
import com.personthecat.cavegenerator.model.ScalableFloat;
import lombok.extern.log4j.Log4j2;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import org.apache.logging.log4j.Level;
import org.hjson.JsonObject;

import java.util.List;

/**
 *   This class is intended for detecting a series of common errors in the preset creation
 * process. The kinds of errors reported to the user should ideally not be critical issues,
 * such as missing required fields or syntax errors, which should be instead reported when
 * objects are deserialized.
 *   In its current form, errors are reported via the standard log4j logger associated with
 * this class. This class' primary method, {@link PresetTester#run}, includes the option for
 * some more serious errors to trigger a crash, requiring corrections from the preset author
 * or user.
 *   In some cases, it may be unclear as to whether a test should be included here or deferred
 * to the source. The current solution is to prefer PresetTester as the primary hub for
 * validating presets, as doing so should improve readability elsewhere.
 */
@Log4j2
public class PresetTester {

    private final CavePreset preset;
    private final Level low, high;
    private final String name;

    /** Primary constructor */
    public PresetTester(CavePreset preset, String name, boolean allowCrash) {
        this.low = allowCrash ? Level.WARN : Level.INFO;
        this.high = allowCrash ? Level.FATAL: Level.WARN;
        this.preset = preset;
        this.name = name;
    }

    /**
     * This class' primary method, which used for testing any GeneratorSettings
     * object in hopes of identifying valuable means of improvement for the user.
     */
    public void run() {
        log.info(" ### Begin testing {} ###", name);
        log.info(" --- Json diagnostics ---");
        this.debugExistingFields(preset.raw);
        this.debugUnusedFields(preset.raw);
        log.info(" --- Logic and syntax diagnostics ---");
        this.testTunnels(preset.tunnels);
        this.testRavines(preset.ravines);
        this.testCaverns(preset.caverns);
        this.testStructures(preset.structures);
        this.testClusters(preset.clusters);
        this.testLayers(preset.layers);
        this.testStalactites(preset.stalactites);
        this.testPillars(preset.pillars);
        log.info(" ### {} testing complete ###", name);
    }

    /** Inform the user of which fields were successfully parsed. */
    private void debugExistingFields(JsonObject json) {
        final List<String> used = json.getUsedPaths();
        if (used.isEmpty()) {
            log.info("No fields were detected in {}. If you were expecting fields, "
                + "you may have commented it out.", name);
            return;
        }
        log.info("The following fields were found in {}. If you do not see one, "
            + "you may have accidentally commented it out.", name);
        for (String s : used) {
            log.info(" + {} ", s);
        }
    }

    /** Inform the user of which fields were never accessed by PresetReader. */
    private void debugUnusedFields(JsonObject json) {
        final List<String> unused = json.getUnusedPaths();
        if (unused.isEmpty()) {
            log.info("No unused fields were detected inside of {}", name);
            return;
        }
        log.info("The following fields were never used by {}. Any field listed " +
            "below has no effect whatsoever.", name);
        for (String s : unused) {
            log.log(low, " - {}", s);
        }
    }

    private void testTunnels(List<TunnelSettings> s) {
        final String path = CavePreset.Fields.tunnels;
        for (TunnelSettings cfg : s) {
            this.testTunnel(cfg, path);
            this.testConditions(cfg.conditions, path);
            this.testDecorators(cfg.decorators, path);
        }
    }

    private void testTunnel(TunnelSettings s, String path) {
        this.testDistance(s.distance, join(path, TunnelSettings.Fields.distance));
        this.assertPositive(s.systemDensity, join(path, TunnelSettings.Fields.systemDensity));
        this.testAngle(s.yaw, join(path, TunnelSettings.Fields.yaw));
        this.testAngle(s.pitch, join(path, TunnelSettings.Fields.pitch));
        this.testScale(s.scale, join(path, TunnelSettings.Fields.scale));
        s.branches.ifPresent(b -> this.testTunnel(b, join(path, TunnelSettings.Fields.branches)));
    }

    private void testRavines(List<RavineSettings> s) {
        final String path = CavePreset.Fields.tunnels;
        for (RavineSettings cfg : s) {
            this.testRavine(cfg, path);
            this.testConditions(cfg.conditions, path);
            this.testDecorators(cfg.decorators, path);
        }
    }

    private void testRavine(RavineSettings s, String path) {
        this.testDistance(s.distance, join(path, RavineSettings.Fields.distance));
        this.testAngle(s.yaw, join(path, RavineSettings.Fields.yaw));
        this.testAngle(s.pitch, join(path, RavineSettings.Fields.pitch));
        this.testScale(s.scale, join(path, RavineSettings.Fields.scale));
        this.testNoise(s.walls, join(path, RavineSettings.Fields.walls));
    }

    private void testCaverns(List<CavernSettings> s) {
        final String path = CavePreset.Fields.caverns;
        for (CavernSettings cfg : s) {
            this.testCavern(cfg, path);
            this.testConditions(cfg.conditions, path);
            this.testDecorators(cfg.decorators, path);
        }
    }

    private void testCavern(CavernSettings s, String path) {
        final String fullPath = join(path, CavernSettings.Fields.generators);
        for (int i = 0; i < s.generators.size(); i++) {
            this.testNoise(s.generators.get(i), fullPath + "[" + i + "]");
        }
        if (s.wallInterpolation) {
            log.log(low, "{} is experimental. There may be bugs.", join(path, CavernSettings.Fields.wallInterpolation));
        }
        if (s.wallCurveRatio > 2.0) {
            log.log(low, "{} is a little high. You may see borders.", join(path, CavernSettings.Fields.wallCurveRatio));
        }
        s.walls.ifPresent(n -> this.testNoise(n, join(path, CavernSettings.Fields.walls)));
        s.offset.ifPresent(n -> this.testNoise(n, join(path, CavernSettings.Fields.offset)));
        s.wallOffset.ifPresent(n -> this.testNoise(n, join(path, CavernSettings.Fields.wallOffset)));
    }

    private void testStructures(List<StructureSettings> s) {
        final String path = CavePreset.Fields.structures;
        for (StructureSettings cfg : s) {
            this.testStructure(cfg, path);
            this.testConditions(cfg.conditions, path);
        }
    }

    private void testStructure(StructureSettings s, String path) {
        final String fullPath = path + "[name=" + s.name + "]";
        if (s.placement.getIntegrity() < 0 || s.placement.getIntegrity() > 1) {
            log.log(high, "Invalid integrity @ {}. Use a number between 0 and 1.", fullPath);
        }
        if (s.count > 100) {
            log.log(low, "Unusually high count @ {}. Consider a dedicated mod.", fullPath);
        }
        int checks = s.airChecks.size() + s.waterChecks.size() + s.nonSolidChecks.size() + s.solidChecks.size();
        if (checks > 25) {
            log.log(low, "Unusually high number of checks @ {}. Try to optimize.", fullPath);
        }
        testChance(s.chance, fullPath);
    }

    private void testClusters(List<ClusterSettings> clusters) {
        final String path = CavePreset.Fields.clusters;
        for (ClusterSettings cfg : clusters) {
            final String fullPath = path + "[states=" + cfg.states.get(0) + "...]";
            this.testCluster(cfg, fullPath);
            this.testConditions(cfg.conditions, fullPath);
        }
    }

    private void testCluster(ClusterSettings s, String path) {
        final Range height = s.conditions.height;
        if (!height.contains(s.centerHeight.min) || height.contains(s.centerHeight.max)) {
            final String center = ClusterSettings.Fields.centerHeight;
            log.log(low, "Invalid heights @ {}. {} does not contain {}", path, height, center);
        }
        this.testChance(s.chance, path);
    }

    private void testLayers(List<LayerSettings> s) {
        final String path = CavePreset.Fields.layers;
        int previousHeight = -1;

        for (LayerSettings l : s) {
            final String fullPath = path + "[state=" + l.state + "]";
            final int maxRange = l.conditions.ceiling.map(c -> c.range.max).orElse(0);
            final int max = l.conditions.height.max + maxRange;
            // Check height order.
            if (max <= previousHeight) { // Need to think about the level used here.
                log.log(low, "Unclear Layer height settings. The array is not sorted in ascending order.");
            }
            previousHeight = max;
            this.testConditions(l.conditions, fullPath);
        }
    }


    private void testStalactites(List<StalactiteSettings> s) {
        final String path = CavePreset.Fields.stalactites;
        for (StalactiteSettings cfg : s) {
            final String fullPath = path + "[state" + cfg.state + "]";
            this.testStalactite(cfg, fullPath);
            this.testConditions(cfg.conditions, fullPath);
        }
    }

    private void testStalactite(StalactiteSettings s, String path) {
        this.testChance(s.chance, path);
    }

    private void testPillars(List<PillarSettings> s) {
        final String path = CavePreset.Fields.pillars;
        for (PillarSettings cfg : s) {
            final String fullPath = path + "[state=" + cfg.state + "]";
            this.testConditions(cfg.conditions, fullPath);
        }
    }

    /** In general, it doesn't sense for distance to be < 0. */
    private void testDistance(int distance, String path) {
        if (distance < 0) {
            log.log(low, "Negative value @ {}. This will have no effect.", path);
        }
    }

    private void testConditions(ConditionSettings s, String path) {
        this.testDimensionList(s.dimensions, s.blacklistDimensions, path);
        this.testBiomeList(s.biomes, s.blacklistBiomes, path);
        s.noise.ifPresent(n -> this.testNoise(n, path));
        s.ceiling.ifPresent(n -> this.testNoise(n, path));
        s.floor.ifPresent(n -> this.testNoise(n, path));
        s.region.ifPresent(n -> this.testNoise(n, path));
    }

    private void testDecorators(DecoratorSettings s, String path) {
        this.testCaveBlocks(s.caveBlocks, path);
        this.testWallDecorators(s.wallDecorators, path);
        this.testShell(s.shell, path);
    }

    private void testCaveBlocks(List<CaveBlockSettings> caveBlocks, String path) {
        int lastMinHeight = -1, lastMaxHeight = -1;
        boolean foundGuaranteed = false;

        for (CaveBlockSettings c : caveBlocks) {
            final String fullPath = path + ".caveBlocks[states=" + c.states + "...]";
            // Check for unreachable objects.
            if (foundGuaranteed) {
                log.log(high, "Additional CaveBlock objects found despite an earlier object where `chance=1`. " +
                    "{} will have no effect.", fullPath);
            }
            final int minY = c.height.min;
            final int maxY = c.height.max;
            final boolean hasNoise = c.noise.isPresent();
            if (c.integrity == 1.0 && !hasNoise && lastMinHeight == minY && lastMaxHeight == maxY) {
                foundGuaranteed = true;
            }
            lastMinHeight = minY;
            lastMaxHeight = maxY;
            // Normal tests.
            this.testChance(c.integrity, fullPath);
            c.noise.ifPresent(n -> this.testNoise(n, fullPath));
        }
    }

    private void testWallDecorators(List<WallDecoratorSettings> wallDecorators, String path) {
        int lastMinHeight = -1, lastMaxHeight = -1;
        boolean foundGuaranteed = false;

        for (WallDecoratorSettings d : wallDecorators) {
            final String fullPath = path + ".wallDecorators[states=" + d.states.get(0) + "...]";
            // Check for unreachable objects.
            if (foundGuaranteed) {
                log.log(high, "Additional WallDecorator objects found despite an earlier object where `chance=1`. " +
                    "{} will have no effect.", fullPath);
            }
            final int minY = d.height.min;
            final int maxY = d.height.max;
            final boolean hasNoise = d.noise.isPresent();
            if (d.integrity == 1.0 && !hasNoise && lastMinHeight == minY && lastMaxHeight == maxY) {
                log.log(low, "{} uses full coverage. It may be better to use Shells or Clusters.", fullPath);
                foundGuaranteed = true;
            }
            lastMinHeight = minY;
            lastMaxHeight = maxY;
            // Normal tests.
            this.testChance(d.integrity, fullPath);
            this.testDirections(d.directions, fullPath);
            d.noise.ifPresent(s -> testNoise(s, fullPath));
        }
    }

    private void testShell(ShellSettings shell, String path) {
        final String decoratorPath = join(path, DecoratorSettings.Fields.shell, ShellSettings.Fields.decorators);
        for (int i = 0; i < shell.decorators.size(); i++) {
            final ShellSettings.Decorator d = shell.decorators.get(i);
            final String fullPath = decoratorPath + "[" + i + "]";
            this.testChance(d.integrity, fullPath);
            d.noise.ifPresent(n -> this.testNoise(n, fullPath));
        }
    }

    private void testDimensionList(List<Integer> dims, boolean blacklist, String path) {
        if (dims.size() > 8) {
            log.log(low, "High number of dimensions in the dimension list @ {}. Consider " +
                "{} `blacklistDimensions`.", path, (blacklist ? "disabling" : "enabling"));
            log.log(low, "If you want this feature to spawn anywhere, you can leave the list empty.");
        }
    }

    private void testBiomeList(List<Biome> biomes, boolean blacklist, String path) {
        if (biomes.size() > ForgeRegistries.BIOMES.getValuesCollection().size() / 2) {
            log.log(low, "High number of dimensions in the dimension list @ {}. Consider " +
                "{} `blacklistBiomes`.", path, (blacklist ? "disabling" : "enabling"));
            log.log(low, "If you want this feature to spawn anywhere, you can leave the list empty.");
        }
    }

    /** Ensures that val > ref */
    private void assertPositive(int val, String path) {
        if (val <= 0) {
            log.log(high, "Invalid number # {}. It must be > 0", path);
        }
    }

    /** Ensure that the value of this angle is in radians. */
    private void testAngle(ScalableFloat angle, String path) {
        if (angle.startVal > 6.0 || angle.startVal < 0.0) {
            log.log(low, "Invalid angle @ {}. This value should be in radians (0 - 6).", path);
        }
    }

    private void testScale(ScalableFloat scale, String path) {
        if (scale.factor > 2.0 || scale.factor < -2.0) {
            log.log(low, "Potentially dangerous factor @ {}. This object will be very large.", path);
        }
        if (scale.exponent > 1.5) {
            log.log(low, "potentially dangerous exponent @ {}. This object will be extremely large.", path);
        }
    }

    /** Verify the ranges of `frequency` and `scale`. */
    private void testNoise(NoiseSettings noise, String path) {
        testNoiseFrequency(noise.frequency, path);
        testNoiseThreshold(noise.threshold, path);
        testNoiseOctaves(noise.octaves, path);
    }

    /** 2D noise includes an addition `minVal` and `maxVal`. */
    private void testNoise(NoiseRegionSettings noise, String path) {
        testNoiseFrequency(noise.frequency, path);
        testNoiseThreshold(noise.threshold, path);
    }

    private void testNoise(NoiseMapSettings noise, String path) {
        testNoiseFrequency(noise.frequency, path);
        testNoiseOctaves(noise.octaves, path);
    }

    /** Chance should always be between 0 and 1. */
    private void testChance(double chance, String path) {
        if (chance < 0 || chance > 1) {
            log.log(low, "Poor chance @ {}. Use a value between 0 and 1.", path);
        }
    }

    /** Noise frequency is the same. Include a more helpful message. */
    private void testNoiseFrequency(float frequency, String path) {
        if (frequency < 0 || frequency > 1) {
            log.log(low, "Poor frequency value @ {}. If you are converting presets from a previous version, " +
                "`spacing` can be converted using the formula `frequency = 1 / spacing`.", path);
        }
    }

    /** Thresholds should always be between -1 and 1. */
    private void testNoiseThreshold(FloatRange threshold, String path) {
        if (threshold.min < -1.0 || threshold.max > 1.0) {
            log.log(low, "Poor threshold @ {}. Use a range between -1 and 1.", path);
        }
    }

    /** Octaves <= 0 is is impossible. Octaves > 5 is unnecessary. */
    private void testNoiseOctaves(int octaves, String path) {
        if (octaves < 0) {
            log.log(low, "Octaves are < 0 @ {}. Octaves is a count and should be > 0.", path);
        } else if (octaves == 0) {
            log.log(low, "Octaves are == 0 @ {}. Set dummy: true if you want this generator to have no effect.", path);
        } else if (octaves > 5) {
            log.log(low, "Unusually high octave count @ {}. This is expensive and may have no effect.", path);
        }
    }

    /** Direction.ALL covers all directions. Others are unnecessary. */
    private void testDirections(List<Direction> directions, String path) {
        final boolean containsSide = directions.contains(Direction.SIDE);
        for (Direction d : directions) {
            if ((d.equals(Direction.ALL) && directions.size() > 1) || (containsSide && isSide(d))) {
                log.log(low, "Direction array contains unnecessary values @ {}.", path);
                return;
            }
        }
    }

    private static boolean isSide(Direction d) {
        return d == Direction.NORTH || d == Direction.SOUTH || d == Direction.EAST || d == Direction.WEST;
    }

    private static String join(String... path) {
        return String.join(".", path);
    }
}