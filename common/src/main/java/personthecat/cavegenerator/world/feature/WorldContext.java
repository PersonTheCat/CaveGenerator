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
    public final int actualX;
    public final int actualZ;
    public final int centerX;
    public final int centerZ;
    public final long seed;
    public final WorldGenRegion region;

    public WorldContext(final WorldGenRegion region) {
        this.rand = region.getRandom();
        this.chunkX = region.getCenterX();
        this.chunkZ = region.getCenterZ();
        this.actualX = chunkX << 4;
        this.actualZ = chunkZ << 4;
        this.centerX = actualX + 8;
        this.centerZ = actualZ + 8;
        this.seed = region.getSeed();
        this.region = region;
    }
}
