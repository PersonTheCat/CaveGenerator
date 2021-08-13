package personthecat.cavegenerator.world.generator;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.chunk.ProtoChunk;
import personthecat.cavegenerator.util.XoRoShiRo;
import personthecat.cavegenerator.world.BiomeSearch;

import java.util.Random;

public class PrimerContext {
    final BiomeManager provider;
    final BiomeSearch search;
    final Random localRand;
    final int chunkX;
    final int chunkZ;
    final int offsetX;
    final int offsetZ;
    final ProtoChunk primer;

    public PrimerContext(
        final BiomeManager provider,
        final BiomeSearch search,
        final long seed,
        final ProtoChunk primer
    ) {
        final ChunkPos pos = primer.getPos();
        this.provider = provider;
        this.search = search;
        this.chunkX = pos.x;
        this.chunkZ = pos.z;
        this.offsetX = chunkX << 4;
        this.offsetZ = chunkZ << 4;
        this.localRand = new XoRoShiRo(seed ^ chunkX ^ chunkZ);
        this.primer = primer;
    }
}
