package personthecat.cavegenerator.world.generator;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.carver.WorldCarver;
import personthecat.cavegenerator.world.BiomeSearch;
import personthecat.cavegenerator.world.config.ConditionConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * To facilitate porting CG to 1.16, I have chosen to continue using the
 * original basic generator model from CG 1.12 instead of migrating to the
 * modern {@link WorldCarver}.
 * <p>
 *   In the following updates, it may be preferable to make this migration,
 *   which could potentially improve compatibility with other mods.
 * </p>
 */
public abstract class EarlyGenerator {

    protected final List<BlockPos> invalidChunks = new ArrayList<>(BiomeSearch.size());
    protected final ConditionConfig conditions;
    protected final Random globalRand;
    protected final long seed;

    public EarlyGenerator(final ConditionConfig conditions, final Random rand, final long seed) {
        this.conditions = conditions;
        this.globalRand = rand;
        this.seed = seed;
    }

    /**
     * Generates this feature <em>after</em> checking to ensure that it can spawn in the
     * current dimension and biomes.
     *
     * @param ctx A context containing world information and coordinates.
     */
    public void generate(final PrimerContext ctx) {
        if (this.conditions.dimensions.test(ctx.primer)) {
            if (this.conditions.hasBiomes || this.conditions.hasRegion) {
                if (ctx.search.anyMatches(this.conditions.biomes)) {
                    this.fillInvalidChunks(ctx.search, ctx.chunkX, ctx.chunkZ);
                    this.generateChecked(ctx);
                    this.invalidChunks.clear();
                }
            } else {
                this.generateChecked(ctx);
            }
        }
    }

    /**
     * Checks the biome and noise conditions in each surrounding chunk for this generator.
     *
     * <p>Any chunks that do not pass will be added into the {@link #invalidChunks} context and
     * used to form a distance-based, cylindrical chunk border in the future.
     *
     * @param search The lazily initialized biome search utility providing biome data.
     */
    protected void fillInvalidChunks(final BiomeSearch search, final int x, final int z) {
        for (final BiomeSearch.Data d : search.surrounding.get()) {
            if (!(this.conditions.biomes.test(d.biome) && this.conditions.region.getBoolean(d.centerX, d.centerZ))) {
                this.invalidChunks.add(new BlockPos(d.centerX, 0, d.centerZ));
            }
        }
    }

    /**
     * The primary function which much be implemented in order to make changes to the
     * world.
     *
     * @param ctx A context containing world information and coordinates.
     */
    protected abstract void generateChecked(final PrimerContext ctx);
}
