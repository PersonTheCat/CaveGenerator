package personthecat.cavegenerator.world.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.StairsShape;
import personthecat.catlib.data.Range;
import personthecat.cavegenerator.world.config.PillarConfig;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Random;

@ParametersAreNonnullByDefault
public class PillarGenerator extends BasicFeature {

    private final PillarConfig cfg;

    public PillarGenerator(final PillarConfig cfg, final Random rand, final long seed) {
        super(cfg.conditions, rand, seed);
        this.cfg = cfg;
    }

    @Override
    protected void doGenerate(final WorldContext ctx) {
        final Random rand = ctx.rand;
        for (int i = 0; i < rand.nextInt(cfg.count + 1); i++) {
            // Avoid pillars spawning right next to each other.
            final int x = ((rand.nextInt(6) * 2) + 2) + (ctx.chunkX * 16); // 2 to 14
            final int z = ((rand.nextInt(6) * 2) + 1) + (ctx.chunkZ * 16); // 1 to 13
            final Biome biome = ctx.region.getBiome(new BlockPos(x, 0, z));

            if (conditions.biomes.test(biome)) {
                final Range height = conditions.getColumn(x, z);

                if (!height.isEmpty() && conditions.region.getBoolean(x, z)) {
                    final int y = height.rand(rand);
                    final int opening = findCeiling(ctx.region, x, y, z, height.max);
                    if (opening != NONE_FOUND && conditions.noise.getBoolean(x, opening, z)) {
                        this.generateSingle(ctx, rand, new BlockPos(x, opening, z));
                    }
                }
            }
        }
    }

    /** @param pos is the top block in the pillar. */
    private void generateSingle(final WorldContext ctx, final Random rand, final BlockPos pos) {
        final int actualMax = pos.getY();
        final int actualMin = getLowestBlock(ctx, pos);
        // Verify that the position is possible.
        if (actualMin < 0) return;

        int length = actualMax - actualMin;
        // Ensure that the difference is within the specified bounds.
        if (length < cfg.length.min || length > cfg.length.max) return;

        for (int y = actualMax; y >= actualMin; y--) {
            final BlockPos current = new BlockPos(pos.getX(), y, pos.getZ());
            // Start by placing the initial block.
            ctx.region.setBlock(current, cfg.state, 2);

            // Handle stair blocks, if applicable.
            if (cfg.stairBlock != null) {
                if (y == actualMax) { // We're at the top. Place stairs upward.
                    this.testPlaceStairs(ctx, cfg.stairBlock, rand, pos, Half.TOP);
                } else if (y == actualMin) { // We're at the bottom. Place stairs downward.
                    this.testPlaceStairs(ctx, cfg.stairBlock, rand, current, Half.BOTTOM);
                }
            }
        }
    }

    /** Determines the lowest block at the given X, Z coordinates. */
    private int getLowestBlock(final WorldContext ctx, BlockPos pos) {
        for (pos = pos.below(); pos.getY() > cfg.conditions.height.min; pos = pos.below()) {
            if (ctx.region.getBlockState(pos).getMaterial().isSolid()) {
                // We're going down. There was air above us, but not at this position.
                // This is the bottom.
                return pos.getY();
            }
        }
        // Nothing was found. Just return -1 instead of boxing it in some container.
        return -1;
    }

    /** Tries to randomly place stair blocks around the pillar in all 4 directions. */
    private void testPlaceStairs(WorldContext ctx, StairBlock stairs, Random rand, BlockPos pos, Half topOrBottom) {
        if (topOrBottom == Half.TOP) {
            this.testPlaceUp(ctx, stairs, pos.north(), Direction.SOUTH, rand, topOrBottom);
            this.testPlaceUp(ctx, stairs, pos.south(), Direction.NORTH, rand, topOrBottom);
            this.testPlaceUp(ctx, stairs, pos.east(), Direction.WEST, rand, topOrBottom);
            this.testPlaceUp(ctx, stairs, pos.west(), Direction.EAST, rand, topOrBottom);
        } else {
            this.testPlaceDown(ctx, stairs, pos.north(), Direction.SOUTH, rand, topOrBottom);
            this.testPlaceDown(ctx, stairs, pos.south(), Direction.NORTH, rand, topOrBottom);
            this.testPlaceDown(ctx, stairs, pos.east(), Direction.WEST, rand, topOrBottom);
            this.testPlaceDown(ctx, stairs, pos.west(), Direction.EAST, rand, topOrBottom);
        }
    }

    /**
     * Randomly looks +-3 blocks vertically surrounding the given X, Z
     * coordinates. Places stairs given a ~1/3 chance.
     */
    private void testPlaceDown(WorldContext ctx, StairBlock stairs, BlockPos pos, Direction facing, Random rand, Half topOrBottom) {
        BlockPos previous = pos.below(4);
        // Iterate 3 blocks down, 3 blocks up.
        for (int i = - 3; i <= 3; i++) {
            final BlockPos current = pos.above(i);
            // Verify that we're within the height bounds. Stop randomly.
            if (current.getY() >= cfg.conditions.height.min && rand.nextInt(2) == 0) {
                // Find a boundary between solid and air.
                if (ctx.region.getBlockState(previous).getMaterial().isSolid() && ctx.region.getBlockState(current).isAir()) {
                    // Replace air.
                    ctx.region.setBlock(current, getStairRotation(stairs, facing, topOrBottom), 16);
                    return;
                }
            }
            previous = current;
        }
    }

    private void testPlaceUp(WorldContext ctx, StairBlock stairs, BlockPos pos, Direction facing, Random rand, Half topOrBottom) {
        BlockPos previous = pos.above(4);
        for (int i =  3; i >= - 3; i--) {
            final BlockPos current = pos.above(i);
            if (current.getY() >= cfg.conditions.height.min && rand.nextInt(2) == 0) {
                // pos.up will add or subtract and is thus still valid.
                if (ctx.region.getBlockState(previous).getMaterial().isSolid() && ctx.region.getBlockState(current).isAir()) {
                    ctx.region.setBlock(current, getStairRotation(stairs, facing, topOrBottom), 16);
                    return;
                }
            }
            previous = current;
        }
    }

    private BlockState getStairRotation(StairBlock stairs, Direction facing, Half topOrBottom) {
        // The null check is handled above.
        return stairs.defaultBlockState()
            .setValue(StairBlock.FACING, facing)
            .setValue(StairBlock.HALF, topOrBottom)
            .setValue(StairBlock.SHAPE, StairsShape.STRAIGHT);
    }
}