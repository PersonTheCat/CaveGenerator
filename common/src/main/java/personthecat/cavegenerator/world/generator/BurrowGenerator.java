package personthecat.cavegenerator.world.generator;

import net.minecraft.core.BlockPos;
import personthecat.catlib.data.Range;
import personthecat.cavegenerator.model.PositionFlags;
import personthecat.cavegenerator.world.config.BurrowConfig;
import personthecat.fastnoise.FastNoise;
import java.util.Random;

public class BurrowGenerator extends CaveCarver implements TunnelSocket {

    private final FastNoise map;
    private final FastNoise offset;
    protected final BurrowConfig cfg;
    private final PositionFlags caverns;
    private final double mid;
    private final double midShelled;
    private final double radiusShelled;

    public BurrowGenerator(final BurrowConfig cfg, final Random rand, final long seed) {
        super(cfg.conditions, cfg.decorators, rand, seed);
        this.map = cfg.map;
        this.offset = cfg.offset;
        this.cfg = cfg;
        this.caverns = new PositionFlags((int) (16 * 16 * (cfg.radius + cfg.decorators.shell.radius) * cfg.stretch * 2) + 1);
        this.mid = cfg.radius / Math.pow(cfg.target, cfg.exponent);
        this.radiusShelled = cfg.radius + cfg.decorators.shell.radius;

        final double ratio = (this.radiusShelled / cfg.radius);
        this.midShelled = this.radiusShelled / Math.pow(cfg.target * ratio, cfg.exponent);
    }

    @Override
    protected void generateChecked(final PrimerContext ctx) {
        if (this.hasShell()) {
            this.generateShelled(ctx);
        } else {
            this.generateUnShelled(ctx);
        }
        this.decorateAll(ctx, this.caverns, ctx.localRand);
        this.caverns.reset();
    }

    private void generateShelled(final PrimerContext ctx) {
        for (int x = 0; x < 16; x++) {
            final int aX = ctx.actualX + x;
            for (int z = 0; z < 16; z++) {
                final int aZ = ctx.actualZ + z;
                final double distance = this.getNearestBorder(aX, aZ);
                final double value = this.map.getNoise(aX, aZ);
                final double shifted = this.cfg.shift + value;
                final int cap = (int) (this.cfg.stretch * (this.cfg.radius - (Math.pow(shifted, this.cfg.exponent) * this.mid)));
                final int shell = (int) (this.cfg.stretch * (this.radiusShelled - (Math.pow(shifted, this.cfg.exponent) * this.midShelled)));

                if (cap > 0) {
                    final int centerY = (int) this.offset.getNoiseScaled(aX, aZ);
                    final Range height = this.conditions.getColumn(ctx, aX, aZ);
                    final int min = Math.max(centerY - cap, height.min);
                    final int max = Math.min(centerY + cap, height.max);
                    final int minShell = Math.max(0, Math.max(centerY - shell, height.min));
                    final int maxShell = Math.min(255, Math.min(centerY + shell, height.max));

                    this.coverOuter(ctx, distance, x, z, minShell, min, centerY);
                    this.coverOuter(ctx, distance, x, z, max, maxShell, centerY);
                    for (int y = min; y < max + 1; y++) {
                        final double curve = distance - this.getBiomeCurve(centerY - y);
                        if (curve > this.cfg.wallDistance) {
                            if (this.replaceBlock(ctx, ctx.localRand, x, y, z)) {
                                this.caverns.add(x, y, z);
                            }
                        }
                        else if (curve > this.cfg.wallDistance - this.decorators.shell.radius) {
                            this.generateShell(ctx, ctx.localRand, x, y, z, centerY);
                        }
                    }
                } else if (shell > 0) {
                    final int centerY = (int) this.offset.getNoiseScaled(aX, aZ);
                    final Range height = this.conditions.getColumn(ctx, aX, aZ);
                    final int minShell = Math.max(0, Math.max(centerY - shell, height.min));
                    final int maxShell = Math.min(255, Math.min(centerY + shell, height.max));
                    this.coverOuter(ctx, distance, x, z, minShell, maxShell, centerY);
                }
            }
        }
    }

    private void coverOuter(PrimerContext ctx, double distance, int x, int z, int min, int max, int centerY) {
        for (int y = min; y < max + 1; y++) {
            final double curve = distance - this.getBiomeCurve(centerY - y);
            if (curve > this.cfg.wallDistance - this.decorators.shell.radius) {
                this.generateShell(ctx, ctx.localRand, x, y, z, centerY);
            }
        }
    }

    private void generateUnShelled(PrimerContext ctx) {
        for (int x = 0; x < 16; x++) {
            final int aX = ctx.actualX + x;
            for (int z = 0; z < 16; z++) {
                final int aZ = ctx.actualZ + z;
                final double distance = this.getNearestBorder(aX, aZ);
                final double value = this.map.getNoise(aX, aZ);
                final double shifted = this.cfg.shift + value;
                final int cap = (int) (this.cfg.stretch * (this.cfg.radius - (Math.pow(shifted, this.cfg.exponent) * this.mid)));

                if (cap > 0) {
                    final int centerY = (int) this.offset.getNoiseScaled(aX, aZ);
                    final Range height = this.conditions.getColumn(ctx, aX, aZ);
                    final int min = Math.max(centerY - cap, height.min);
                    final int max = Math.min(centerY + cap, height.max);

                    for (int y = min; y < max + 1; y++) {
                        final double curve = distance - this.getBiomeCurve(centerY - y);
                        if (curve > this.cfg.wallDistance) {
                            if (this.replaceBlock(ctx, ctx.localRand, x, y, z)) {
                                this.caverns.add(x, y, z);
                            }
                        }
                    }
                }
            }
        }
    }

    private double getBiomeCurve(final double relY) {
        final double curve = (Math.pow(relY, this.cfg.wallExponent) / this.cfg.radius);
        return Math.min(this.cfg.radius * 2.0, curve);
    }

    private double getNearestBorder(final int x, final int z) {
        double shortestDistance = Double.MAX_VALUE;

        for (BlockPos pos : this.invalidChunks) {
            final double sum = Math.pow(x - pos.getX(), 2) + Math.pow(z - pos.getZ(), 2);
            final double distance = Math.sqrt(sum);
            shortestDistance = Math.min(distance, shortestDistance);
        }
        return shortestDistance;
    }

    @Override
    public int getTunnelHeight(Random rand, int x, int z, int chunkX, int chunkZ) {
        final double value = this.map.getNoise(x, z);
        final double shifted = this.cfg.shift + value;
        final int cap = (int) (this.cfg.stretch * (this.cfg.radius - (Math.pow(shifted, this.cfg.exponent) * this.mid)));
        if (cap > 0) {
            final int y = (int) this.offset.getNoiseScaled(x, z);
            if (this.conditions.getColumn(x, z).contains(y)) {
                if (this.conditions.noise.getBoolean(x, y, z)) {
                    return y;
                }
            }
        }
        return CANNOT_SPAWN;
    }
}
