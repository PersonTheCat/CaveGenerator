package personthecat.cavegenerator.world.event;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class BiomeCleanupEvent {

    public static void onBiomeCleanup(final CaveModificationContext ctx) {
        log.info("Running biome cleanup event on {}", ctx.getBiomeName());
    }
}
