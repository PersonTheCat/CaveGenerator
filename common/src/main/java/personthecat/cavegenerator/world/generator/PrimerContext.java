package personthecat.cavegenerator.world.generator;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import personthecat.cavegenerator.mixin.PrimerAccessor;
import personthecat.cavegenerator.util.XoRoShiRo;
import personthecat.cavegenerator.world.BiomeSearch;

import java.util.*;

import static personthecat.cavegenerator.util.CommonBlocks.BLK_AIR;
import static personthecat.cavegenerator.util.CommonBlocks.BLK_VOID;

public final class PrimerContext {

    public final BiomeManager provider;
    public final BiomeSearch search;
    public final Random localRand;
    public final int chunkX;
    public final int chunkZ;
    public final int actualX;
    public final int actualZ;
    public final int centerX;
    public final int centerZ;
    public final int seaLevel;
    public final long seed;
    public final ProtoChunk primer;
    public final GenerationStep.Carving step;
    public final Map<Heightmap.Types, Heightmap> heightmaps;
    public final Heightmap oceanFloor;
    public final Map<GenerationStep.Carving, BitSet> carvingMasks;
    public final List<Heightmap> heightmapsAfter = new ArrayList<>();

    public PrimerContext(
        final BiomeManager provider,
        final BiomeSearch search,
        final long seed,
        final int seaLevel,
        final ProtoChunk primer,
        final GenerationStep.Carving step
    ) {
        final ChunkPos pos = primer.getPos();
        this.provider = provider;
        this.search = search;
        this.chunkX = pos.x;
        this.chunkZ = pos.z;
        this.actualX = chunkX << 4;
        this.actualZ = chunkZ << 4;
        this.centerX = actualX + 8;
        this.centerZ = actualZ + 8;
        this.localRand = new XoRoShiRo(seed ^ chunkX ^ chunkZ);
        this.seaLevel = seaLevel;
        this.seed = seed;
        this.primer = primer;
        this.step = step;
        this.heightmaps = ((PrimerAccessor) primer).heightmaps();
        this.oceanFloor = this.heightmaps.get(Heightmap.Types.OCEAN_FLOOR_WG);
        this.carvingMasks = ((PrimerAccessor) primer).carvingMasks();

        for (final Heightmap.Types type : primer.getStatus().heightmapsAfter()) {
            this.heightmapsAfter.add(this.heightmaps.get(type));
        }
    }

    /**
     * Reimplementation of {@link ProtoChunk#getBlockState} which avoids the unnecessary allocation
     * of additional {@link BlockPos} containers.
     *
     * @param x The x-coordinate of the expected block.
     * @param y The y-coordinate of the expected block.
     * @param z The z-coordinate of the expected block.
     * @return The block state located at this position.
     */
    public BlockState get(final int x, final int y, final int z) {
        if (Level.isOutsideBuildHeight(y)) {
            return BLK_VOID;
        }
        return getUnchecked(x, y, z);
    }

    /**
     * An unchecked variant of {@link #get(int, int, int)} which ignores the possibility
     * of <code>y</code> being out of bounds.
     *
     * @param x The x-coordinate of the expected block.
     * @param y The y-coordinate of the expected block.
     * @param z The z-coordinate of the expected block.
     * @return The block state located at this position.
     */
    public BlockState getUnchecked(final int x, final int y, final int z) {
        // Todo: investigate pre-calculating how many sections are needed.
        final LevelChunkSection section = this.primer.getSections()[y >> 4];
        if (LevelChunkSection.isEmpty(section)) {
            return BLK_AIR;
        }
        return section.getBlockState(x & 15, y & 15, z & 15);
    }

    /**
     * Reimplementation of {@link ProtoChunk#setBlockState} which avoids the unnecessary allocation
     * of additional {@link BlockPos} containers.
     *
     * @param x     The x-coordinate of the expected block.
     * @param y     The y-coordinate of the expected block.
     * @param z     The z-coordinate of the expected block.
     * @param state The block to be placed at the given coordinates.
     * @return The original block at this position.
     */
    public BlockState set(final int x, final int y, final int z, final BlockState state) {
        if (Level.isOutsideBuildHeight(y)) {
            return state;
        }
        final int i = y >> 4;
        LevelChunkSection section = this.primer.getSections()[i];
        if (section == LevelChunk.EMPTY_SECTION) {
            section = this.primer.getSections()[i] = new LevelChunkSection(i << 4);
        }
        return setUnchecked(section, x, y, z, state);
    }

    /**
     * An unchecked variant of {@link #set(int, int, int, BlockState)} which ignores the possibility
     * of <code>y</code> being out of bounds or in a not-yet-generated {@link LevelChunkSection}.
     *
     * @param x      The x-coordinate of the expected block.
     * @param y      The y-coordinate of the expected block.
     * @param z      The z-coordinate of the expected block.
     * @return The block state located at this position.
     */
    public BlockState setUnchecked(LevelChunkSection section, int x, int y, int z, BlockState state) {
        // This is "unchecked." Consider re/moving it.
        x &= 15;
        y &= 15;
        z &= 15;

        if (state.getLightEmission() > 0) {
            this.primer.addLight(new BlockPos(this.actualX + x, y, this.actualZ + z));
        }
        final BlockState original = section.setBlockState(x, y, z, state);
        for (final Heightmap map : this.heightmapsAfter) {
            map.update(x, y, z, state);
        }
        return original;
    }

    /**
     * Experimental method responsible for initializing the heightmaps that will be used
     * during the current generation stage. This allows us to reduce the number of redundant
     * checks that occur when updating blocks in the world.
     */
    public void primeHeightmaps() {
        final EnumSet<Heightmap.Types> maps = EnumSet.noneOf(Heightmap.Types.class);
        for (final Heightmap.Types type : this.primer.getStatus().heightmapsAfter()) {
            if (!this.heightmaps.containsKey(type)) {
                maps.add(type);
            }
        }
        Heightmap.primeHeightmaps(this.primer, maps);
    }

    /**
     * Experimental method responsible for creating the chunk sections that will be used by
     * the generator controller. This action relieves the responsibility of repeatedly checking
     * whether those sections exist in the future.
     *
     * <p>Note that an additional section may be created in each direction in case of block
     * updates.
     *
     * @param minY The lowest y-level affected by <b>any</b> controller.
     * @param maxY The highest y-level affected by <b>any</b> controller.
     */
    public void primeSections(final int minY, final int maxY) {
        final int min = Math.max(0, (minY - 1) >> 4);
        final int max = Math.min(15, (maxY + 1) >> 4);
        for (int i = min; i <= max; i++) {
            if (this.primer.getSections()[i] == null) {
                this.primer.getSections()[i] = new LevelChunkSection(i << 4);
            }
        }
    }

    /**
     * Direct variant of calling {@link ProtoChunk#getHeight}
     *
     * @param x The absolute or chunk x-coordinate.
     * @param z The absolute or chunk z-coordinate.
     * @return The height at these coordinates.
     */
    public int getHeight(final int x, final int z) {
        return this.oceanFloor.getFirstAvailable(x & 15, z & 15) - 1;
    }
}
