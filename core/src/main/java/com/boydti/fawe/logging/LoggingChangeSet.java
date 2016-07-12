package com.boydti.fawe.logging;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.changeset.FaweChangeSet;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.history.change.Change;
import java.util.Iterator;
import org.bukkit.entity.Player;
import org.primesoft.blockshub.IBlocksHubApi;
import org.primesoft.blockshub.api.IPlayer;
import org.primesoft.blockshub.api.IWorld;

public class LoggingChangeSet extends FaweChangeSet {

    private static boolean initialized = false;

    public static FaweChangeSet wrap(FawePlayer<Player> player, FaweChangeSet parent) {
        if (!initialized) {
            initialized = true;
            api = (IBlocksHubApi) Fawe.imp().getBlocksHubApi();
        }
        if (api == null) {
            return parent;
        }
        return new LoggingChangeSet(player, parent);
    }

    public static IBlocksHubApi api;

    private final FaweChangeSet parent;

    private final MutableVector loc;
    private final IPlayer player;
    private final IWorld world;
    private final MutableBlockData oldBlock;
    private final MutableBlockData newBlock;

    private LoggingChangeSet(FawePlayer player, FaweChangeSet parent) {
        super(parent.getWorld());
        this.parent = parent;
        this.world = api.getWorld(player.getLocation().world);
        this.loc = new MutableVector();
        this.oldBlock = new MutableBlockData();
        this.newBlock = new MutableBlockData();
        this.player = api.getPlayer(player.getUUID());
    }

    @Override
    public boolean flush() {
        return parent.flush();
    }

    @Override
    public void add(int x, int y, int z, int combinedId4DataFrom, int combinedId4DataTo) {
        // Mutable (avoids object creation)
        loc.x = x;
        loc.y = y;
        loc.z = z;
        oldBlock.id = FaweCache.getId(combinedId4DataFrom);
        oldBlock.data = FaweCache.getData(combinedId4DataFrom);
        newBlock.id = FaweCache.getId(combinedId4DataTo);
        newBlock.data = FaweCache.getData(combinedId4DataTo);
        // Log to BlocksHub and parent
        api.logBlock(loc, player, world, oldBlock, newBlock);
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