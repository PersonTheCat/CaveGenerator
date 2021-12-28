package com.personthecat.cavegenerator.world.generator;

import com.personthecat.cavegenerator.data.LayerSettings;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.ChunkPrimer;

import java.util.Random;

public class LayerGenerator extends BasicGenerator {

    private final LayerSettings cfg;

    public LayerGenerator(LayerSettings cfg, World world) {
        super(cfg.conditions, world);
        this.cfg = cfg;
    }

    @Override
    public void generate(PrimerContext ctx) {
        final int dim = ctx.world.provider.getDimension();
        if (conditions.dimensions.test(dim)) {
            generateChecked(ctx);
        }
    }

    @Override
    protected void generateChecked(PrimerContext ctx) {
        for (int x = 0; x < 16; x++) {
            final int actualX = x + (ctx.chunkX * 16);
            for (int z = 0; z < 16; z++) {
                final int actualZ = z + (ctx.chunkZ * 16);
                final Biome b = ctx.world.getBiome(new BlockPos(actualX, 0, actualZ));
                if (conditions.biomes.test(b) && conditions.region.GetBoolean(actualX, actualZ)) {
                    for (int y : conditions.getColumn(actualX, actualZ)) {
                        if (cfg.matchers.contains(ctx.primer.getBlockState(x, y, z))) {
                            if (conditions.noise.GetBoolean(x, z)) {
                                ctx.primer.setBlockState(x, y, z, cfg.state);
                            }
                        }
                    }
                }
            }
        }
    }
}
