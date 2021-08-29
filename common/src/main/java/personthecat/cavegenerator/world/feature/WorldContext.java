package personthecat.cavegenerator.world.feature;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import lombok.extern.log4j.Log4j2;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.levelgen.Heightmap;
import personthecat.cavegenerator.CaveRegistries;

import java.util.Random;

@Log4j2
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
    public final ServerLevel level;
    public final CommandDispatcher<CommandSourceStack> dispatcher;

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
        this.level = region.getLevel();
        this.dispatcher = level.getServer().getCommands().getDispatcher();
    }

    public int getHeight(final int x, final int y) {
        return this.region.getHeight(Heightmap.Types.OCEAN_FLOOR, x, y);
    }

    public void execute(final String cmd) {
        final CommandSourceStack source = CaveRegistries.COMMAND_SOURCE.get();
        if (source == null) {
            log.error("No command source in mod context. Cannot run {}", cmd);
            return;
        }
        try {
            this.dispatcher.execute(cmd, source);
        } catch (final CommandSyntaxException e) {
            log.error("Running " + cmd, e);
        }
    }
}
