package personthecat.cavegenerator.world.feature;

import net.minecraft.world.level.Level;
import personthecat.cavegenerator.world.GeneratorController;

import java.util.Random;

public class WorldContext {
    final GeneratorController gen;
    final Random rand;
    final int chunkX;
    final int chunkZ;
    final int offsetX;
    final int offsetZ;
    final Level level;

    public WorldContext(
        final GeneratorController gen,
        final Random rand,
        final int chunkX,
        final int chunkZ,
        final Level level
    ) {
        this.gen = gen;
        this.rand = rand;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.offsetX = chunkX << 4 + 8;
        this.offsetZ = chunkZ << 4 + 8;
        this.level = level;
    }
}
