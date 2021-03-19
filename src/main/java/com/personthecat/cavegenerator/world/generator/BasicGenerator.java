package com.personthecat.cavegenerator.world.generator;

import com.personthecat.cavegenerator.data.ConditionSettings;
import com.personthecat.cavegenerator.model.Conditions;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.ChunkPrimer;

import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.Random;

public abstract class BasicGenerator {

    protected static final IBlockState BLK_AIR = Blocks.AIR.getDefaultState();
    protected static final IBlockState BLK_STONE = Blocks.STONE.getDefaultState();
    protected static final IBlockState BLK_WATER = Blocks.WATER.getDefaultState();

    protected final Conditions conditions;
    protected final WeakReference<World> world;

    public BasicGenerator(ConditionSettings conditions, World world) {
        Objects.requireNonNull(world, "Nullable world types are not yet supported.");
        this.conditions = Conditions.compile(conditions, world);
        this.world = new WeakReference<>(world);
    }

    protected final World getWorld() {
        return Objects.requireNonNull(world.get(), "World reference has been culled.");
    }

    public final void generate(World world, Random rand, int destChunkX, int destChunkZ, int chunkX, int chunkZ, ChunkPrimer primer) {
        final int dim = world.provider.getDimension();
        final Biome b = world.getBiome(new BlockPos(chunkX * 16, 0, chunkZ * 16));

        if (conditions.dimensions.test(dim) && conditions.biomes.test(b)) {
            doGenerate(world, rand, destChunkX, destChunkZ, chunkX, chunkZ, primer);
        }
    }

    protected abstract void doGenerate(World world, Random rand, int destChunkX, int destChunkZ, int chunkX, int chunkZ, ChunkPrimer primer);

}
