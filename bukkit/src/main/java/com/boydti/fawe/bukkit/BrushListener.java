package com.boydti.fawe.bukkit;

import com.boydti.fawe.config.BBC;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.brush.MovableBrush;
import com.boydti.fawe.object.brush.scroll.ScrollableBrush;
import com.boydti.fawe.object.brush.visualization.VisualBrush;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.command.tool.BrushTool;
import com.sk89q.worldedit.command.tool.InvalidToolBindException;
import com.sk89q.worldedit.command.tool.Tool;
import com.sk89q.worldedit.command.tool.brush.Brush;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;

public class BrushListener implements Listener {
    public BrushListener(Plugin plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerItemHoldEvent(final PlayerItemHeldEvent event) {
        final Player bukkitPlayer = event.getPlayer();
        if (bukkitPlayer.isSneaking()) {
            return;
        }
        FawePlayer<Object> fp = FawePlayer.wrap(bukkitPlayer);
        com.sk89q.worldedit.entity.Player player = fp.getPlayer();
        LocalSession session = fp.getSession();
        Tool tool = session.getTool(player);
        if (tool != null) {
            ScrollableBrush scrollable;
            if (tool instanceof ScrollableBrush) {
                scrollable = (ScrollableBrush) tool;
            } else if (tool instanceof BrushTool) {
                Brush brush = ((BrushTool) tool).getBrush();
                scrollable = brush instanceof ScrollableBrush ? (ScrollableBrush) brush : null;
            } else {
                return;
            }
            if (scrollable != null) {
                final int slot = event.getNewSlot();
                final int oldSlot = event.getPreviousSlot();
                final int ri;
                if ((((slot - oldSlot) <= 4) && ((slot - oldSlot) > 0)) || (((slot - oldSlot) < -4))) {
                    ri = 1;
                } else {
                    ri = -1;
                }
                if (scrollable.increment(ri)) {
                    final PlayerInventory inv = bukkitPlayer.getInventory();
                    final ItemStack item = inv.getItem(slot);
                    final ItemStack newItem = inv.getItem(oldSlot);
                    inv.setItem(slot, newItem);
                    inv.setItem(oldSlot, item);
                    bukkitPlayer.updateInventory();
                    if (scrollable instanceof VisualBrush) {
                        try {
                            ((VisualBrush) scrollable).queueVisualization(fp);
                        } catch (Throwable e) {
                            WorldEdit.getInstance().getPlatformManager().handleThrowable(e, player);
                        }
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if ((from.getYaw() != to.getYaw() &&  from.getPitch() != to.getPitch()) || from.getBlockX() != to.getBlockX() || from.getBlockZ() != to.getBlockZ() || from.getBlockY() != to.getBlockY()) {
            Player bukkitPlayer = event.getPlayer();
            FawePlayer<Object> fp = FawePlayer.wrap(bukkitPlayer);
            com.sk89q.worldedit.entity.Player player = fp.getPlayer();
            LocalSession session = fp.getSession();
            Tool tool = session.getTool(player);
            if (tool != null) {
                if (tool instanceof MovableBrush) {
                    ((MovableBrush) tool).move(player);
                } else if (tool instanceof BrushTool) {
                    Brush brush = ((BrushTool) tool).getBrush();
                    if (brush instanceof MovableBrush) {
                        if (((MovableBrush) brush).move(player)) {
                            if (brush instanceof VisualBrush) {
                                try {
                                    ((VisualBrush) brush).queueVisualization(fp);
                                } catch (Throwable e) {
                                    WorldEdit.getInstance().getPlatformManager().handleThrowable(e, player);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteract(final PlayerInteractEvent event) {
        switch (event.getAction()) {
            case LEFT_CLICK_AIR:
            case LEFT_CLICK_BLOCK:
                Player bukkitPlayer = event.getPlayer();
                if (!bukkitPlayer.isSneaking()) {
                    return;
                }
                FawePlayer<Object> fp = FawePlayer.wrap(bukkitPlayer);
                com.sk89q.worldedit.entity.Player player = fp.getPlayer();
                LocalSession session = fp.getSession();
                int item = player.getItemInHand();
                Tool tool = session.getTool(item);
                if (tool != null) {
                    try {
                        session.setTool(item, null, player);
                        BBC.TOOL_NONE.send(player);
                    } catch (InvalidToolBindException e) {
                        e.printStackTrace();
                    }
                }
        }
    }
}
