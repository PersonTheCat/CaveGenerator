package com.personthecat.cavegenerator.world.feature;

import com.personthecat.cavegenerator.data.StructureSettings;
import com.personthecat.cavegenerator.model.Range;
import lombok.extern.log4j.Log4j2;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.template.Template;

import java.util.Optional;
import java.util.Random;

import static com.personthecat.cavegenerator.util.CommonMethods.empty;
import static com.personthecat.cavegenerator.util.CommonMethods.full;

@Log4j2
public class StructureGenerator extends FeatureGenerator {

    private final StructureSettings cfg;
    private final Template structure;

    public StructureGenerator(StructureSettings cfg, World world) {
        super(cfg.conditions, world);
        this.cfg = cfg;
        this.structure = StructureSpawner.getTemplate(cfg.name, world);
    }

    @Override
    protected void doGenerate(FeatureInfo info) {
        for (int i = 0; i < cfg.count; i++) {
            if (info.rand.nextDouble() <= cfg.chance) {
                generateSingle(info);
            }
        }
    }

    private void generateSingle(FeatureInfo info) {
        // Attempt to locate a suitable spawn position and then proceed.
        getSpawnPos(info).ifPresent(pos -> {
            if (allChecksPass(pos, info.world)) {
                preStructureSpawn(info, pos);
                final BlockPos adjusted = offset(centerBySize(pos, structure.getSize()), cfg.offset);
                StructureSpawner.spawnStructure(structure, cfg.placement, info.world, adjusted);
            }
        });
    }

    private boolean allChecksPass(BlockPos pos, World world) {
        return checkSources(cfg.matchers, world, pos) &&
            checkNonSolid(cfg.nonSolidChecks, world, pos) &&
            checkSolid(cfg.solidChecks, world, pos) &&
            checkAir(cfg.airChecks, world, pos) &&
            checkWater(cfg.waterChecks, world, pos);
    }

    /** Attempts to determine a suitable spawn point in the current location. */
    private Optional<BlockPos> getSpawnPos(FeatureInfo info) {
        // Favor vertical spawns, detecting horizontal surfaces first.
        if (cfg.directions.up || cfg.directions.down) {
            final Optional<BlockPos> vertical = getSpawnPosVertical(info, structure);
            if (vertical.isPresent()) {
                return vertical;
            } // else, try horizontal
        }
        // Attempt to locate any vertical surfaces, if necessary.
        if (cfg.directions.side) {
            return getSpawnPosHorizontal(info, structure);
        }
        return empty();
    }

    /** Attempts to find a spawn point for this structure on the vertical axis. */
    private Optional<BlockPos> getSpawnPosVertical(FeatureInfo info,  Template structure) {
        for (int i = 0; i < VERTICAL_RETRIES; i++) {
            // Start with random (x, z) coordinates.
            final BlockPos xz = randCoords(info.rand, structure.getSize(), info.offsetX, info.offsetZ);
            final int x = xz.getX();
            final int z = xz.getZ();
            final Range height = conditions.getColumn(x, z);
            final int maxY = Math.min(info.heightmap[x & 15][z & 15], height.max);
            final int minY = height.min;
            if (minY >= maxY) continue;

            final int y;
            // Search both -> just up -> just down.
            if (cfg.directions.up && cfg.directions.down) {
                y = findOpeningVertical(info.rand, info.world, x, z, minY, maxY);
            } else if (cfg.directions.up) {
                y = randFindCeiling(info.world, info.rand, x, z, minY, maxY);
            } else {
                y = randFindFloor(info.world, info.rand, x, z, minY, maxY);
            }
            // Check to see if an opening was found, else retry;
            if (y != NONE_FOUND) {
                return full(new BlockPos(x, y, z));
            }
        }
        return empty();
    }

    /** Attempts to find a spawn point for this structure on the horizontal axes. */
    private Optional<BlockPos> getSpawnPosHorizontal(FeatureInfo info, Template structure) {
        final BlockPos size = structure.getSize();
        final Range height = conditions.getColumn(info.offsetX, info.offsetZ);
        for (int i = 0; i < HORIZONTAL_RETRIES; i++) {
            final int y = height.rand(info.rand);
            Optional<BlockPos> pos;
            if (info.rand.nextBoolean()) {
                pos = randCoordsNS(info, size.getX(), y);
            } else {
                pos = randCoordsEW(info, size.getZ(), y);
            }
            if (pos.isPresent()) {
                return pos;
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
    private BlockPos randCoords(Random rand, BlockPos size, int offsetX, int offsetZ) {
        final int x = randCoord(rand, size.getX(), offsetX);
        final int z = randCoord(rand, size.getZ(), offsetZ);
        return new BlockPos(x, 0, z);
    }

    /**
     * Attempts to find a random surface on the east-west axis by scaling north-south in a random direction.
     * Returns Optional#empty if no surface is found, or if the surface found is not below the terrain height.
     */
    private Optional<BlockPos> randCoordsNS(FeatureInfo info, int sizeX, int y) {
        final int x = cornerInsideChunkBounds(info.rand, sizeX) + info.offsetX;
        final int z = info.rand.nextBoolean()
            ? findOpeningNorth(info.world, x, y, info.offsetZ)
            : findOpeningSouth(info.world, x, y, info.offsetZ);
        if (z != NONE_FOUND && y < info.heightmap[x & 15][z & 15]) {
            return full(new BlockPos(x, y, z));
        }
        return empty();
    }

    /**
     * Attempts to find a random surface on the north-south axis by scaling east-west in a random direction.
     * Returns Optional#empty if no surface is found, or if the surface found is not below the terrain height.
     */
    private Optional<BlockPos> randCoordsEW(FeatureInfo info, int sizeZ, int y) {
        final int z = cornerInsideChunkBounds(info.rand, sizeZ) + info.offsetZ;
        final int x = info.rand.nextBoolean()
            ? findOpeningEast(info.world, y, z, info.offsetX)
            : findOpeningWest(info.world, y, z, info.offsetX);
        if (x != NONE_FOUND && y < info.heightmap[x & 15][z & 15]) {
            return full(new BlockPos(x, y, z));
        }
        return empty();
    }

    /** Moves each dimension by half of `size` in the opposite direction. */
    private static BlockPos centerBySize(BlockPos toCenter, BlockPos size) {
        final int xOffset = (size.getX() / 2) * -1;
        final int zOffset = (size.getZ() / 2) * -1;
        return toCenter.add(xOffset, 0, zOffset);
    }

    /** Applies an offset to the original BlockPos. */
    private static BlockPos offset(BlockPos original, BlockPos offset) {
        return original.add(offset.getX(), offset.getY(), offset.getZ());
    }

    /** All operations related to structures before spawning should be organized herein. */
    private void preStructureSpawn(FeatureInfo info, BlockPos pos) {
        if (cfg.rotateRandomly) {
            final Rotation randRotation = Rotation.values()[info.rand.nextInt(3)];
            cfg.placement.setRotation(randRotation);
        }
        if (cfg.debugSpawns) {
            log.info("Spawning {} at {}", cfg.name, pos);
        }
    }

    /**
     * Returns a random relative coordinate such that result + size <= 16. In other words, when
     * a structure is started at this coordinate, the other end cannot exceed chunk bounds.
     */
    private static int cornerInsideChunkBounds(Random rand, int size) {
        return rand.nextInt(16 - size) + (size / 2);
    }
}
