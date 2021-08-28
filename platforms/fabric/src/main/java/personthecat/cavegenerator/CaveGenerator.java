package personthecat.cavegenerator;

import lombok.extern.log4j.Log4j2;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import personthecat.catlib.exception.MissingOverrideException;
import personthecat.overwritevalidator.annotations.Inherit;
import personthecat.overwritevalidator.annotations.OverwriteClass;

@Log4j2
@OverwriteClass
public class CaveGenerator implements ModInitializer {

    @Override
    public void onInitialize() {
        this.initCommon();

        ServerLifecycleEvents.SERVER_STARTING.register(this::serverStarting);
        ServerLifecycleEvents.SERVER_STOPPING.register(this::serverStopping);
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
}
