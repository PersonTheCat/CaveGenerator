package com.personthecat.cavegenerator.world;

import com.personthecat.cavegenerator.CaveInit;
import com.personthecat.cavegenerator.Main;
import com.personthecat.cavegenerator.config.ConfigFile;
import net.minecraftforge.event.terraingen.OreGenEvent.GenerateMinable;
import net.minecraftforge.fml.common.eventhandler.Event.Result;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class DisableVanillaStoneGen {
    @SubscribeEvent /** Disables generation of vanilla stone veins. */
    public static void onDisableVanillaStoneGen(GenerateMinable event) {
        // Ensure that this feature is indicated in the config file.
        if (!ConfigFile.enableVanillaStoneClusters) {
            // Block generation only when the generator will actually run
            // in the current dimension.
            int dimension = event.getWorld().provider.getDimension();

            if (CaveInit.anyGeneratorEnabled(Main.instance.loadGenerators(event.getWorld()), dimension)) {
                switch (event.getType()) {
                    case ANDESITE :
                    case DIORITE :
                    case GRANITE : event.setResult(Result.DENY);
                    default : {}
                }
            }
        }
    }
}