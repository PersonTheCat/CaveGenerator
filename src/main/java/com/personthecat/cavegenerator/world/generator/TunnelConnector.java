package com.personthecat.cavegenerator.world.generator;

import com.personthecat.cavegenerator.data.TunnelSettings;
import com.personthecat.cavegenerator.model.PrimerData;
import com.personthecat.cavegenerator.util.XoRoShiRo;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkPrimer;

import java.util.Random;

public class TunnelConnector<Socket extends TunnelSocket> extends TunnelGenerator {

    private final Socket parent;

    public TunnelConnector(TunnelSettings tunnelCfg, Socket parent, World world) {
        super(tunnelCfg, world);
        this.parent = parent;
    }

    @Override
    protected void mapGenerate(MapGenerationContext ctx) {
        this.bury(ctx.heightmap, ctx.world, ctx.rand.nextLong(), ctx.destChunkX, ctx.destChunkZ, ctx.chunkX, ctx.chunkZ, ctx.primer);
    }

    protected void bury(int[][] heightmap, World world, long seed, int destX, int destZ, int x, int z, ChunkPrimer primer) {
        final Random rand = new XoRoShiRo(seed);
        final int frequency = this.getTunnelCount(rand);
        for (int i = 0; i < frequency; i++) {
            final int distance = cfg.distance;
            final PrimerData data = new PrimerData(primer, x, z);

            for (int j = 0; j < this.getBranchCount(rand); j++) {
                final TunnelPathInfo path = new TunnelPathInfo(cfg, rand, destX, destZ);
                final int y = this.parent.getTunnelHeight(heightmap, rand, (int) path.getX(), (int) path.getZ(), x, z);

                if (y != TunnelSocket.CANNOT_SPAWN) {
                    path.setY(y);
                    this.addTunnel(world, rand.nextLong(), data, path,0, distance);
                }
            }
        }
    }
}
