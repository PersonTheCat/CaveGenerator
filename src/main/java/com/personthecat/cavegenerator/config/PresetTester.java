package com.personthecat.cavegenerator.config;

import com.personthecat.cavegenerator.util.Direction;
import com.personthecat.cavegenerator.util.NoiseSettings2D;
import com.personthecat.cavegenerator.util.NoiseSettings3D;
import com.personthecat.cavegenerator.util.ScalableFloat;
import com.personthecat.cavegenerator.world.*;
import com.personthecat.cavegenerator.world.GeneratorSettings.*;
import com.personthecat.cavegenerator.world.feature.GiantPillar;
import com.personthecat.cavegenerator.world.feature.LargeStalactite;
import net.minecraft.block.BlockStone;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.Level;
import org.hjson.JsonObject;

import java.util.List;

import static com.personthecat.cavegenerator.util.CommonMethods.*;

/**
 *   This class is intended for detecting a series of common
 * errors in the preset creation process. The kinds of errors
 * reported to the user should ideally not be critical issues,
 * such as missing required fields or syntax errors, which
 * should be instead reported when objects are deserialized.
 *   In its current form, errors are reported via the standard
 * log4j logger associated with this mod. This class' primary
 * method, {@link PresetTester#run}, includes the option for
 * some more serious errors to trigger a crash, requiring
 * corrections from the preset author or user.
 *   In some cases, it may be unclear as to whether a test
 * should be included here or deferred to the source. The
 * current solution is to prefer PresetTester as the
 * primary hub for validating presets, as doing so should
 * improve readability elsewhere.
 */
public class PresetTester {

    /**
     * A list of blocks that are commonly used as matchers or marked as
     * replaceable when this action would have no effect, theoretically
     * reducing performance.
     */
    private static final IBlockState[] COMMON_PROBLEM_STATES = {
        getVariant(Blocks.STONE, BlockStone.VARIANT, BlockStone.EnumType.ANDESITE),
        getVariant(Blocks.STONE, BlockStone.VARIANT, BlockStone.EnumType.DIORITE),
        getVariant(Blocks.STONE, BlockStone.VARIANT, BlockStone.EnumType.GRANITE)
    };

    private final GeneratorSettings preset;
    private final IBlockState[] decorators;
    private final Level low, high;
    private final String name;

    /** Primary constructor */
    public PresetTester(GeneratorSettings preset, String name, boolean allowCrash) {
        this.decorators = preset.decorators.getDecoratorBlocks();
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
        info(" ### Begin testing {} ###", name);
        info(" --- Json diagnostics ---");
        debugExistingFields(preset.preset);
        debugUnusedFields(preset.preset);
        info(" --- Logic and syntax diagnostics ---");
        testSpawns(preset.conditions);
        testTunnels(preset.tunnels);
        testRavines(preset.ravines);
        testCaverns(preset.caverns);
        testStructures(preset.structures);
        testClusters(preset.decorators.stoneClusters);
        testLayers(preset.decorators.stoneLayers);
        testCaveBlocks(preset.decorators.caveBlocks);
        testWallDecorators(preset.decorators.wallDecorators);
        testStalactites(preset.decorators.stalactites);
        testPillars(preset.decorators.pillars);
        testEarlyMatchers(preset.replaceable, "replaceableBlocks");
        info(" ### {} testing complete ###", name);
    }

    /** Inform the user of which fields were successfully parsed. */
    private void debugExistingFields(JsonObject json) {
        info("The following fields were found in {}. If you do not see one, " +
            "you may have accidentally commented it out.", name);
        for (JsonObject.Member m : json) {
            info(" * {}", m.getName());
        }
    }

    /** Inform the user of which fields were never accessed by PresetReader. */
    private void debugUnusedFields(JsonObject json) {
        List<String> unused = json.getUnusedPaths();
        if (unused.size() == 0) {
            info("No unused fields were detected inside of {}", name);
            return;
        }
        info("The following fields were never used by {}. Any field listed " +
            "below has no effect whatsoever.", name);
        for (String s : unused) {
            log(low, " * {}", s);
        }
    }

    private void testSpawns(SpawnSettings s) {
        if (s.dimensions.length > 8) {
            log(low, "High number of dimensions in the dimension list. Consider " +
                "{} `useDimensionBlacklist.`", (s.dimensionBlacklist ? "enabling" : "disabling"));
        }
        testHeights(s.minHeight, s.maxHeight, "root");
    }

    private void testTunnels(TunnelSettings[] s) {
        for (TunnelSettings cfg : s) {
            testTunnel(cfg);
        }
    }

    private void testTunnel(TunnelSettings s) {
        testDistance(s.startDistance, "tunnels.distance");
        testHeights(s.minHeight, s.maxHeight, "tunnels");
        testAngle(s.angleXZ, "tunnels.angleXZ");
        testAngle(s.angleY, "tunnels.angleY");
    }

    private void testRavines(RavineSettings[] s) {
        for (RavineSettings cfg : s) {
            testRavine(cfg);
        }
    }

    private void testRavine(RavineSettings s) {
        testDistance(s.startDistance, "ravines.distance");
        testHeights(s.minHeight, s.maxHeight, "ravines");
        testAngle(s.angleXZ, "ravines.angleXZ");
        testAngle(s.angleY, "ravines.angleY");
        testNoise(s.wallNoise, "ravines.wallNoise");
    }

    private void testCaverns(CavernSettings s) {
        testHeights(s.minHeight, s.maxHeight, "caverns");
        for (int i = 0; i < s.noise.length; i++) {
            testNoise(s.noise[i], "caverns.noise3D[" + i + "]");
        }
        testNoise(s.ceilNoise, "caverns.ceiling");
        testNoise(s.floorNoise, "caverns.floor");
    }

    private void testStructures(StructureSettings[] structures) {
        for (StructureSettings s : structures) {
            final String path = "structures[name=" + s.name + "]";
            if (s.settings.getIntegrity() < 0 || s.settings.getIntegrity() > 1) {
                log(high, "Invalid integrity @ {}. Use a number between 0 and 1.", path);
            }
            if (s.frequency > 100) {
                log(low, "Unusually high frequency @ {}. You may benefit from a different mod.", path);
            }
            testChance(s.chance, path);
        }
    }

    private void testClusters(StoneCluster[] clusters) {
        for (StoneCluster c : clusters) {
            final String path = "stoneClusters[state=" + c.getState() + "]";
            testChance(c.getChance(), path);
        }
    }

    private void testLayers(StoneLayer[] layers) {
        int previousHeight = -1;

        for (StoneLayer l : layers) {
            // Check height order.
            if (l.getMaxHeight() <= previousHeight) { // Need to think about the level used here.
                log(low, "Unclear StoneLayer height settings. The array is not sorted in ascending order.");
            }
            previousHeight = l.getMaxHeight();
            // Normal tests.
            testNoise(l.getSettings(), "stoneLayers[state=" + l.getState() + "]");
        }
    }


    private void testCaveBlocks(CaveBlock[] caveBlocks) {
        int lastMinHeight = -1, lastMaxHeight = -1;
        boolean foundGuaranteed = false;

        for (CaveBlock c : caveBlocks) {
            final String path = "caveBlocks[state=" + c.getFillBlock() + "]";
            // Check for unreachable objects.
            if (foundGuaranteed) {
                log(high, "Additional CaveBlock objects found despite an earlier object where `chance=1`. " +
                    "{} will have no effect.", path);
            }
            if (c.getChance() == 1 &&
                lastMinHeight == c.getMinHeight() &&
                lastMaxHeight == c.getMaxHeight()
            ) {
                foundGuaranteed = true;
            }
            lastMinHeight = c.getMinHeight();
            lastMaxHeight = c.getMaxHeight();
            // Normal tests.
            testChance(c.getChance(), path);
            testHeights(c.getMinHeight(), c.getMaxHeight(), path);
        }
    }

    private void testWallDecorators(WallDecorator[] wallDecorators) {
        int lastMinHeight = -1, lastMaxHeight = -1;
        boolean foundGuaranteed = false;

        for (WallDecorator d : wallDecorators) {
            final String path = "wallDecorators[state=" + d.getFillBlock() + "]";
            // Check for unreachable objects.
            if (foundGuaranteed) {
                log(high, "Additional WallDecorator objects found despite an earlier object where `chance=1`. " +
                    "{} will have no effect.", path);
            }
            if (d.getChance() == 1 &&
                !d.spawnInPatches() &&
                lastMinHeight == d.getMinHeight() &&
                lastMaxHeight == d.getMaxHeight()
            ) {
                log(low, "{} uses full coverage. It may be better to use StoneClusters.", path);
                foundGuaranteed = true;
            }
            lastMinHeight = d.getMinHeight();
            lastMaxHeight = d.getMaxHeight();
            // Normal tests.
            d.getSettings().ifPresent(s -> testNoise(s, path));
            testChance(d.getChance(), path);
            testHeights(d.getMinHeight(), d.getMaxHeight(), path);
            testDirections(d.getDirections(), path);
        }
    }

    private void testStalactites(LargeStalactite[] stalactites) {
        for (LargeStalactite s : stalactites) {
            final String path = s.getType() == LargeStalactite.Type.STALACTITE ?
                "largeStalactites[state=" + s.getState() + "]":
                "largeStalagmites[state=" + s.getState() + "]";
            s.getSettings().ifPresent(n -> testNoise(n, path));
            testHeights(s.getMinHeight(), s.getMaxHeight(), path);
            testChance(s.getChance(), path);
        }
    }

    private void testPillars(GiantPillar[] pillars) {
        for (GiantPillar p : pillars) {
            final String path = "giantPillars[state=" + p.getPillarBlock() + "]";
            testHeights(p.getMinHeight(), p.getMaxHeight(), path);
            testLength(p.getMinHeight(), p.getMaxHeight(), path);
        }
    }

    /** Test for a series of common blocks that do not usually exist at this point in time. */
    private void testEarlyMatchers(IBlockState[] states, String path) {
        for (IBlockState state : states) {
            // If it is a problem block and it isn't specifically placed by the current generator.
            if (ArrayUtils.contains(COMMON_PROBLEM_STATES, state) && !ArrayUtils.contains(decorators, state)) {
                log(low, "Unnecessary block @ {}. Unless it is placed by a different preset or mod, " +
                "{} does not exist when this array is used.", path, state);
            }
        }
    }

    /** In general, it doesn't sense for distance to be < 0. */
    private void testDistance(int distance, String path) {
        if (distance < 0) {
            log(low, "Negative value @ {}. This may have no effect.", path);
        }
    }

    /** Ensure that min < max. */
    private void testHeights(int min, int max, String path) {
        if (min > max) {
            log(high, "Invalid range @ {}. minHeight > maxHeight", path);
        }
    }

    /** Variant of testHeights() with the updated error message. */
    private void testLength(int min, int max, String path) {
        if (min > max) {
            log(high, "Invalid range @ {}. minLength > maxLength.", path);
        }
    }

    /** Ensure that the value of this angle is in radians. */
    private void testAngle(ScalableFloat angle, String path) {
        if (angle.startVal > 6 || angle.startVal < 0) {
            log(low, "Invalid angle @ {}. This value should be in radians (0 - 6).", path);
        }
    }

    /** Verify the ranges of `frequency` and `scale`. */
    private void testNoise(NoiseSettings3D noise, String path) {
        testNoiseFrequency(noise.frequency, path);
        testNoiseScale(noise.scale, path);
    }

    /** 2D noise includes an addition `minVal` and `maxVal`. */
    private void testNoise(NoiseSettings2D noise, String path) {
        if (noise.min > noise.max) {
            log(high, "Invalid range @ {}. minVal > maxVal.", path);
        }
        testNoiseFrequency(noise.frequency, path);
        testNoiseScale(noise.scale, path);
    }

    /** Chance should always be between 0 and 1. */
    private void testChance(double chance, String path) {
        if (chance < 0 || chance > 1) {
            log(low, "Poor chance @ {}. Use a value between 0 and 1.", path);
        }
    }

    /** Noise frequency is the same. Include a more helpful message. */
    private void testNoiseFrequency(float frequency, String path) {
        if (frequency < 0 || frequency > 1) {
            log(low, "Poor frequency value @ {}. If you are converting presets from a previous version, " +
                "`spacing` can be converted using the formula `frequency = 1 / spacing`.", path);
        }
    }

    /** Scale should always be between 0 and 1. */
    private void testNoiseScale(float scale, String path) {
        if (scale < 0 || scale > 1) {
            log(low, "Poor scale @ {}. Use a number between 0 and 1.", path);
        }
    }

    /** Direction.ALL covers all directions. Others are unnecessary. */
    private void testDirections(Direction[] directions, String path) {
        for (Direction d : directions) {
            if (d.equals(Direction.ALL) && directions.length > 1) {
                log(low, "Direction array contains unnecessary values @ {}.", path);
                return;
            }
        }
    }
}