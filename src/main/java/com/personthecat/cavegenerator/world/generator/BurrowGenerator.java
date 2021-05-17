package com.personthecat.cavegenerator.world.generator;

import com.personthecat.cavegenerator.config.ConfigFile;
import com.personthecat.cavegenerator.data.BurrowSettings;
import com.personthecat.cavegenerator.model.Range;
import com.personthecat.cavegenerator.util.PositionFlags;
import com.personthecat.cavegenerator.world.BiomeSearch;
import fastnoise.FastNoise;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkPrimer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BurrowGenerator extends WorldCarver implements TunnelSocket {

    private final List<BlockPos> invalidChunks = new ArrayList<>(BiomeSearch.size());
    private final FastNoise map;
    private final FastNoise offset;
    protected final BurrowSettings cfg;
    private final PositionFlags caverns;
    private final double mid;
    private final double midShelled;
    private final double radiusShelled;

    public BurrowGenerator(BurrowSettings cfg, World world) {
        super(cfg.conditions, cfg.decorators, world);
        this.map = cfg.map.getGenerator(world);
        this.offset = cfg.offset.getGenerator(world);
        this.cfg = cfg;
        this.caverns = new PositionFlags((int) (16 * 16 * (cfg.radius + cfg.decorators.shell.radius) * cfg.stretch * 2) + 1);
        this.mid = cfg.radius / Math.pow(cfg.target, cfg.exponent);
        this.radiusShelled = cfg.radius + cfg.decorators.shell.radius;

        final double ratio = (this.radiusShelled / cfg.radius);
        this.midShelled = this.radiusShelled / Math.pow(cfg.target * ratio, cfg.exponent);
    }

    @Override
    public void generate(PrimerContext ctx) {
        if (this.conditions.dimensions.test(ctx.world.provider.getDimension())) {
            if (this.conditions.hasBiomes) {
                if (ctx.biomes.anyMatches(this.conditions.biomes)) {
                    this.fillInvalidChunks(ctx.biomes);
                    this.generateChecked(ctx);
                    this.invalidChunks.clear();
                }
            } else if (this.conditions.hasRegion) {
                if (this.conditions.region.GetBoolean(ctx.offsetX, ctx.offsetZ)) {
                    this.fillInvalidChunks(ctx.chunkX, ctx.chunkZ);
                    this.generateChecked(ctx);
                    this.invalidChunks.clear();
                }
            } else {
                this.generateChecked(ctx);
            }
            this.caverns.reset();
        }
    }

    private void fillInvalidChunks(BiomeSearch biomes) {
        for (BiomeSearch.Data d : biomes.surrounding.get()) {
            if (!(this.conditions.biomes.test(d.biome) && this.conditions.region.GetBoolean(d.centerX, d.centerZ))) {
                this.invalidChunks.add(new BlockPos(d.centerX, 0, d.centerZ));
            }
        }
    }

    private void fillInvalidChunks(int chunkX, int chunkZ) {
        final int range = ConfigFile.biomeRange;
        for (int cX = chunkX - range; cX <= chunkX + range; cX++) {
            for (int cZ = chunkZ - range; cZ < chunkZ + range; cZ++) {
                final int centerX = cX * 16 + 8;
                final int centerZ = cZ * 16 + 8;
                if (!this.conditions.region.GetBoolean(centerX, centerZ)) {
                    this.invalidChunks.add(new BlockPos(centerX, 0, centerZ));
                }
            }
        }
    }

    @Override
    protected void generateChecked(PrimerContext ctx) {
        final boolean storePositions = this.hasWallDecorators() || this.hasPonds();
        // Optimize for several different use cases.
        if (this.hasShell()) {
            this.generateShelled(ctx, storePositions ? this::replaceRecord : this::replaceOnly);
        } else {
            this.generateUnShelled(ctx, storePositions ? this::replaceRecord : this::replaceOnly);
        }
        this.decorateAll(this.caverns, ctx.localRand, ctx.world, ctx.primer, ctx.chunkX, ctx.chunkZ);
    }

    private void generateShelled(PrimerContext ctx, GenerationFunction f) {
        for (int x = 0; x < 16; x++) {
            final int actualX = ctx.chunkX * 16 + x;
            for (int z = 0; z < 16; z++) {
                final int actualZ = ctx.chunkZ * 16 + z;
                final double distance = this.getNearestBorder(actualX, actualZ);
                final double value = this.map.GetNoise(actualX, actualZ);
                final double shifted = this.cfg.shift + value;
                final int cap = (int) (this.cfg.stretch * (this.cfg.radius - (Math.pow(shifted, this.cfg.exponent) * this.mid)));
                final int shell = (int) (this.cfg.stretch * (this.radiusShelled - (Math.pow(shifted, this.cfg.exponent) * this.midShelled)));

                if (cap > 0) {
                    final int centerY = (int) this.offset.GetAdjustedNoise(actualX, actualZ);
                    final Range height = this.conditions.getColumn(ctx.heightmap, actualX, actualZ);
                    final int min = Math.max(centerY - cap, height.min);
                    final int max = Math.min(centerY + cap, height.max);
                    final int minShell = Math.max(0, Math.max(centerY - shell, height.min));
                    final int maxShell = Math.min(255, Math.min(centerY + shell, height.max));

                    this.coverOuter(ctx, distance, x, z, minShell, min, centerY);
                    this.coverOuter(ctx, distance, x, z, max, maxShell, centerY);
                    for (int y = min; y < max + 1; y++) {
                        final double curve = distance - this.getBiomeCurve(centerY - y);
                        if (curve > this.cfg.wallDistance) {
                            f.generate(ctx, x, y, z);
                        }
                        else if (curve > this.cfg.wallDistance - this.decorators.shell.radius) {
                            this.generateShell(ctx.localRand, ctx.primer, x, y, z, centerY, ctx.chunkX, ctx.chunkZ);
                        }
                    }
                } else if (shell > 0) {
                    final int centerY = (int) this.offset.GetAdjustedNoise(actualX, actualZ);
                    final Range height = this.conditions.getColumn(ctx.heightmap, actualX, actualZ);
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
                this.generateShell(ctx.localRand, ctx.primer, x, y, z, centerY, ctx.chunkX, ctx.chunkZ);
            }
        }
    }

    private void generateUnShelled(PrimerContext ctx, GenerationFunction f) {
        for (int x = 0; x < 16; x++) {
            final int actualX = ctx.chunkX * 16 + x;
            for (int z = 0; z < 16; z++) {
                final int actualZ = ctx.chunkZ * 16 + z;
                final double distance = this.getNearestBorder(actualX, actualZ);
                final double value = this.map.GetNoise(actualX, actualZ);
                final double shifted = this.cfg.shift + value;
                final int cap = (int) (this.cfg.stretch * (this.cfg.radius - (Math.pow(shifted, this.cfg.exponent) * this.mid)));

                if (cap > 0) {
                    final int centerY = (int) this.offset.GetAdjustedNoise(actualX, actualZ);
                    final Range height = this.conditions.getColumn(ctx.heightmap, actualX, actualZ);
                    final int min = Math.max(centerY - cap, height.min);
                    final int max = Math.min(centerY + cap, height.max);

                    for (int y = min; y < max + 1; y++) {
                        final double curve = distance - this.getBiomeCurve(centerY - y);
                        if (curve > this.cfg.wallDistance) {
                            f.generate(ctx, x, y, z);
                        }
                    }
                }
            }
        }
    }

    private double getBiomeCurve(double relY) {
        final double curve = (Math.pow(relY, this.cfg.wallExponent) / this.cfg.radius);
        return Math.min(this.cfg.radius * 2.0, curve);
    }

    private void replaceOnly(PrimerContext ctx, int x, int y, int z) {
        this.replaceBlock(ctx.localRand, ctx.primer, x, y, z, ctx.chunkX, ctx.chunkZ);
    }

    private void replaceRecord(PrimerContext ctx, int x, int y, int z) {
        this.replaceOnly(ctx, x, y, z);
        this.caverns.add(x, y, z);
    }

    private double getNearestBorder(int x, int z) {
        double shortestDistance = Double.MAX_VALUE;

        for (BlockPos pos : this.invalidChunks) {
            final double sum = Math.pow(x - pos.getX(), 2) + Math.pow(z - pos.getZ(), 2);
            final double distance = Math.sqrt(sum);
            shortestDistance = Math.min(distance, shortestDistance);
        }
        return shortestDistance;
    }

    private void decorateCaverns(Random rand, ChunkPrimer primer, int chunkX, int chunkZ) {
        this.caverns.forEach((x, y, z) -> this.decorateBlock(rand, primer, x, y, z, chunkX, chunkZ));
    }

    @Override
    public int getTunnelHeight(int[][] heightmap, Random rand, int x, int z, int chunkX, int chunkZ) {
        final double value = this.map.GetNoise(x, z);
        final double shifted = this.cfg.shift + value;
        final int cap = (int) (this.cfg.stretch * (this.cfg.radius - (Math.pow(shifted, this.cfg.exponent) * this.mid)));
        if (cap > 0) {
            final int y = (int) this.offset.GetAdjustedNoise(x, z);
            if (this.conditions.getColumn(x, z).contains(y)) {
                if (this.conditions.noise.GetBoolean(x, y, z)) {
                    return y;
                }
            }
        }
        return CANNOT_SPAWN;
    }

    /** Experimental interface to reduce shell generation performance cost. Still testing. */
    @FunctionalInterface
    private interface GenerationFunction {
        void generate(PrimerContext ctx, int x, int y, int z);
    }
}
