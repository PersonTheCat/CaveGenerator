package com.personthecat.cavegenerator.world;

import com.personthecat.cavegenerator.Main;
import com.personthecat.cavegenerator.util.Lazy;
import net.minecraftforge.event.terraingen.InitMapGenEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Optional;

import static com.personthecat.cavegenerator.util.CommonMethods.*;

public class ReplaceVanillaCaveGen {

    /** Determines whether any presets are enabled in dim -1. */
    private static final Lazy<Boolean> netherPresetExists = new Lazy<>(
        () -> find(Main.instance.presets.values(), cfg -> ArrayUtils.contains(cfg.conditions.dimensions, -1))
            .isPresent()
    );

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
            case NETHER_CAVE :
                if (netherPresetExists.get()) {
                    if (!event.getNewGen().equals(event.getOriginalGen())) {
                        Main.instance.priorNetherCaves = Optional.of(event.getNewGen());
                    }
                    event.setNewGen(new CaveManager());
                }
        }
    }
}