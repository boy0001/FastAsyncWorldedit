package com.boydti.fawe.nukkit.listener;

import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerInteractEvent;
import cn.nukkit.event.player.PlayerItemHeldEvent;
import cn.nukkit.event.player.PlayerMoveEvent;
import cn.nukkit.inventory.PlayerInventory;
import cn.nukkit.item.Item;
import cn.nukkit.level.Location;
import cn.nukkit.plugin.Plugin;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.brush.MovableTool;
import com.boydti.fawe.object.brush.ResettableTool;
import com.boydti.fawe.object.brush.scroll.ScrollTool;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.command.tool.Tool;

public class BrushListener implements Listener {
    public BrushListener(Plugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerItemHoldEvent(final PlayerItemHeldEvent event) {
        Player nukkitPlayer = event.getPlayer();
        if (nukkitPlayer.isSneaking()) {
            return;
        }
        FawePlayer<Object> fp = FawePlayer.wrap(nukkitPlayer);
        com.sk89q.worldedit.entity.Player player = fp.getPlayer();
        LocalSession session = fp.getSession();
        Tool tool = session.getTool(player);
        if (tool instanceof ScrollTool) {
            final int slot = event.getInventorySlot();
            final int oldSlot = event.getSlot();
            final int ri;
            if ((((slot - oldSlot) <= 4) && ((slot - oldSlot) > 0)) || (((slot - oldSlot) < -4))) {
                ri = 1;
            } else {
                ri = -1;
            }
            ScrollTool scrollable = (ScrollTool) tool;
            if (scrollable.increment(player, ri)) {
                final PlayerInventory inv = nukkitPlayer.getInventory();
                final Item item = inv.getItem(slot);
                final Item newItem = inv.getItem(oldSlot);
                inv.setItem(slot, newItem);
                inv.setItem(oldSlot, item);
                inv.sendContents(nukkitPlayer);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if ((from.getYaw() != to.getYaw() &&  from.getPitch() != to.getPitch()) || from.getFloorX() != to.getFloorX() || from.getFloorZ() != to.getFloorZ() || from.getFloorY() != to.getFloorY()) {
            Player nukkitPlayer = event.getPlayer();
            FawePlayer<Object> fp = FawePlayer.wrap(nukkitPlayer);
            com.sk89q.worldedit.entity.Player player = fp.getPlayer();
            LocalSession session = fp.getSession();
            Tool tool = session.getTool(player);
            if (tool != null) {
                if (tool instanceof MovableTool) {
                    ((MovableTool) tool).move(player);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteract(final PlayerInteractEvent event) {
        Player nukkitPlayer = event.getPlayer();
        if (nukkitPlayer.isSneaking()) {
            if (event.getAction() == PlayerInteractEvent.Action.PHYSICAL) {
                return;
            }
            FawePlayer<Object> fp = FawePlayer.wrap(nukkitPlayer);
            com.sk89q.worldedit.entity.Player player = fp.getPlayer();
            LocalSession session = fp.getSession();
            Tool tool = session.getTool(player);
            if (tool instanceof ResettableTool) {
                if (((ResettableTool) tool).reset()) {
                    event.setCancelled(true);
                }
            }
        }
    }
}
