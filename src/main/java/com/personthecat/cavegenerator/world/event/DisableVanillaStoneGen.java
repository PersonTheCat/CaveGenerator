package com.personthecat.cavegenerator.world.event;

import com.personthecat.cavegenerator.CaveInit;
import com.personthecat.cavegenerator.Main;
import com.personthecat.cavegenerator.config.ConfigFile;
import net.minecraftforge.event.terraingen.OreGenEvent.GenerateMinable;
import net.minecraftforge.fml.common.eventhandler.Event.Result;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class DisableVanillaStoneGen {

    /** Disables generation of vanilla stone veins. */
    @SubscribeEvent
    public static void onDisableVanillaStoneGen(GenerateMinable event) {
        // Ensure that this feature is indicated in the config file.
        if (!ConfigFile.enableVanillaStoneClusters) {
            switch (event.getType()) {
                case ANDESITE :
                case DIORITE :
                case GRANITE : event.setResult(Result.DENY);
                default : {}
            }
        }
    }
}