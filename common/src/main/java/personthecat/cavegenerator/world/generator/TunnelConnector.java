package personthecat.cavegenerator.world.generator;

import personthecat.cavegenerator.model.TunnelPathInfo;
import personthecat.cavegenerator.presets.data.TunnelSettings;
import personthecat.cavegenerator.util.XoRoShiRo;

import java.util.Random;

public class TunnelConnector<Socket extends TunnelSocket> extends TunnelGenerator {
    private final Socket parent;

    public TunnelConnector(final TunnelSettings tunnelCfg, final Socket parent, final Random rand, final long seed) {
        super(tunnelCfg, rand, seed);
        this.parent = parent;
    }

    @Override
    protected void mapGenerate(final PrimerContext ctx, final int destX, final int destZ) {
        this.bury(ctx, ctx.localRand.nextLong(), destX, destZ);
    }

    protected void bury(final PrimerContext ctx, final long seed, final int destX, final int destZ) {
        final Random rand = new XoRoShiRo(seed);
        final int frequency = this.getTunnelCount(rand);
        for (int i = 0; i < frequency; i++) {
            final int distance = cfg.distance;

            for (int j = 0; j < this.getBranchCount(rand); j++) {
                final TunnelPathInfo path = new TunnelPathInfo(cfg, rand, destX, destZ);
                final int y = this.parent.getTunnelHeight(rand, (int) path.getX(), (int) path.getZ(), ctx.chunkX, ctx.chunkZ);

                if (y != TunnelSocket.CANNOT_SPAWN) {
                    path.setY(y);
                    this.addTunnel(ctx, rand.nextLong(), path,0, distance);
                }
            }
        }
    }
}
