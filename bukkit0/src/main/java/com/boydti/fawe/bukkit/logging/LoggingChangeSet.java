package com.boydti.fawe.bukkit.logging;

import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.changeset.FaweChangeSet;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.history.change.Change;
import java.util.Iterator;
import org.PrimeSoft.blocksHub.IBlocksHubApi;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class LoggingChangeSet extends FaweChangeSet {

    private final FaweChangeSet parent;
    private final IBlocksHubApi api;
    private final World world;
    private final Location loc;
    private final String name;

    public LoggingChangeSet(FawePlayer<Player> player, FaweChangeSet parent, IBlocksHubApi api) {
        super(parent.getWorld());
        this.parent = parent;
        this.name = player.getName();
        this.api = api;
        this.world = player.parent.getWorld();
        this.loc = new Location(world, 0, 0, 0);
    }

    @Override
    public boolean flush() {
        return parent.flush();
    }

    @Override
    public void add(int x, int y, int z, int combinedId4DataFrom, int combinedId4DataTo) {
        loc.setX(x);
        loc.setY(y);
        loc.setZ(z);
        api.logBlock(name, world, loc, combinedId4DataFrom >> 4, (byte) (combinedId4DataFrom & 0xF), combinedId4DataTo >> 4, (byte) (combinedId4DataTo & 0xF));
        parent.add(x, y, z, combinedId4DataFrom, combinedId4DataTo);
    }

    @Override
    public void addTileCreate(CompoundTag tag) {
        parent.addTileCreate(tag);
    }

    @Override
    public void addTileRemove(CompoundTag tag) {
        parent.addTileRemove(tag);
    }

    @Override
    public void addEntityRemove(CompoundTag tag) {
        parent.addEntityRemove(tag);
    }

    @Override
    public void addEntityCreate(CompoundTag tag) {
        parent.addEntityCreate(tag);
    }

    @Override
    public Iterator<Change> getIterator(boolean undo) {
        return parent.getIterator(undo);
    }

    @Override
    public int size() {
        return parent.size();
    }
}
