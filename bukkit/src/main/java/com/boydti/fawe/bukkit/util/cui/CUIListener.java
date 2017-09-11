package com.boydti.fawe.bukkit.util.cui;

import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.util.cui.CUI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.Plugin;

public class CUIListener implements Listener {

    public CUIListener(Plugin plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if ((int) from.getX() != (int) to.getX() || (int) from.getZ() != (int) to.getZ()) {
            FawePlayer<Object> player = FawePlayer.wrap(event.getPlayer());
            CUI cui = player.getMeta("CUI");
            if (cui instanceof StructureCUI) {
                StructureCUI sCui = (StructureCUI) cui;
            }
        }
    }

}
