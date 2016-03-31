package com.boydti.fawe.logging;

import org.PrimeSoft.blocksHub.BlocksHub;
import org.PrimeSoft.blocksHub.IBlocksHubApi;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.boydti.fawe.object.FawePlayer;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.history.changeset.ChangeSet;

public class BlocksHubHook {
    private final BlocksHub hub;
    private final IBlocksHubApi api;

    public BlocksHubHook() {
        this.hub = (BlocksHub) Bukkit.getServer().getPluginManager().getPlugin("BlocksHub");
        this.api = this.hub.getApi();
    }

    public Extent getLoggingExtent(final Extent parent, final ChangeSet set, final FawePlayer<?> player) {
        return new LoggingExtent(parent, set, (FawePlayer<Player>) player, this.api);
    }
}
