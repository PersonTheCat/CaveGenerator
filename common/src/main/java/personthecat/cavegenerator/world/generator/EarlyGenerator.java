package personthecat.cavegenerator.world.generator;

import net.minecraft.world.level.levelgen.carver.WorldCarver;
import personthecat.cavegenerator.world.config.ConditionConfig;

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
     * Todo: Also check region noise here?
     *
     * @param ctx A context containing world information and coordinates.
     */
    public void generate(final PrimerContext ctx) {
        // Todo: dimensions
        if (!this.conditions.hasBiomes || ctx.search.anyMatches(this.conditions.biomes)) {
            this.generateChecked(ctx);
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
