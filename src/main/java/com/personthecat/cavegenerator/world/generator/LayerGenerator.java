package com.personthecat.cavegenerator.world.generator;

import com.personthecat.cavegenerator.data.LayerSettings;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkPrimer;

import java.util.Random;

public class LayerGenerator extends BasicGenerator {

    private final LayerSettings cfg;

    public LayerGenerator(LayerSettings cfg, World world) {
        super(cfg.conditions, world);
        this.cfg = cfg;
    }

    @Override
    protected void doGenerate(World world, Random rand, int destChunkX, int destChunkZ, int chunkX, int chunkZ, ChunkPrimer primer) {
        for (int x = 0; x < 16; x++) {
            final int actualX = x + (chunkX * 16);
            for (int z = 0; z < 16; z++) {
                final int actualZ = z + (chunkZ * 16);
                if (conditions.biomes.test(world.getBiome(new BlockPos(actualX, 0, actualZ)))) {
                    for (int y : conditions.getColumn(actualX, actualZ)) {
                        if (BLK_STONE.equals(primer.getBlockState(x, y, z))) {
                            primer.setBlockState(x, y, z, cfg.state);
                        }
                    }
                }
            }
        }
    }
}
