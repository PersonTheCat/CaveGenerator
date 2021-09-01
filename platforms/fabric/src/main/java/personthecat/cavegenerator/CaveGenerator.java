package personthecat.cavegenerator;

import lombok.extern.log4j.Log4j2;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModificationContext;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectionContext;
import net.fabricmc.fabric.api.biome.v1.ModificationPhase;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import personthecat.catlib.exception.MissingOverrideException;
import personthecat.cavegenerator.world.event.BiomeCleanupEvent;
import personthecat.cavegenerator.world.event.CaveModificationContext;
import personthecat.overwritevalidator.annotations.Inherit;
import personthecat.overwritevalidator.annotations.OverwriteClass;

import java.util.function.BiConsumer;

@Log4j2
@OverwriteClass
public class CaveGenerator implements ModInitializer {

    @Override
    public void onInitialize() {
        this.initCommon();

        ServerLifecycleEvents.SERVER_STARTING.register(this::serverStarting);
        ServerLifecycleEvents.SERVER_STOPPING.register(this::serverStopping);
        this.createBiomeEvent((s, m) -> BiomeCleanupEvent.onBiomeCleanup(new CaveModificationContext(s, m)));
    }

    @Inherit
    public void initCommon() {
        throw new MissingOverrideException();
    }

    @Inherit
    @SuppressWarnings("unused")
    public void serverStarting(final MinecraftServer server) {
        throw new MissingOverrideException();
    }

    @Inherit
    @SuppressWarnings("unused")
    public void serverStopping(final MinecraftServer server) {
        throw new MissingOverrideException();
    }

    @SuppressWarnings("deprecation")
    private void createBiomeEvent(final BiConsumer<BiomeSelectionContext, BiomeModificationContext> event) {
        BiomeModifications.create(new ResourceLocation("cavegenerator:biome_cleanup"))
            .add(ModificationPhase.REMOVALS, s -> true, event);
    }
}
