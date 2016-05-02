package com.boydti.fawe.bukkit.logging;

import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.changeset.FaweChangeSet;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.history.change.Change;
import java.util.Iterator;
import org.PrimeSoft.blocksHub.IBlocksHubApi;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class LoggingChangeSet implements FaweChangeSet {

    private final FaweChangeSet parent;
    private final IBlocksHubApi api;
    private final World world;
    private final Location loc;
    private final String name;

    public LoggingChangeSet(FawePlayer<Player> player, FaweChangeSet parent, IBlocksHubApi api) {
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
    public int getCompressedSize() {
        return parent.getCompressedSize();
    }

    @Override
    public void add(Vector location, BaseBlock from, BaseBlock to) {
        loc.setX(location.getX());
        loc.setY(location.getY());
        loc.setZ(location.getZ());
        api.logBlock(name, world, loc, from.getId(), (byte) from.getData(), to.getId(), (byte) to.getData());
        parent.add(location, from, to);
    }

    @Override
    public void add(int x, int y, int z, int combinedId4DataFrom, BaseBlock to) {
        loc.setX(x);
        loc.setY(y);
        loc.setZ(z);
        api.logBlock(name, world, loc, combinedId4DataFrom >> 4, (byte) (combinedId4DataFrom & 0xF), to.getId(), (byte) to.getData());
        parent.add(x, y, z, combinedId4DataFrom, to);
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
    public void add(Change change) {
        parent.add(change);
    }

    @Override
    public Iterator<Change> backwardIterator() {
        return parent.backwardIterator();
    }

    @Override
    public Iterator<Change> forwardIterator() {
        return parent.forwardIterator();
    }

    @Override
    public int size() {
        return parent.size();
    }
}
