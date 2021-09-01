package personthecat.cavegenerator;

import lombok.extern.log4j.Log4j2;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.BiomeLoadingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import personthecat.catlib.exception.MissingOverrideException;
import personthecat.cavegenerator.util.Reference;
import personthecat.cavegenerator.world.event.BiomeCleanupEvent;
import personthecat.cavegenerator.world.event.CaveModificationContext;
import personthecat.overwritevalidator.annotations.Inherit;
import personthecat.overwritevalidator.annotations.OverwriteClass;

@Log4j2
@OverwriteClass
@Mod(Reference.MOD_ID)
public class CaveGenerator {

    public CaveGenerator() {
        final IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        final IEventBus eventBus = MinecraftForge.EVENT_BUS;

        modBus.addListener((FMLCommonSetupEvent e) -> this.initCommon());
        eventBus.addListener((FMLServerStartingEvent e) -> this.serverStarting(e.getServer()));
        eventBus.addListener((FMLServerStoppingEvent e) -> this.serverStopping(e.getServer()));
        eventBus.addListener((BiomeLoadingEvent e) -> BiomeCleanupEvent.onBiomeCleanup(new CaveModificationContext(e)));
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
