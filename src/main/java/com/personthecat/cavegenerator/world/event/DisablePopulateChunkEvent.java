package com.personthecat.cavegenerator.world.event;

import com.personthecat.cavegenerator.config.ConfigFile;
import net.minecraftforge.event.terraingen.PopulateChunkEvent;
import net.minecraftforge.event.terraingen.PopulateChunkEvent.Populate.EventType;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class DisablePopulateChunkEvent {

    @SubscribeEvent
    public static void onPopulateChunk(PopulateChunkEvent.Populate event) {
        if (!ConfigFile.enableWaterLakes && event.getType() == EventType.LAKE) {
            event.setResult(Event.Result.DENY);
        } else if (!ConfigFile.enableLavaLakes && event.getType() == EventType.LAVA) {
            event.setResult(Event.Result.DENY);
        }
    }
}
