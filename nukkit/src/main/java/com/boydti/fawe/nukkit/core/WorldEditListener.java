/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

// $Id$

package com.boydti.fawe.nukkit.core;


import cn.nukkit.block.Block;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.Listener;
import cn.nukkit.event.block.BlockBreakEvent;
import cn.nukkit.event.player.PlayerChatEvent;
import cn.nukkit.event.player.PlayerCommandPreprocessEvent;
import cn.nukkit.event.player.PlayerGameModeChangeEvent;
import cn.nukkit.event.player.PlayerInteractEvent;
import cn.nukkit.event.player.PlayerInteractEvent.Action;

import com.google.common.base.Joiner;
import com.sk89q.util.StringUtil;
import com.sk89q.worldedit.LocalPlayer;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldVector;
import com.sk89q.worldedit.event.platform.CommandEvent;
import com.sk89q.worldedit.extension.platform.CommandManager;
import com.sk89q.worldedit.internal.LocalWorldAdapter;
import com.sk89q.worldedit.world.World;
import java.util.Arrays;

/**
 * Handles all events thrown in relation to a Player
 */
public class WorldEditListener implements Listener {

    private NukkitWorldEdit plugin;

    /**
     * Called when a player plays an animation, such as an arm swing
     *
     * @param event Relevant event details
     */

    /**
     * Construct the object;
     *
     * @param plugin the plugin
     */
    public WorldEditListener(NukkitWorldEdit plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGamemode(PlayerGameModeChangeEvent event) {
        // this will automatically refresh their session, we don't have to do anything
        WorldEdit.getInstance().getSession(plugin.wrapPlayer(event.getPlayer()));
    }

    /**
     * Called when a player attempts to use a command
     *
     * @param event Relevant event details
     */
    @EventHandler(ignoreCancelled = true,priority = EventPriority.MONITOR)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        String[] split = event.getMessage().split(" ");

        if (split.length > 0) {
            split[0] = split[0].substring(1);
            split = WorldEdit.getInstance().getPlatformManager().getCommandManager().commandDetection(split);
        }
        final String newMessage = "/" + StringUtil.joinString(split, " ");

        if (!newMessage.equals(event.getMessage())) {
            event.setMessage(newMessage);
            plugin.getServer().getPluginManager().callEvent(event);
            if (!event.isCancelled()) {
                if (!event.getMessage().isEmpty()) {
                    plugin.getServer().dispatchCommand(event.getPlayer(), event.getMessage().substring(1));
                }

                event.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true,priority = EventPriority.LOWEST)
    public void onPlayerChat(PlayerChatEvent event) {
        String message = event.getMessage();
        if (message.charAt(0) == '.') {
            String[] split = event.getMessage().split(" ");
            if (split.length > 0) {
                split[0] = split[0].substring(1).replace('.', '/');
                CommandManager cmdMan = WorldEdit.getInstance().getPlatformManager().getCommandManager();
                split = cmdMan.commandDetection(split);
                CommandEvent cmdEvent = new CommandEvent(plugin.wrapCommandSender(event.getPlayer()), Joiner.on(" ").join(Arrays.asList(split)));
                if (cmdMan.getDispatcher().contains(split[0])) {
                    WorldEdit.getInstance().getEventBus().post(cmdEvent);
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockBreak(BlockBreakEvent event) {
        final LocalPlayer player = plugin.wrapPlayer(event.getPlayer());
        final World world = player.getWorld();
        final WorldEdit we = WorldEdit.getInstance();
        final Block clickedBlock = event.getBlock();
        final WorldVector pos = new WorldVector(LocalWorldAdapter.adapt(world), clickedBlock.getX(), clickedBlock.getY(), clickedBlock.getZ());
        if (we.handleBlockLeftClick(player, pos)) {
            event.setCancelled(true);
        }
        if (we.handleArmSwing(player)) {
            event.setCancelled(true);
        }
    }

    /**
     * Called when a player interacts
     *
     * @param event Relevant event details
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerInteract(PlayerInteractEvent event) {
        final LocalPlayer player = plugin.wrapPlayer(event.getPlayer());
        final World world = player.getWorld();
        final WorldEdit we = WorldEdit.getInstance();

        Action action = event.getAction();
        if (action == PlayerInteractEvent.Action.LEFT_CLICK_BLOCK) {
            final Block clickedBlock = event.getBlock();
            final WorldVector pos = new WorldVector(LocalWorldAdapter.adapt(world), clickedBlock.getX(), clickedBlock.getY(), clickedBlock.getZ());

            if (we.handleBlockLeftClick(player, pos)) {
                event.setCancelled(true);
            }

            if (we.handleArmSwing(player)) {
                event.setCancelled(true);
            }

        } else if (action == PlayerInteractEvent.Action.LEFT_CLICK_AIR) {
            if (we.handleArmSwing(player)) {
                event.setCancelled(true);
            }


        } else if (action == PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK) {
            final Block clickedBlock = event.getBlock();
            final WorldVector pos = new WorldVector(LocalWorldAdapter.adapt(world), clickedBlock.getX(),
                    clickedBlock.getY(), clickedBlock.getZ());

            if (we.handleBlockRightClick(player, pos)) {
                event.setCancelled(true);
            }

            if (we.handleRightClick(player)) {
                event.setCancelled(true);
            }
        } else if (action == PlayerInteractEvent.Action.RIGHT_CLICK_AIR) {
            if (we.handleRightClick(player)) {
                event.setCancelled(true);
            }
        }
    }
}