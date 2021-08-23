package personthecat.cavegenerator.model;

import net.minecraft.world.level.chunk.ProtoChunk;

/**
 * A DTO used for consolidating positional data when generating early features.
 */
public class PrimerData {
    public final ProtoChunk p;
    public final int chunkX, chunkZ;
    public final int absX, absZ;
    public final int centerX, centerZ;

    public PrimerData(final ProtoChunk p, final int chunkX, final int chunkZ) {
        this.p = p;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.absX = chunkX * 16;
        this.absZ = chunkZ * 16;
        this.centerX = this.absX + 8;
        this.centerZ = this.absZ + 8;
    }
}