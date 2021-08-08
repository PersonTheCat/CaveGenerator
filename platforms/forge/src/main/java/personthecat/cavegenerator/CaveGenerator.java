package personthecat.cavegenerator;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import personthecat.cavegenerator.util.Reference;

@Mod(Reference.MOD_ID)
public class CaveGenerator {

    private final IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
    private final IEventBus eventBus = MinecraftForge.EVENT_BUS;

    public CaveGenerator() {
        this.setupEventHandlers();
    }

    private void setupEventHandlers() {
        this.modBus.addListener(this::initCommon);
        this.eventBus.addListener(this::initServer);
    }

    @SuppressWarnings("unused")
    private void initCommon(final FMLCommonSetupEvent event) {
        // argument types
    }

    @SuppressWarnings("unused")
    private void initServer(final FMLServerStartingEvent event) {
        // commands
    }
}
