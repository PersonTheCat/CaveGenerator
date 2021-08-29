package personthecat.cavegenerator.world.feature;

import lombok.extern.log4j.Log4j2;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import personthecat.catlib.data.Range;
import personthecat.cavegenerator.presets.data.StructureSettings;

import java.util.Optional;
import java.util.Random;

import static personthecat.catlib.util.Shorthand.full;
import static java.util.Optional.empty;

@Log4j2
public class StructureGenerator extends BasicFeature {

    private final StructureSettings cfg;
    private StructureTemplate structure;

    public StructureGenerator(StructureSettings cfg, final Random rand, final long seed) {
        super(cfg.conditions, rand, seed);
        this.cfg = cfg;
        this.structure = null;
    }

    @Override
    protected void doGenerate(final WorldContext ctx) {
        if (structure == null) structure = StructureSpawner.getTemplate(cfg.name, ctx.level);

        final BlockPos center = new BlockPos(ctx.centerX, 0, ctx.centerZ);
        if (conditions.biomes.test(ctx.region.getBiome(center))) {
            for (int i = 0; i < cfg.count; i++) {
                if (ctx.rand.nextDouble() <= cfg.chance) {
                    this.generateSingle(ctx);
                }
            }
        }
    }

    private void generateSingle(WorldContext ctx) {
        final Optional<BlockPos> spawnPos = this.getSpawnPos(ctx)
            .filter(pos -> conditions.noise.getBoolean(pos.getX(), pos.getY(), pos.getZ()));
        // Attempt to locate a suitable spawn position and then proceed.
        spawnPos.ifPresent(pos -> {
            if (allChecksPass(pos, ctx.region)) {
                this.preStructureSpawn(ctx, pos);
                final BlockPos adjusted = centerBySize(pos, structure.getSize()).offset(cfg.offset);
                StructureSpawner.spawnStructure(structure, cfg.placement, ctx.region, adjusted, ctx.rand);
            }
        });
    }

    private boolean allChecksPass(final BlockPos pos, final WorldGenRegion region) {
        return checkSources(cfg.matchers, region, pos)
            && checkNonSolid(cfg.nonSolidChecks, region, pos)
            && checkSolid(cfg.solidChecks, region, pos)
            && checkAir(cfg.airChecks, region, pos)
            && checkWater(cfg.waterChecks, region, pos)
            && checkBlocks(cfg.blockChecks, region, pos);
    }

    /** Attempts to determine a suitable spawn point in the current location. */
    private Optional<BlockPos> getSpawnPos(final WorldContext ctx) {
        // Favor vertical spawns, detecting horizontal surfaces first.
        if (cfg.directions.up || cfg.directions.down) {
            final Optional<BlockPos> vertical = this.getSpawnPosVertical(ctx, structure);
            if (vertical.isPresent()) {
                return vertical;
            } // else, try horizontal
        }
        // Attempt to locate any vertical surfaces, if necessary.
        if (cfg.directions.side) {
            return this.getSpawnPosHorizontal(ctx, structure);
        }
        Optional<BlockPos> side = empty();
        if (cfg.directions.north) {
            side = this.getSpawnPosN(ctx, structure);
        }
        if (cfg.directions.south && !side.isPresent()) {
            side = this.getSpawnPosS(ctx, structure);
        }
        if (cfg.directions.east && !side.isPresent()) {
            side = this.getSpawnPosE(ctx, structure);
        }
        if (cfg.directions.west && !side.isPresent()) {
            side = this.getSpawnPosW(ctx, structure);
        }
        return side;
    }

    /** Attempts to find a spawn point for this structure on the vertical axis. */
    private Optional<BlockPos> getSpawnPosVertical(final WorldContext ctx, final StructureTemplate structure) {
        for (int i = 0; i < VERTICAL_RETRIES; i++) {
            // Start with random (x, z) coordinates.
            final BlockPos xz = this.randCoords(ctx.rand, structure.getSize(), ctx.centerX, ctx.centerZ);
            final int x = xz.getX();
            final int z = xz.getZ();
            final Range height = conditions.getColumn(x, z);
            final int maxY = cfg.checkSurface
                ? Math.min(ctx.getHeight(x, z) - SURFACE_ROOM, height.max)
                : ctx.region.getHeight();
            final int minY = height.min;
            if (minY >= maxY || !conditions.region.getBoolean(x, z)) continue;

            final int y;
            // Search both -> just up -> just down.
            if (cfg.directions.up && cfg.directions.down) {
                y = this.findOpeningVertical(ctx.rand, ctx.region, x, z, minY, maxY);
            } else if (cfg.directions.up) {
                y = this.randFindCeiling(ctx.region, ctx.rand, x, z, minY, maxY);
            } else {
                y = this.randFindFloor(ctx.region, ctx.rand, x, z, minY, maxY);
            }
            // Check to see if an opening was found, else retry;
            if (y != NONE_FOUND) {
                return full(new BlockPos(x, y, z));
            }
        }
        return empty();
    }

    /** Attempts to find a spawn point for this structure on the horizontal axes. */
    private Optional<BlockPos> getSpawnPosHorizontal(final WorldContext ctx, StructureTemplate structure) {
        final BlockPos size = structure.getSize();
        final Range height = conditions.getColumn(ctx.centerX, ctx.centerZ);
        if (height.diff() != 0 && conditions.region.getBoolean(ctx.centerX, ctx.centerZ)) {
            for (int i = 0; i < HORIZONTAL_RETRIES; i++) {
                final int y = height.rand(ctx.rand);
                final Optional<BlockPos> pos;
                if (ctx.rand.nextBoolean()) {
                    pos = this.randCoordsNS(ctx, size.getX(), y);
                } else {
                    pos = this.randCoordsEW(ctx, size.getZ(), y);
                }
                if (pos.isPresent()) {
                    return pos;
                }
            }
        }
        return empty();
    }

    /** Locates a northern wall as a potential spawn candidate. */
    private Optional<BlockPos> getSpawnPosN(final WorldContext ctx, StructureTemplate structure) {
        final BlockPos size = structure.getSize();
        final Range height = conditions.getColumn(ctx.centerX, ctx.centerZ);
        if (height.diff() != 0 && conditions.region.getBoolean(ctx.centerX, ctx.centerZ)) {
            for (int i = 0; i < HORIZONTAL_RETRIES; i++) {
                final int y = height.rand(ctx.rand);
                final Optional<BlockPos> pos = this.randCoordsN(ctx, size.getX(), y);
                if (pos.isPresent()) {
                    return pos;
                }
            }
        }
        return empty();
    }

    /** Locates a southern wall as a potential spawn candidate. */
    private Optional<BlockPos> getSpawnPosS(final WorldContext ctx, StructureTemplate structure) {
        final BlockPos size = structure.getSize();
        final Range height = conditions.getColumn(ctx.centerX, ctx.centerZ);
        if (height.diff() != 0 && conditions.region.getBoolean(ctx.centerX, ctx.centerZ)) {
            for (int i = 0; i < HORIZONTAL_RETRIES; i++) {
                final int y = height.rand(ctx.rand);
                final Optional<BlockPos> pos = this.randCoordsS(ctx, size.getX(), y);
                if (pos.isPresent()) {
                    return pos;
                }
            }
        }
        return empty();
    }

    /** Locates an eastern wall as a potential spawn candidate. */
    private Optional<BlockPos> getSpawnPosE(final WorldContext ctx, StructureTemplate structure) {
        final BlockPos size = structure.getSize();
        final Range height = conditions.getColumn(ctx.centerX, ctx.centerZ);
        if (height.diff() != 0 && conditions.region.getBoolean(ctx.centerX, ctx.centerZ)) {
            for (int i = 0; i < HORIZONTAL_RETRIES; i++) {
                final int y = height.rand(ctx.rand);
                final Optional<BlockPos> pos = this.randCoordsE(ctx, size.getZ(), y);
                if (pos.isPresent()) {
                    return pos;
                }
            }
        }
        return empty();
    }

    /** Locates a western wall as a potential spawn candidate. */
    private Optional<BlockPos> getSpawnPosW(final WorldContext ctx, StructureTemplate structure) {
        final BlockPos size = structure.getSize();
        final Range height = conditions.getColumn(ctx.centerX, ctx.centerZ);
        if (height.diff() != 0 && conditions.region.getBoolean(ctx.centerX, ctx.centerZ)) {
            for (int i = 0; i < HORIZONTAL_RETRIES; i++) {
                final int y = height.rand(ctx.rand);
                final Optional<BlockPos> pos = this.randCoordsW(ctx, size.getZ(), y);
                if (pos.isPresent()) {
                    return pos;
                }
            }
        }
        return empty();
    }

    /**
     * Generate a random x or z coordinate based on size. Ensure that the resulting coordinate will not
     * produce cascading gen lag.
     */
    private int randCoord(Random rand, int size, int offset) {
        size = Math.abs(size);
        if (size < 16) {
            // Don't let even numbers break chunk bounds (?)
            if (size % 2 == 0) {
                size += 1;
            }
            return cornerInsideChunkBounds(rand, size) + offset;
        }
        // The size is too large. Spawn at the intersection of all four chunks.
        return offset + 8; // chunk * 16 + 16
    }

    /** Generates random, valid coordinate pair for this location. */
    private BlockPos randCoords(final Random rand, final BlockPos size, final int centerX, final int centerZ) {
        final int x = this.randCoord(rand, size.getX(), centerX);
        final int z = this.randCoord(rand, size.getZ(), centerZ);
        return new BlockPos(x, 0, z);
    }

    /**
     * Attempts to find a random surface on the east-west axis by scaling north-south in a random direction.
     * Returns Optional#empty if no surface is found, or if the surface found is not below the terrain height.
     */
    private Optional<BlockPos> randCoordsNS(final WorldContext ctx, final int sizeX, final int y) {
        final int x = cornerInsideChunkBounds(ctx.rand, sizeX) + ctx.centerX;
        final int z = ctx.rand.nextBoolean()
            ? this.findOpeningNorth(ctx.region, x, y, ctx.centerZ)
            : this.findOpeningSouth(ctx.region, x, y, ctx.centerZ);
        if (z != NONE_FOUND && y < ctx.getHeight(x, z)) {
            return full(new BlockPos(x, y, z));
        }
        return empty();
    }

    /**
     * Attempts to find a random surface on the north-south axis by scaling east-west in a random direction.
     * Returns Optional#empty if no surface is found, or if the surface found is not below the terrain height.
     */
    private Optional<BlockPos> randCoordsEW(final WorldContext ctx, int sizeZ, int y) {
        final int z = cornerInsideChunkBounds(ctx.rand, sizeZ) + ctx.centerZ;
        final int x = ctx.rand.nextBoolean()
            ? this.findOpeningEast(ctx.region, y, z, ctx.centerX)
            : this.findOpeningWest(ctx.region, y, z, ctx.centerX);
        if (x != NONE_FOUND && y < ctx.getHeight(x, z)) {
            return full(new BlockPos(x, y, z));
        }
        return empty();
    }

    /** Variant of #randCoords which operates north-bound only. */
    private Optional<BlockPos> randCoordsN(final WorldContext ctx, int sizeX, int y) {
        final int x = cornerInsideChunkBounds(ctx.rand, sizeX) + ctx.centerX;
        final int z = this.findOpeningNorth(ctx.region, x, y, ctx.centerZ);
        if (z != NONE_FOUND && y < ctx.getHeight(x, z)) {
            return full(new BlockPos(x, y, z));
        }
        return empty();
    }

    /** Variant of #randCoordsNS which operates south-bound only. */
    private Optional<BlockPos> randCoordsS(final WorldContext ctx, int sizeX, int y) {
        final int x = cornerInsideChunkBounds(ctx.rand, sizeX) + ctx.centerX;
        final int z = this.findOpeningSouth(ctx.region, x, y, ctx.centerZ);
        if (z != NONE_FOUND && y < ctx.getHeight(x, z)) {
            return full(new BlockPos(x, y, z));
        }
        return empty();
    }

    /** Variant of #randCoordsEW which operates east-bound only. */
    private Optional<BlockPos> randCoordsE(final WorldContext ctx, int sizeZ, int y) {
        final int z = cornerInsideChunkBounds(ctx.rand, sizeZ) + ctx.centerZ;
        final int x = this.findOpeningEast(ctx.region, y, z, ctx.centerX);
        if (x != NONE_FOUND && y < ctx.getHeight(x, z)) {
            return full(new BlockPos(x, y, z));
        }
        return empty();
    }

    /** Variant of #randCoordsEW which operates west-bound only. */
    private Optional<BlockPos> randCoordsW(final WorldContext ctx, int sizeZ, int y) {
        final int z = cornerInsideChunkBounds(ctx.rand, sizeZ) + ctx.centerZ;
        final int x = this.findOpeningWest(ctx.region, y, z, ctx.centerX);
        if (x != NONE_FOUND && y < ctx.getHeight(x, z)) {
            return full(new BlockPos(x, y, z));
        }
        return empty();
    }

    /** Moves each dimension by half of `size` in the opposite direction. */
    private static BlockPos centerBySize(final BlockPos toCenter, final BlockPos size) {
        final int xOffset = (size.getX() / 2) * -1;
        final int zOffset = (size.getZ() / 2) * -1;
        return toCenter.offset(xOffset, 0, zOffset);
    }

    /** All operations related to structures before spawning should be organized herein. */
    private void preStructureSpawn(final WorldContext ctx, final BlockPos pos) {
        if (cfg.rotateRandomly) {
            final Rotation randRotation = Rotation.values()[ctx.rand.nextInt(3)];
            cfg.placement.setRotation(randRotation);
        }
        if (cfg.debugSpawns) {
            log.info("Spawning {} at {}", cfg.name, pos);
        }
        if (!cfg.command.isEmpty()) {
            final String interpolated = cfg.command
                .replace("{x}", String.valueOf(pos.getX()))
                .replace("{y}", String.valueOf(pos.getY()))
                .replace("{z}", String.valueOf(pos.getZ()));
            ctx.execute(interpolated);
        }
    }

    /**
     * Returns a random relative coordinate such that result + size <= 16. In other words, when
     * a structure is started at this coordinate, the other end cannot exceed chunk bounds.
     */
    private static int cornerInsideChunkBounds(Random rand, int size) {
        final int min = size / 2;
        return rand.nextInt(16 - min) + min;
    }
}
