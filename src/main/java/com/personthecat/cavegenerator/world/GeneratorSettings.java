package com.personthecat.cavegenerator.world;

import com.personthecat.cavegenerator.util.Direction;
import com.personthecat.cavegenerator.util.NoiseSettings;
import com.personthecat.cavegenerator.world.feature.GiantPillar;
import com.personthecat.cavegenerator.world.feature.LargeStalactite;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.structure.template.PlacementSettings;

/**
 * All of this data needs to be set and accessed externally.
 * As such, it doesn't make much sense to go through the
 * boilerplate of using getters and setters.
 */
public class GeneratorSettings {
    public final boolean enabled;
    public final IBlockState[] replaceable;
    public final SpawnSettings conditions;
    public final TunnelSettings tunnels;
    public final RavineSettings ravines;
    public final RoomSettings rooms;
    public final CavernSettings caverns;
    public final StructureSettings[] structures;
    public final DecoratorSettings decorators;

    public GeneratorSettings(
        boolean enabled,
        IBlockState[] replaceable,
        SpawnSettings conditions,
        TunnelSettings tunnels,
        RavineSettings ravines,
        RoomSettings rooms,
        CavernSettings caverns,
        StructureSettings[] structures,
        DecoratorSettings decorators
    ) {
        this.enabled = enabled;
        this.replaceable = replaceable;
        this.conditions = conditions;
        this.tunnels = tunnels;
        this.ravines = ravines;
        this.rooms = rooms;
        this.caverns = caverns;
        this.structures = structures;
        this.decorators = decorators;
    }

    /**
     * Stores information regarding the conditions under
     * which this generator can spawn caves and decorations.
     */
    public static class SpawnSettings {
        // Whether to use a blacklist for biomes, instead of a whitelist.
        public boolean useBiomeBlacklist = false;
        // The biomes in which most features will generate.
        public Biome[] biomes = new Biome[0];
        // Whether to use a blacklist for dimensions, instead of a whitelist.
        public boolean useDimensionBlacklist = false;
        // The dimension IDs in which all features will generate.
        public int[] dimensions = new int[0];
        // The global minimum height bounds for this generator.
        public int minHeight = 8;
        // The global maximum height bounds for this generator.
        public int maxHeight = 128;
    }

    /**
     * Stores information regarding how this generator's
     * caverns should be shaped.
     */
    public static class TunnelSettings {
        // Controls a vanilla function for reducing vertical noise.
        public boolean noiseYReduction = true;
        public boolean resizeBranches = true;

        // These values are to be used for the first segment of this generator's tunnels.
        public float startingTwistXZ = 0.00f; // Horizontal rotation.
        public float startingTwistY = 0.00f; // Vertical rotation.
        public float startingScale = 0.00f; // Overall scale.
        public float startingScaleY = 1.00f; // Vertical scale.
        public float startingAngleXZ = 0.00f; // Horizontal angle in radians.
        public float startingAngleY = 0.00f; // Vertical angle in radians.

        // These values randomly multiply the starting values above.
        public float startingScaleRandFactor = 1.00f;
        public float startingAngleXZRandFactor = 1.00f;
        public float startingAngleYRandFactor = 0.25f;

        // These exponentially modify their counterparts per segment.
        public float twistXZExponent = 1.00f;
        public float twistYExponent = 1.00f;
        public float scaleExponent = 1.00f;
        public float scaleYExponent = 1.00f;

        // These multiply their counterparts per segment.
        public float twistXZFactor = 0.75f;
        public float twistYFactor = 0.90f;
        public float scaleFactor = 1.00f;
        public float scaleYFactor = 1.00f;

        // These randomly multiply their counterparts per segment.
        public float twistXZRandFactor = 4.00f;
        public float twistYRandFactor = 4.00f;
        public float scaleRandFactor = 0.00f;
        public float scaleYRandFactor = 0.00f;

        // Inverse chance = (1 / #) chance.
        public int spawnInSystemInverseChance = 4; // The chance that this tunnel will spawn as part of a system.
        public int spawnIsolatedInverseChance = 7; // Increases the distance between tunnels.

        // The expected distance of the first cave generated in this system. 0 -> 121?
        public int startingDistance = 0;

        public int minHeight = 8;
        public int maxHeight = 128;
        public int frequency = 15;
    }

    /**
     * Stores information regarding how this generator's
     * ravines should be shaped. Mostly similar to
     * TunnelSettings, but with a few different values
     * to provide defaults for matching the vanilla
     * ravine style.
     */
    public static class RavineSettings {
        // Non-randomly reduces vertical noise.
        public float noiseYFactor = 0.70f;

        // These values are to be used for the first segment of this generator's tunnels.
        public float startingTwistXZ = 0.00f; // Horizontal rotation.
        public float startingTwistY = 0.00f; // Vertical rotation.
        public float startingScale = 0.00f; // Overall scale.
        public float startingScaleY = 1.00f; // Vertical scale.
        public float startingAngleXZ = 0.00f; // Horizontal angle in radians.
        public float startingAngleY = 0.00f; // Vertical angle in radians.

        // These values randomly multiply the starting values above.
        public float startingScaleRandFactor = 1.00f;
        public float startingAngleXZRandFactor = 1.00f;
        public float startingAngleYRandFactor = 0.25f;

        // These exponentially modify their counterparts per segment.
        public float twistXZExponent = 1.00f;
        public float twistYExponent = 1.00f;
        public float scaleExponent = 1.00f;
        public float scaleYExponent = 1.00f;

        // These multiply their counterparts per segment.
        public float twistXZFactor = 0.50f;
        public float twistYFactor = 0.80f;
        public float scaleFactor = 1.00f;
        public float scaleYFactor = 1.00f;

        // These randomly multiply their counterparts per segment.
        public float twistXZRandFactor = 4.00f;
        public float twistYRandFactor = 2.00f;
        public float scaleRandFactor = 0.00f;
        public float scaleYRandFactor = 0.00f;

        // The expected distance of the first cave generated in this system. 0 -> 121?
        // May not matter.
        public int startingDistance = 0;

        // Inverse chance = (1 / #) chance.
        public int inverseChance = 50;
        public int minHeight = 20;
        public int maxHeight = 50;
    }

    /**
     * Stores information regarding how this generator's
     * rooms should be shaped.
     */
    public static class RoomSettings {
        // The overall radius of this room;
        public float scale = 6.00f;
        // Multiplies `scale` on the vertical axis.
        public float scaleY = 0.50f;
    }

    /**
     * Stores information regarding how this generator's
     * caverns should be shaped.
     */
    public static class CavernSettings {
        public boolean enabled = false;
        public NoiseSettings noise = new NoiseSettings(0.2f, 70.00f, 0.50f, 1.00f);
    }

    /**
     * Stores information regarding the structures that
     * will be spawned by this preset.
     */
    public static class StructureSettings {
        // This needs to be guaranteed to exist.
        public final String name;

        public StructureSettings(String name) {
            this.name = name;
        }
        // The sources blocks that can be selected for this structure to spawn.
        public IBlockState[] sources = new IBlockState[0];
        // Whether the structure should spawn on the floor, ceiling, or both.
        public Direction[] directions = new Direction[0];
        // Any relative coordinates that should be air.
        public BlockPos[] airMatchers = new BlockPos[0];
        // Any relative coordinates that should be solid.
        public BlockPos[] solidMatchers = new BlockPos[0];
        // Structure placement settings.
        public PlacementSettings settings;
        // The relative coordinates to the selected block that this should spawn.
        public BlockPos offset;
        // The minimum percentage of blocks that must be surrounded by air.
        public double minBurialPercentage = 0.00d;
        // The 0-100% chance that this structure should spawn in a chunk.
        public double chance = 100.00d;
        // The number of times per chunk this structure should try to spawn.
        public int frequency = 10;
        // Minimum height bound.
        public int minHeight = 0;
        // Maximum height bound.
        public int maxHeight = 48;
    }

    /**
     * Stores information regarding how this generator's
     * miscellaneous decorators should be shaped.
     */
    public static class DecoratorSettings {
        public StoneCluster[] stoneClusters = new StoneCluster[0];
        public StoneLayer[] stoneLayers = new StoneLayer[0];
        public CaveBlocks[] caveBlocks = new CaveBlocks[0];
        public WallDecorators[] ceilingDecorators = new WallDecorators[0];
        public WallDecorators[] floorDecorators = new WallDecorators[0];
        public WallDecorators[] wallDecorators = new WallDecorators[0];
        public LargeStalactite[] stalactites = new LargeStalactite[0];
        public GiantPillar[] pillars = new GiantPillar[0];
    }
}