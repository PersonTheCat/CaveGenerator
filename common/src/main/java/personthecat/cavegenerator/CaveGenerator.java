package personthecat.cavegenerator;

import lombok.extern.log4j.Log4j2;
import net.minecraft.server.MinecraftServer;
import personthecat.catlib.command.LibCommandRegistrar;
import personthecat.catlib.event.world.FeatureModificationEvent;
import personthecat.cavegenerator.commands.CommandCave;
import personthecat.cavegenerator.config.Cfg;
import personthecat.cavegenerator.io.JarFiles;
import personthecat.cavegenerator.noise.CachedNoiseHelper;
import personthecat.cavegenerator.util.Reference;
import personthecat.cavegenerator.world.event.CaveCleanupEvent;
import personthecat.cavegenerator.world.feature.FallbackFeatureHook;
import personthecat.cavegenerator.world.generator.FallbackCarverHook;
import personthecat.overwritevalidator.annotations.OverwriteTarget;
import personthecat.overwritevalidator.annotations.PlatformMustInherit;

@Log4j2
@OverwriteTarget(required = true)
public class CaveGenerator {

    @PlatformMustInherit
    public void initCommon() {
        JarFiles.copyFiles();
        Cfg.register();
        LibCommandRegistrar.registerCommands(Reference.MOD, true, CommandCave.class);

        if (Cfg.FALLBACK_FEATURES.getAsBoolean()) {
            FallbackFeatureHook.register();
        }
        if (Cfg.FALLBACK_CARVERS.getAsBoolean()) {
            FallbackCarverHook.register();
        }
        FeatureModificationEvent.EVENT.register(CaveCleanupEvent::onBiomeCleanup);
    }

    @PlatformMustInherit
    public void serverStarting(final MinecraftServer server) {
        log.info("Loading cave generators");
        CaveRegistries.loadAll();
        CaveRegistries.COMMAND_SOURCE.create(server);
    }

    @PlatformMustInherit
    @SuppressWarnings("unused")
    public void serverStopping(final MinecraftServer server) {
        log.info("Unloading cave generators.");
        CaveRegistries.resetAll();
        CachedNoiseHelper.removeAll();
        CaveRegistries.COMMAND_SOURCE.clear();
    }
}
