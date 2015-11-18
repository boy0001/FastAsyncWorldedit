package com.boydti.fawe.util;

import com.sk89q.worldedit.event.extent.EditSessionEvent;
import com.sk89q.worldedit.util.eventbus.EventHandler.Priority;
import com.sk89q.worldedit.util.eventbus.Subscribe;

public class WESubscriber {
    
    @Subscribe(priority = Priority.VERY_EARLY)
    public void onEditSession(final EditSessionEvent event) {
    }
}
