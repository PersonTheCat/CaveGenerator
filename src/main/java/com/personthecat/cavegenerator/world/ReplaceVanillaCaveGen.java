package com.personthecat.cavegenerator.world;

import com.personthecat.cavegenerator.Main;
import com.personthecat.cavegenerator.util.Lazy;
import net.minecraftforge.event.terraingen.InitMapGenEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import java.util.Optional;

public class ReplaceVanillaCaveGen {

    /** Determines whether any presets are enabled in dim -1. */
    private static final Lazy<Boolean> netherPresetExists = new Lazy<>(
        () -> Main.instance.generators.containsKey(-1)
    );

    @SuppressWarnings("unused")
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onInitMapGen(InitMapGenEvent event) {
        if (event.getNewGen().equals(event.getOriginalGen())) {
            return;
        }
        switch (event.getType()) {
            case CAVE :
                Main.instance.priorCaves = Optional.of(event.getNewGen());
                event.setNewGen(new CaveManager());
                break;
            case RAVINE :
                Main.instance.priorRavines = Optional.of(event.getNewGen());
                event.setNewGen(new RavineManager());
                break;
            case NETHER_CAVE:
                if (netherPresetExists.get()) {
                    Main.instance.priorNetherCaves = Optional.of(event.getNewGen());
                    event.setNewGen(new CaveManager());
                }
        }
    }
}