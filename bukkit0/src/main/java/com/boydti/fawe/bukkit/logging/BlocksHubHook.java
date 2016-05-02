package com.boydti.fawe.bukkit.logging;

import com.boydti.fawe.object.FaweLimit;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.changeset.FaweChangeSet;
import com.boydti.fawe.util.FaweQueue;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.extent.Extent;
import org.PrimeSoft.blocksHub.BlocksHub;
import org.PrimeSoft.blocksHub.IBlocksHubApi;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class BlocksHubHook {
    private final BlocksHub hub;
    private final IBlocksHubApi api;

    public BlocksHubHook() {
        this.hub = (BlocksHub) Bukkit.getServer().getPluginManager().getPlugin("BlocksHub");
        this.api = this.hub.getApi();
    }

    public FaweChangeSet getLoggingChangeSet(EditSession session, FaweLimit limit, Extent parent, FaweChangeSet set, FaweQueue queue, FawePlayer<?> player) {
        return new LoggingChangeSet((FawePlayer<Player>) player, set, api);
    }
}
