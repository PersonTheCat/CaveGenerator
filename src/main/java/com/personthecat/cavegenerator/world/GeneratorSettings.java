package com.personthecat.cavegenerator.world;

import com.personthecat.cavegenerator.util.Direction;
import com.personthecat.cavegenerator.util.HjsonTools;
import com.personthecat.cavegenerator.util.NoiseSettings3D;
import com.personthecat.cavegenerator.util.ScalableFloat;
import com.personthecat.cavegenerator.world.feature.GiantPillar;
import com.personthecat.cavegenerator.world.feature.LargeStalactite;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.structure.template.PlacementSettings;
import org.hjson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.personthecat.cavegenerator.util.CommonMethods.*;
import static com.personthecat.cavegenerator.util.HjsonTools.*;

/**
 * Contains the entire sum of the data to be used by CaveGenerators.
 * Some sacrifices were made in terms of readability in order to
 * allow for all fields to be both final and (usually) have default
 * values. If you are interested in proposing ways to improve this
 * class' readability, please feel free to submit an issue or pull
 * request on GitHub.
 *
 * @author PersonTheCat
 */
public class GeneratorSettings {
    public final SpawnSettings conditions;
    public final IBlockState[] replaceable;
    public final TunnelSettings tunnels;
    public final RavineSettings ravines;
    public final RoomSettings rooms;
    public final CavernSettings caverns;
    public final StructureSettings[] structures;
    public final DecoratorSettings decorators;

    /**
     * Primary and sole constructor.
     * None of these values are optional.
     */
    public GeneratorSettings(
        SpawnSettings conditions,
        IBlockState[] replaceable,
        TunnelSettings tunnels,
        RavineSettings ravines,
        RoomSettings rooms,
        CavernSettings caverns,
        StructureSettings[] structures,
        DecoratorSettings decorators
    ) {
        this.conditions = conditions;
        this.replaceable = replaceable;
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
        /** Whether this preset is enabled globally. */
        public final boolean enabled;
        /** Whether to use a blacklist for biomes, instead of a whitelist. */
        public final boolean biomeBlacklist;
        /** The biomes in which most features will generate. */
        public final Biome[] biomes;
        /** Whether to use a blacklist for dimensions, instead of a whitelist. */
        public final boolean dimensionBlacklist;
        /** The dimension IDs in which all features will generate. */
        public final int[] dimensions;
        /** The global minimum height bounds for this generator. */
        public final int minHeight;
        /** The global maximum height bounds for this generator. */
        public final int maxHeight;

        /** Primary constructor. */
        public SpawnSettings(
            boolean enabled,
            boolean biomeBlacklist,
            Biome[] biomes,
            boolean dimensionBlacklist,
            int[] dimensions,
            int minHeight,
            int maxHeight
        ) {
            this.enabled = enabled;
            this.biomeBlacklist = biomeBlacklist;
            this.biomes = biomes;
            this.dimensionBlacklist = dimensionBlacklist;
            this.dimensions = dimensions;
            this.minHeight = minHeight;
            this.maxHeight = maxHeight;
        }

        /**
         * On why Optional types are used in construction of this object:
         *
         *   The values passed into this constructor generally result
         * from parsing JsonObjects somewhere externally. The current
         * design philosophy of this program is such that all json
         * fields must be optional unless absolutely necessary, e.g.
         * when instantiating a world decorator which is based on a
         * particular Block. As a result, those fields can either be
         * `null` or wrapped inside of Optional types.
         *   One alternative is to use null values directly, but I
         * would strongly prefer to avoid directly interacting with
         * null values whenever possible, as doing so requires a
         * higher level of diligence and fidelity.
         *   Another alternative is to construct the object directly
         * from its respective JsonObject. This has the effect of
         * very clearly indicating the purpose of each field, how it's
         * supposed to change (usually, it is *not*), and where it's
         * supposed to come from (usually, the json file), etc.
         * However, in practice, the literal width of lines produced
         * by writing constructors in this format is rather unsightly
         * and difficult to interpret.
         *   Yet another alternative is to construct the object
         * reflectively, employing annotated fields to indicate
         * default values. This has the benefit of producing shorter
         * and prettier code. Moreover, I have written a nearly-working
         * version of some code that does exactly that; however, using
         * it seems inconsistent with the otherwise explicit and
         * (usually) safe nature of this program.
         *   In my opinion, this is more a problem with the language
         * itself--the existence of null values, the lack of modern
         * features including default arguments, etc--than it is a
         * problem of style. The best solution that I have thus far
         * been able to come up with that accounts for all of these
         * problems is to use optional types. If you would like to
         * propose an alternative syntax, please feel free to submit
         * an issue or pull request on GitHub. -PersonTheCat
         */
        @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
        public SpawnSettings(JsonObject json) {
            this(
                getBoolOr(json, "enabled", true),
                getBoolOr(json, "useBiomeBlacklist", false),
                getObject(json, "biomes").map(HjsonTools::getAllBiomes).orElse(new Biome[0]),
                getBoolOr(json, "useDimensionBlacklist", false),
                getIntArrayOr(json, "dimensions", new int[0]),
                getIntOr(json, "minHeight", 8),
                getIntOr(json, "maxHeight", 128)
            );
        }
    }

    /**
     * Stores information regarding how this generator's
     * caverns should be shaped.
     */
    public static class TunnelSettings {
        /** Controls a vanilla function for reducing vertical noise. */
        public final boolean noiseYReduction;
        public final boolean resizeBranches;

        /** Horizontal rotation. */
        public final ScalableFloat twistXZ;
        /** Vertical rotation. */
        public final ScalableFloat twistY;
        /** Overall scale. */
        public final ScalableFloat scale;
        /** Vertical scale. */
        public final ScalableFloat scaleY;
        /** Horizontal angle in radians. */
        public final ScalableFloat angleXZ;
        /** Vertical angle in radians. */
        public final ScalableFloat angleY;

        // Inverse chance = (1 / #) chance.
        /** The chance that this tunnel will spawn as part of a system. */
        public final int spawnInSystemInverseChance;
        /**  Increases the distance between tunnels. */
        public final int spawnIsolatedInverseChance;

        /**
         * The expected distance of the first cave generated in this
         * system. 0 -> (132 to 136)?
         */
        public final int startingDistance;

        public final int minHeight;
        public final int maxHeight;
        public final int frequency;

        /** Default values used for the scalable floats here. */
        public static final ScalableFloat DEFAULT_TWIST_XZ =
            new ScalableFloat(1.0f, 0.75f, 4.0f, 0.0f, 0.0f);
        public static final ScalableFloat DEFAULT_TWIST_Y =
            new ScalableFloat(1.0f, 0.9f, 4.0f, 0.0f, 0.0f);
        public static final ScalableFloat DEFAULT_SCALE =
            new ScalableFloat(1.0f, 1.0f, 0.0f, 0.0f, 1.0f);
        public static final ScalableFloat DEFAULT_SCALE_Y =
            new ScalableFloat(1.0f, 1.0f, 0.0f, 1.0f, 1.0f);
        public static final ScalableFloat DEFAULT_ANGLE_XZ =
            new ScalableFloat(1.0f, 1.0f, 0.0f, 0.0f, 1.0f);
        public static final ScalableFloat DEFAULT_ANGLE_Y =
            new ScalableFloat(1.0f, 1.0f, 0.0f, 0.0f, 0.25f);

        /**
         * The primary constructor of this object. Necessary for allowing all
         * fields to to be final.
         */
        public TunnelSettings(
            boolean noiseYReduction,
            boolean resizeBranches,
            ScalableFloat twistXZ,
            ScalableFloat twistY,
            ScalableFloat scale,
            ScalableFloat scaleY,
            ScalableFloat angleXZ,
            ScalableFloat angleY,
            int spawnInSystemInverseChance,
            int spawnIsolatedInverseChance,
            int startingDistance,
            int minHeight,
            int maxHeight,
            int frequency
        ) {
            this.noiseYReduction = noiseYReduction;
            this.resizeBranches = resizeBranches;
            this.twistXZ = twistXZ;
            this.twistY = twistY;
            this.scale = scale;
            this.scaleY = scaleY;
            this.angleXZ = angleXZ;
            this.angleY = angleY;
            this.spawnInSystemInverseChance = spawnInSystemInverseChance;
            this.spawnIsolatedInverseChance = spawnIsolatedInverseChance;
            this.startingDistance = startingDistance;
            this.minHeight = minHeight;
            this.maxHeight = maxHeight;
            this.frequency = frequency;
        }

        /** From Json. */
        public TunnelSettings(JsonObject tun) {
            this(
                getBoolOr(tun, "noiseYReduction", true),
                getBoolOr(tun, "resizeBranches", true),
                getScalableFloatOr(tun, "twistXZ", DEFAULT_TWIST_XZ),
                getScalableFloatOr(tun, "twistY", DEFAULT_TWIST_Y),
                getScalableFloatOr(tun, "scale", DEFAULT_SCALE),
                getScalableFloatOr(tun, "scaleY", DEFAULT_SCALE_Y),
                getScalableFloatOr(tun, "angleXZ", DEFAULT_ANGLE_XZ),
                getScalableFloatOr(tun, "angleY", DEFAULT_ANGLE_Y),
                getIntOr(tun, "spawnInSystemInverseChance", 4),
                getIntOr(tun, "spawnIsolatedInverseChance", 7),
                getIntOr(tun, "distance", 0),
                getIntOr(tun, "minHeight", 8),
                getIntOr(tun, "maxHeight", 128),
                getIntOr(tun, "frequency", 15)
            );
        }

        /**
         * Constructs a new instance of this object using the default values only.
         * Using Optional#empty instead of raw values so that all default values
         * can remain within the first constructor above.
         */
        public TunnelSettings() {
            this(new JsonObject());
        }
    }

    /**
     * Stores information regarding how this generator's
     * ravines should be shaped. Mostly similar to
     * TunnelSettings, but with a few different values
     * to provide defaults for matching the vanilla
     * ravine style.
     */
    public static class RavineSettings {
        /** Non-randomly reduces vertical noise. */
        public final float noiseYFactor;

        /** Horizontal rotation. */
        public final ScalableFloat twistXZ;
        /** Vertical rotation. */
        public final ScalableFloat twistY;
        /** Overall scale. */
        public final ScalableFloat scale;
        /** Vertical scale. */
        public final ScalableFloat scaleY;
        /** Horizontal angle in radians. */
        public final ScalableFloat angleXZ;
        /** Vertical angle in radians. */
        public final ScalableFloat angleY;

        /** The expected distance of the first cave generated in this system. 0 -> 121? */
        public final int startingDistance;

        // Inverse chance = (1 / #) chance.
        public final int inverseChance;
        public final int minHeight;
        public final int maxHeight;

        /** Default values used for the scalable floats here. */
        public static final ScalableFloat DEFAULT_TWIST_XZ =
            new ScalableFloat(1.0f, 0.5f, 4.0f, 0.0f, 0.0f);
        public static final ScalableFloat DEFAULT_TWIST_Y =
            new ScalableFloat(1.0f, 0.8f, 2.0f, 0.0f, 0.0f);
        public static final ScalableFloat DEFAULT_SCALE =
            new ScalableFloat(1.0f, 1.0f, 0.0f, 0.0f, 1.0f);
        public static final ScalableFloat DEFAULT_SCALE_Y =
            new ScalableFloat(1.0f, 1.0f, 0.0f, 1.0f, 1.0f);
        public static final ScalableFloat DEFAULT_ANGLE_XZ =
            new ScalableFloat(1.0f, 1.0f, 0.0f, 0.0f, 1.0f);
        public static final ScalableFloat DEFAULT_ANGLE_Y =
            new ScalableFloat(1.0f, 1.0f, 0.0f, 0.0f, 0.25f);

        /** Primary constructor. */
        public RavineSettings(
            float noiseYFactor,
            ScalableFloat twistXZ,
            ScalableFloat twistY,
            ScalableFloat scale,
            ScalableFloat scaleY,
            ScalableFloat angleXZ,
            ScalableFloat angleY,
            int startingDistance,
            int minHeight,
            int maxHeight,
            int inverseChance
        ) {
            this.noiseYFactor = noiseYFactor;
            this.twistXZ = twistXZ;
            this.twistY = twistY;
            this.scale = scale;
            this.scaleY = scaleY;
            this.angleXZ = angleXZ;
            this.angleY = angleY;
            this.startingDistance = startingDistance;
            this.minHeight = minHeight;
            this.maxHeight = maxHeight;
            this.inverseChance = inverseChance;
        }

        /** From Json. */
        public RavineSettings(JsonObject rav) {
            this(
                getFloatOr(rav, "noiseYFactor", 0.7f),
                getScalableFloatOr(rav, "twistXZ", DEFAULT_TWIST_XZ),
                getScalableFloatOr(rav, "twistY", DEFAULT_TWIST_Y),
                getScalableFloatOr(rav, "scale", DEFAULT_SCALE),
                getScalableFloatOr(rav, "scaleY", DEFAULT_SCALE_Y),
                getScalableFloatOr(rav, "angleXZ", DEFAULT_ANGLE_XZ),
                getScalableFloatOr(rav, "angleY", DEFAULT_ANGLE_Y),
                getIntOr(rav, "distance", 0),
                getIntOr(rav, "minHeight", 20),
                getIntOr(rav, "maxHeight", 40),
                getIntOr(rav, "inverseChance", 50)
            );
        }

        /** Default values. */
        public RavineSettings() {
            this(new JsonObject());
        }
    }

    /**
     * Stores information regarding how this generator's
     * rooms should be shaped.
     */
    public static class RoomSettings {
        // The overall radius of this room;
        public final float scale;
        // Multiplies `scale` on the vertical axis.
        public final float scaleY;

        /** Primary constructor. */
        public RoomSettings(float scale, float scaleY) {
            this.scale = scale;
            this.scaleY = scaleY;
        }

        /** From Json */
        public RoomSettings(JsonObject rooms) {
            this(
                getFloatOr(rooms, "scale", 6.0f),
                getFloatOr(rooms, "scaleY", 0.5f)
            );
        }

        /** Default values. */
        public RoomSettings() {
            this(new JsonObject());
        }
    }

    /**
     * Stores information regarding how this generator's
     * caverns should be shaped.
     */
    public static class CavernSettings {
        public final boolean enabled;
        public final int minHeight;
        public final int maxHeight;
        public final NoiseSettings3D noise;

        /** Default values used for the noise settings here. */
        public static final NoiseSettings3D DEFAULT_NOISE =
            new NoiseSettings3D(0.2f, 70.00f, 0.50f, 1);

        /** Primary constructor. */
        public CavernSettings(boolean enabled, int minHeight, int maxHeight, NoiseSettings3D noise) {
            this.enabled = enabled;
            this.minHeight = minHeight;
            this.maxHeight = maxHeight;
            this.noise = noise;
        }

        /** From Json. */
        public CavernSettings(JsonObject caverns) {
            this(
                getBoolOr(caverns, "enabled", false),
                getIntOr(caverns, "minHeight", 10),
                getIntOr(caverns, "maxHeight", 50),
                getNoiseSettingsOr(caverns, "noise3D", DEFAULT_NOISE)
            );
        }

        /** Default values. */
        public CavernSettings() {
            this(new JsonObject());
        }
    }

    /**
     * Stores information regarding the structures that
     * will be spawned by this preset.
     */
    public static class StructureSettings {
        /** This needs to be guaranteed to exist. */
        public final String name;
        /** The source blocks that can be selected for this structure to spawn. */
        public final IBlockState[] matchers;
        /** Whether the structure should spawn on the floor, ceiling, or both. */
        public final Direction[] directions;
        /** Any relative coordinates that should be air. */
        public final BlockPos[] airMatchers;
        /** Any relative coordinates that should be solid. */
        public final BlockPos[] solidMatchers;
        /** Structure placement settings. */
        public final PlacementSettings settings;
        /** The relative coordinates to the selected block that this should spawn. */
        public final BlockPos offset;
        /** The minimum percentage of blocks that must be surrounded by air. */
        public final float minBurialPercentage;
        /** The 0-100% chance that this structure should spawn in a chunk. */
        public final float chance;
        /** The number of times per chunk this structure should try to spawn. */
        public final int frequency;
        /** Minimum height bound. */
        public final int minHeight;
        /** Maximum height bound. */
        public final int maxHeight;
        /** Whether to display the location of each spawn. */
        public final boolean debugSpawns;
        /** Whether to rotate the structure randomly upon placement. */
        public final boolean rotateRandomly;

        /** The default PlacementSettings to be used by most structures. */
        public static final PlacementSettings DEFAULT_SETTINGS =
            new PlacementSettings().setReplacedBlock(Blocks.STONE);

        /** Primary constructor */
        public StructureSettings(
            String name,
            PlacementSettings settings,
            IBlockState[] matchers,
            Direction[] directions,
            BlockPos[] airMatchers,
            BlockPos[] solidMatchers,
            BlockPos offset,
            float minBurialPercentage,
            float chance,
            int frequency,
            int minHeight,
            int maxHeight,
            boolean debugSpawns,
            boolean rotateRandomly
        ) {
            this.name = name;
            this.settings = settings;
            this.matchers = matchers;
            this.directions = directions;
            this.airMatchers = airMatchers;
            this.solidMatchers = solidMatchers;
            this.offset = offset;
            this.minBurialPercentage = minBurialPercentage;
            this.chance = chance;
            this.frequency = frequency;
            this.minHeight = minHeight;
            this.maxHeight = maxHeight;
            this.debugSpawns = debugSpawns;
            this.rotateRandomly = rotateRandomly;
        }

        /**
         * Constructs a new instance of this object using optional
         * values.
         * @see GeneratorSettings.SpawnSettings#SpawnSettings for
         * information on why Optional types are used here.
         */
        @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
        public StructureSettings(JsonObject structure) {
            this(
                getString(structure, "name") // Must exist.
                    .orElseThrow(() -> runEx("Structures must contain the field \"name.\"")),
                getPlacementSettings(structure),
                getBlocksOr(structure, "matchers", Blocks.STONE.getDefaultState()),
                getDirectionsOr(structure, "directions", Direction.ALL),
                getPositionsOr(structure, "airMatchers" /* No defaults */),
                getPositionsOr(structure, "solidMatchers" /* No defaults */),
                getPositionOr(structure, "offset", new BlockPos(0, 0, 0)),
                getFloatOr(structure, "minBurialPercentage", 0.0f),
                getFloatOr(structure, "chance", 100.0f),
                getIntOr(structure, "frequency", 10),
                getIntOr(structure, "minHeight", 0),
                getIntOr(structure, "maxHeight", 48),
                getBoolOr(structure, "debugSpawns", false),
                getBoolOr(structure, "rotateRandomly", false)
            );
        }
    }

    /**
     * Stores information regarding how this generator's
     * miscellaneous decorators should be shaped.
     */
    public static class DecoratorSettings {
        public StoneCluster[] stoneClusters = new StoneCluster[0];
        public StoneLayer[] stoneLayers = new StoneLayer[0];
        public CaveBlocks[] caveBlocks = new CaveBlocks[] { CaveBlocks.VANILLA_LAVA };
        public WallDecorators[] ceilingDecorators = new WallDecorators[0];
        public WallDecorators[] floorDecorators = new WallDecorators[0];
        public WallDecorators[] wallDecorators = new WallDecorators[0];
        public LargeStalactite[] stalactites = new LargeStalactite[0];
        public GiantPillar[] pillars = new GiantPillar[0];

        /** Sorts the input decorators into each appropriate field. */
        public void sortWallDecorators(WallDecorators[] decorators) {
            // Create separate arrays for each side to be decorated.
            List<WallDecorators> ceiling = new ArrayList<>();
            List<WallDecorators> floor = new ArrayList<>();
            List<WallDecorators> wall = new ArrayList<>();

            // Match directions from each decorator.
            for (WallDecorators decorator : decorators) {
                for (Direction d : decorator.getDirections()) {
                    switch (d) {
                        case UP :
                            ceiling.add(decorator);
                            break;
                        case DOWN :
                            floor.add(decorator);
                            break;
                        case SIDE :
                            wall.add(decorator);
                            break;
                        default:
                            // It's nice not having ownership.
                            ceiling.add(decorator);
                            floor.add(decorator);
                            wall.add(decorator);
                    }
                }
            }

            // Convert the lists into standard arrays;
            ceilingDecorators = toArray(ceiling);
            floorDecorators = toArray(floor);
            wallDecorators = toArray(wall);
        }
    }
}