package com.personthecat.cavegenerator.world;

import com.personthecat.cavegenerator.config.ConfigFile;
import net.minecraftforge.event.terraingen.InitMapGenEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class ReplaceVanillaCaveGen {

    @SuppressWarnings("unused")
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onInitMapGen(InitMapGenEvent event) {
        switch (event.getType()) {
            case CAVE :
                if (event.getNewGen().equals(event.getOriginalGen())) {
                    event.setNewGen(new EarlyCaveHook(null));
                } else {
                    event.setNewGen(new EarlyCaveHook(event.getNewGen()));
                }
                break;
            case RAVINE :
                if (event.getNewGen().equals(event.getOriginalGen())) {
                    event.setNewGen(new NoGeneration());
                }
                break;
            case NETHER_CAVE :
                if (ConfigFile.netherGenerate) {
                    if (event.getNewGen().equals(event.getOriginalGen())) {
                        event.setNewGen(new EarlyCaveHook(null));
                    } else {
                        event.setNewGen(new EarlyCaveHook(event.getNewGen()));
                    }
                }
        }
    }
}