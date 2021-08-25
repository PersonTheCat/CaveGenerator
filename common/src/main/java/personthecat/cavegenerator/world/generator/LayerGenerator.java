package personthecat.cavegenerator.world.generator;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;
import personthecat.cavegenerator.presets.data.LayerSettings;

import java.util.Random;

import static personthecat.cavegenerator.util.CommonBlocks.BLK_STONE;

// Todo: this used to be optimized to avoid overlapping layers. Switch to ListGenerator.
public class LayerGenerator extends EarlyGenerator {

    private final LayerSettings cfg;

    public LayerGenerator(final LayerSettings cfg, final Random rand, final long seed) {
        super(cfg.conditions, rand, seed);
        this.cfg = cfg;
    }

    @Override
    protected void generateChecked(final PrimerContext ctx) {
        for (int x = 0; x < 16; x++) {
            final int aX = ctx.actualX + x;
            for (int z = 0; z < 16; z++) {
                final int aZ = ctx.actualZ + z;
                final Biome b = ctx.provider.getBiome(new BlockPos(aX, 0, aZ));
                if (conditions.biomes.test(b) && conditions.region.getBoolean(aX, aZ)) {
                    for (int y : conditions.getColumn(aX, aZ)) {
                        if (BLK_STONE.equals(ctx.get(x, y, z))) {
                            if (conditions.noise.getBoolean(x, z)) {
                                ctx.set(x, y, z, cfg.state);
                            }
                        }
                    }
                }
            }
        }
    }
}
