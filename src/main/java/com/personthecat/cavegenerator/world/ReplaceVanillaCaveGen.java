package com.personthecat.cavegenerator.world;

import com.personthecat.cavegenerator.Main;
import net.minecraftforge.event.terraingen.InitMapGenEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import java.util.Optional;

public class ReplaceVanillaCaveGen {
    @SuppressWarnings("unused")
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onInitMapGen(InitMapGenEvent event) {
        switch (event.getType()) {
            case CAVE :
                if (!event.getNewGen().equals(event.getOriginalGen())) {
                    Main.instance.priorCaves = Optional.of(event.getNewGen());
                }
                event.setNewGen(new CaveManager());
                break;
            case RAVINE :
                if (!event.getNewGen().equals(event.getOriginalGen())) {
                    Main.instance.priorRavines = Optional.of(event.getNewGen());
                }
                event.setNewGen(new RavineManager());
                break;
        }
    }
}