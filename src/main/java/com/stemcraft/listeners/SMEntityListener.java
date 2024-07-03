package com.stemcraft.listeners;

import com.stemcraft.*;
import org.bukkit.entity.Enderman;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.jetbrains.annotations.NotNull;

public class SMEntityListener extends SMListener {

    /**
     * When an entity changes a block within the world
     * @param event The event
     */
    @EventHandler
    public void onEntityChangeBlock(@NotNull EntityChangeBlockEvent event) {

        /* deny Enderman picking up blocks */
        if(event.getEntity() instanceof Enderman) {
            event.setCancelled(true);
        }
    }
}
