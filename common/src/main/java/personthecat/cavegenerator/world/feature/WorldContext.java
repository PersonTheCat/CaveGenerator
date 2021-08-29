package personthecat.cavegenerator.world.feature;

import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.Level;
import personthecat.cavegenerator.util.XoRoShiRo;
import personthecat.cavegenerator.world.GeneratorController;

import java.util.Random;

public class WorldContext {

    public final Random rand;
    public final int chunkX;
    public final int chunkZ;
    public final int centerX;
    public final int centerZ;
    public final long seed;
    public final WorldGenRegion region;

    public WorldContext(final WorldGenRegion region) {
        this.rand = new XoRoShiRo(region.getSeed());
        this.centerX = region.getCenterX();
        this.centerZ = region.getCenterZ();
        this.chunkX = centerX >> 4;
        this.chunkZ = centerZ >> 4;
        this.seed = region.getSeed();
        this.region = region;
    }
}
