package com.personthecat.cavegenerator.world;

import com.personthecat.cavegenerator.model.Direction;
import com.personthecat.cavegenerator.model.NoiseSettings2D;
import com.personthecat.cavegenerator.model.NoiseSettings3D;
import com.personthecat.cavegenerator.model.ScalableFloat;
import com.personthecat.cavegenerator.model.generator.*;
import com.personthecat.cavegenerator.util.*;
import jdk.nashorn.internal.ir.annotations.Immutable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.structure.template.PlacementSettings;
import org.apache.commons.lang3.ArrayUtils;
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
@Immutable
public class GeneratorSettings {
    public final SpawnSettings conditions;
    public final IBlockState[] replaceable;
    public final TunnelSettings[] tunnels;
    public final RavineSettings[] ravines;
    public final RoomSettings rooms;
    public final CavernSettings caverns;
    public final StructureSettings[] structures;
    public final DecoratorSettings decorators;
    public final JsonObject preset;

    /**
     * Primary and sole constructor.
     * None of these values are optional.
     */
    public GeneratorSettings(
        SpawnSettings conditions,
        TunnelSettings[] tunnels,
        RavineSettings[] ravines,
        RoomSettings rooms,
        CavernSettings caverns,
        StructureSettings[] structures,
        DecoratorSettings decorators,
        IBlockState[] replaceable,
        boolean replaceDecorators,
        JsonObject preset
    ) {
        this.conditions = conditions;
        this.tunnels = tunnels;
        this.ravines = ravines;
        this.rooms = rooms;
        this.caverns = caverns;
        this.structures = structures;
        this.decorators = decorators;
        this.preset = preset;
        this.replaceable = getReplaceable(
            replaceable,
            decorators,
            replaceDecorators
        );
    }

    private IBlockState[] getReplaceable(IBlockState[] base, DecoratorSettings decorators, boolean replaceDecorators) {
        if (replaceDecorators) {
            return ArrayUtils.addAll(base, decorators.getDecoratorBlocks());
        }
        return base;
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
                getIntArrayOr(json, "dimensions", new int[] {0}),
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
        public final ScalableFloat dYaw;
        /** Vertical rotation. */
        public final ScalableFloat dPitch;
        /** Overall scale. */
        public final ScalableFloat scale;
        /** Vertical scale. */
        public final ScalableFloat scaleY;
        /** Horizontal angle in radians. */
        public final ScalableFloat yaw;
        /** Vertical angle in radians. */
        public final ScalableFloat pitch;

        // Inverse chance = (1 / #) chance.
        /** The chance that this tunnel will spawn as part of a system. */
        public final int systemInverseChance;
        /**  Increases the distance between tunnels. */
        public final int isolatedInverseChance;
        /** The maximum possible number of branches at system origins. */
        public final int systemDensity;

        /**
         * The expected distance of the first cave generated in this
         * system. 0 -> (132 to 136)?
         */
        public final int startDistance;

        public final int minHeight;
        public final int maxHeight;
        public final int frequency;

        /** Default values used for the scalable floats here. */
        public static final ScalableFloat DEFAULT_DELTA_YAW =
            new ScalableFloat(0.0f, 0.0f, 0.75f, 4.0f, 1.0f);
        public static final ScalableFloat DEFAULT_DELTA_PITCH =
            new ScalableFloat(0.0f, 0.0f, 0.9f, 2.0f, 1.0f);
        public static final ScalableFloat DEFAULT_SCALE =
            new ScalableFloat(0.0f, 1.0f, 1.0f, 0.0f, 1.0f);
        public static final ScalableFloat DEFAULT_SCALE_Y =
            new ScalableFloat(1.0f, 1.0f, 1.0f, 0.0f, 1.0f);
        public static final ScalableFloat DEFAULT_YAW =
            new ScalableFloat(0.0f, 1.0f, 1.0f, 0.0f, 1.0f);
        public static final ScalableFloat DEFAULT_PITCH =
            new ScalableFloat(0.0f, 0.25f, 1.0f, 0.0f, 1.0f);

        /**
         * The primary constructor of this object. Necessary for allowing all
         * fields to to be final.
         */
        public TunnelSettings(
            boolean noiseYReduction,
            boolean resizeBranches,
            ScalableFloat dYaw,
            ScalableFloat dPitch,
            ScalableFloat scale,
            ScalableFloat scaleY,
            ScalableFloat yaw,
            ScalableFloat pitch,
            int systemInverseChance,
            int isolatedInverseChance,
            int systemDensity,
            int startingDistance,
            int minHeight,
            int maxHeight,
            int frequency
        ) {
            this.noiseYReduction = noiseYReduction;
            this.resizeBranches = resizeBranches;
            this.dYaw = dYaw;
            this.dPitch = dPitch;
            this.scale = scale;
            this.scaleY = scaleY;
            this.yaw = yaw;
            this.pitch = pitch;
            this.systemInverseChance = systemInverseChance;
            this.isolatedInverseChance = isolatedInverseChance;
            this.systemDensity = systemDensity;
            this.startDistance = startingDistance;
            this.minHeight = minHeight;
            this.maxHeight = maxHeight;
            this.frequency = frequency;
        }

        /** From Json. */
        public TunnelSettings(JsonObject tun) {
            this(
                getBoolOr(tun, "noiseYReduction", true),
                getBoolOr(tun, "resizeBranches", true),
                getScalableFloatOr(tun, "dYaw", DEFAULT_DELTA_YAW),
                getScalableFloatOr(tun, "dPitch", DEFAULT_DELTA_PITCH),
                getScalableFloatOr(tun, "scale", DEFAULT_SCALE),
                getScalableFloatOr(tun, "scaleY", DEFAULT_SCALE_Y),
                getScalableFloatOr(tun, "yaw", DEFAULT_YAW),
                getScalableFloatOr(tun, "pitch", DEFAULT_PITCH),
                invert(getFloatOr(tun, "systemChance", 0.25f)),
                invert(getFloatOr(tun, "isolatedChance", 0.14f)),
                getIntOr(tun, "systemDensity", 4),
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
        public TunnelSettings(boolean blankSlate) {
            this(
                blankSlate ?
                new JsonObject().add("frequency", 0) :
                new JsonObject()
            );
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
        public final ScalableFloat dYaw;
        /** Vertical rotation. */
        public final ScalableFloat dPitch;
        /** Overall scale. */
        public final ScalableFloat scale;
        /** Vertical scale. */
        public final ScalableFloat scaleY;
        /** Horizontal angle in radians. */
        public final ScalableFloat yaw;
        /** Vertical angle in radians. */
        public final ScalableFloat pitch;

        /** The expected distance of the first cave generated in this system. 0 -> 121? */
        public final int startDistance;

        // Inverse chance = (1 / #) chance.
        public final int inverseChance;
        public final int minHeight;
        public final int maxHeight;

        /** Settings related to the optional use of noise-based wall generation. */
        public final boolean useWallNoise;
        public final NoiseSettings2D wallNoise;

        /** Default values used for the scalable floats here. */
        public static final ScalableFloat DEFAULT_DELTA_YAW =
            new ScalableFloat(0.0f, 0.0f, 0.5f, 4.0f, 1.0f);
        public static final ScalableFloat DEFAULT_DELTA_PITCH =
            new ScalableFloat(0.0f, 0.0f, 0.8f, 2.0f, 1.0f);
        public static final ScalableFloat DEFAULT_SCALE =
            new ScalableFloat(0.0f, 2.0f, 1.0f, 0.0f, 1.0f);
        public static final ScalableFloat DEFAULT_SCALE_Y =
            new ScalableFloat(3.0f, 1.0f, 1.0f, 0.0f, 1.0f);
        public static final ScalableFloat DEFAULT_YAW =
            new ScalableFloat(0.0f, 1.0f, 1.0f, 0.0f, 1.0f);
        public static final ScalableFloat DEFAULT_PITCH =
            new ScalableFloat(0.0f, 0.25f, 1.0f, 0.0f, 1.0f);

        /** The default noise values to be used for ravine walls. */
        public static final NoiseSettings2D DEFAULT_WALL_NOISE = NoiseSettings2D.builder()
            .scale(0.1f)
            .frequency(0.5f)
            .min(0)
            .max(4)
            .build();

        /** Primary constructor. */
        public RavineSettings(
            float noiseYFactor,
            ScalableFloat dYaw,
            ScalableFloat dPitch,
            ScalableFloat scale,
            ScalableFloat scaleY,
            ScalableFloat yaw,
            ScalableFloat pitch,
            int startingDistance,
            int minHeight,
            int maxHeight,
            int inverseChance,
            boolean useWallNoise,
            NoiseSettings2D wallNoise
        ) {
            this.noiseYFactor = noiseYFactor;
            this.dYaw = dYaw;
            this.dPitch = dPitch;
            this.scale = scale;
            this.scaleY = scaleY;
            this.yaw = yaw;
            this.pitch = pitch;
            this.startDistance = startingDistance;
            this.minHeight = minHeight;
            this.maxHeight = maxHeight;
            this.inverseChance = inverseChance;
            this.useWallNoise = useWallNoise;
            this.wallNoise = wallNoise;
        }

        /** From Json. */
        public RavineSettings(JsonObject rav) {
            this(
                getFloatOr(rav, "noiseYFactor", 0.7f),
                getScalableFloatOr(rav, "dYaw", DEFAULT_DELTA_YAW),
                getScalableFloatOr(rav, "dPitch", DEFAULT_DELTA_PITCH),
                getScalableFloatOr(rav, "scale", DEFAULT_SCALE),
                getScalableFloatOr(rav, "scaleY", DEFAULT_SCALE_Y),
                getScalableFloatOr(rav, "yaw", DEFAULT_YAW),
                getScalableFloatOr(rav, "pitch", DEFAULT_PITCH),
                getIntOr(rav, "distance", 0),
                getIntOr(rav, "minHeight", 20),
                getIntOr(rav, "maxHeight", 40),
                invert(getFloatOr(rav, "chance", 0.02f)),
                getObject(rav, "wallNoise").isPresent(),
                getObject(rav, "wallNoise")
                    .map(o -> getNoiseSettingsOr(o, "noise2D", DEFAULT_WALL_NOISE))
                    .orElse(DEFAULT_WALL_NOISE) // To-do: noise blocks at optional levels.
            );
        }

        /** Default values. */
        public RavineSettings(boolean blankSlate) {
            this(
                blankSlate ?
                new JsonObject().add("chance", 0) :
                new JsonObject()
            );
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
        public RoomSettings(boolean blankSlate) {
            this(
                blankSlate ?
                new JsonObject().add("scale", 0) :
                new JsonObject()
            );
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
        public final NoiseSettings3D[] noise;
        public final NoiseSettings2D ceilNoise, floorNoise;

        /** Default values used for the noise settings here. */
        public static final NoiseSettings3D DEFAULT_NOISE =
            NoiseSettings3D.builder()
            .frequency(0.0143f)
            .scale(0.2f)
            .scaleY(0.5f)
            .octaves(1)
            .build();

        public static final NoiseSettings2D DEFAULT_CEIL_NOISE = NoiseSettings2D.builder()
            .frequency(0.02f)
            .scale(0.5f)
            .min(-17)
            .max(-3)
            .build();

        public static final NoiseSettings2D DEFAULT_FLOOR_NOISE = NoiseSettings2D.builder()
            .frequency(0.02f)
            .scale(0.5f)
            .min(0)
            .max(8)
            .build();

        /** Primary constructor. */
        public CavernSettings(
            boolean enabled,
            int minHeight,
            int maxHeight,
            NoiseSettings3D[] noise,
            NoiseSettings2D ceilNoise,
            NoiseSettings2D floorNoise
        ) {
            this.enabled = enabled;
            this.minHeight = minHeight;
            this.maxHeight = maxHeight;
            this.noise = noise;
            this.ceilNoise = ceilNoise;
            this.floorNoise = floorNoise;
        }

        /** From Json. */
        public CavernSettings(JsonObject caverns) {
            this(
                getBoolOr(caverns, "enabled", false),
                getIntOr(caverns, "minHeight", 10),
                getIntOr(caverns, "maxHeight", 50),
                getNoiseArray(caverns, "noise3D", DEFAULT_NOISE),
                getObject(caverns, "ceiling").map(o ->
                    getNoiseSettingsOr(o, "noise2D", DEFAULT_CEIL_NOISE))
                    .orElse(DEFAULT_CEIL_NOISE),
                getObject(caverns, "floor").map(o ->
                    getNoiseSettingsOr(o, "noise2D", DEFAULT_FLOOR_NOISE))
                    .orElse(DEFAULT_FLOOR_NOISE)
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
        public final Direction.Container directions;
        /** Any relative coordinates that should be air. */
        public final BlockPos[] airMatchers;
        /** Any relative coordinates that should be solid. */
        public final BlockPos[] solidMatchers;
        /** Any relative coordinates that should be non-solid. */
        public final BlockPos[] nonSolidMatchers;
        /** Any relative coordinates that should be water. */
        public final BlockPos[] waterMatchers;
        /** Structure placement settings. */
        public final PlacementSettings settings;
        /** The relative coordinates to the selected block that this should spawn. */
        public final BlockPos offset;
        /** The minimum percentage of blocks that must be surrounded by air. */
        public final float minBurialPercentage;
        /** The 0-1 chance that this structure should spawn in a chunk. */
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
            BlockPos[] nonSolidMatchers,
            BlockPos[] waterMatchers,
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
            this.directions = Direction.Container.from(directions);
            this.airMatchers = airMatchers;
            this.solidMatchers = solidMatchers;
            this.nonSolidMatchers = nonSolidMatchers;
            this.waterMatchers = waterMatchers;
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
                getPositionsOr(structure, "nonSolidMatchers" /* No defaults */),
                getPositionsOr(structure, "waterMatchers" /* No defaults */),
                getPositionOr(structure, "offset", BlockPos.ORIGIN),
                getFloatOr(structure, "minBurialPercentage", 0.0f),
                getFloatOr(structure, "chance", 1.0f),
                getIntOr(structure, "frequency", 1),
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
        public final Cluster[] clusters;
        public final StoneLayer[] stoneLayers;
        public final CaveBlock[] caveBlocks;
        public final WallDecorator[] ceilingDecorators;
        public final WallDecorator[] floorDecorators;
        public final WallDecorator[] wallDecorators;
        public final LargeStalactite[] stalactites;
        public final GiantPillar[] pillars;

        /**
         * Constructs a new instance of this object using optional
         * values.
         * @see GeneratorSettings.SpawnSettings#SpawnSettings for
         * information on why Optional types are used here.
         */
        @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
        public DecoratorSettings(
            Cluster[] clusters,
            StoneLayer[] stoneLayers,
            Optional<CaveBlock[]> caveBlocks,
            WallDecorator[] decorators,
            LargeStalactite[] stalactites,
            GiantPillar[] pillars,
            boolean blankSlate
        ) {
            WallDecorator[][] sorted = sortWallDecorators(decorators);
            this.caveBlocks = caveBlocks.orElse(blankSlate ?
                new CaveBlock[0] : new CaveBlock[] { CaveBlock.VANILLA_LAVA });
            this.ceilingDecorators = sorted[0];
            this.floorDecorators = sorted[1];
            this.wallDecorators = sorted[2];
            this.clusters = clusters;
            this.stoneLayers = stoneLayers;
            this.stalactites = stalactites;
            this.pillars = pillars;
        }

        /** Sorts the input decorators into each appropriate field. */
        private WallDecorator[][] sortWallDecorators(WallDecorator[] decorators) {
            // Create separate arrays for each side to be decorated.
            List<WallDecorator> ceiling = new ArrayList<>();
            List<WallDecorator> floor = new ArrayList<>();
            List<WallDecorator> wall = new ArrayList<>();

            // Match directions from each decorator.
            for (WallDecorator decorator : decorators) {
                for (Direction d : decorator.getDirections()) {
                    switch (d) {
                        case UP : ceiling.add(decorator); break;
                        case DOWN : floor.add(decorator); break;
                        case SIDE : wall.add(decorator); break;
                        default:
                            ceiling.add(decorator);
                            floor.add(decorator);
                            wall.add(decorator);
                    }
                }
            }
            // Convert the lists into standard arrays;
            return new WallDecorator[][] {
                toArray(ceiling, WallDecorator.class),
                toArray(floor, WallDecorator.class),
                toArray(wall, WallDecorator.class)
            };
        }

        /** Retrieves an array of all blocks used blocks each decorator. */
        public IBlockState[] getDecoratorBlocks() {
            List<IBlockState> blocks = new ArrayList<>();
            for (Cluster cluster : clusters) {
                blocks.add(cluster.getState());
            }
            for (StoneLayer layer : stoneLayers) {
                blocks.add(layer.getState());
            }
            for (WallDecorator wDecorators : ceilingDecorators) {
                blocks.add(wDecorators.getFillBlock());
            }
            for (WallDecorator wDecorators : floorDecorators) {
                blocks.add(wDecorators.getFillBlock());
            }
            for (WallDecorator wDecorators : wallDecorators) {
                blocks.add(wDecorators.getFillBlock());
            }
            return toArray(blocks, IBlockState.class);
        }
    }
}