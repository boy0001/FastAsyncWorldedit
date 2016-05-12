package com.boydti.fawe.object.change;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.object.extent.FastWorldEditExtent;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.DoubleTag;
import com.sk89q.jnbt.LongTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.history.UndoContext;
import com.sk89q.worldedit.history.change.Change;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MutableEntityChange implements Change {

    public CompoundTag tag;
    public boolean create;

    public MutableEntityChange(CompoundTag tag, boolean create) {
        this.tag = tag;
        this.create = create;
    }

    @Override
    public void undo(UndoContext context) throws WorldEditException {
        if (!create) {
            create(context);
        } else {
            delete(context);
        }
    }

    @Override
    public void redo(UndoContext context) throws WorldEditException {
        if (create) {
            create(context);
        } else {
            delete(context);
        }
    }

    public void delete(UndoContext context) {
        Extent extent = context.getExtent();
        if (extent.getClass() == FastWorldEditExtent.class) {
            FastWorldEditExtent fwee = (FastWorldEditExtent) extent;
            Map<String, Tag> map = tag.getValue();
            long most;
            long least;
            if (map.containsKey("UUIDMost")) {
                most = ((LongTag) map.get("UUIDMost")).getValue();
                least = ((LongTag) map.get("UUIDLeast")).getValue();
            } else if (map.containsKey("PersistentIDMSB")) {
                most = ((LongTag) map.get("PersistentIDMSB")).getValue();
                least = ((LongTag) map.get("PersistentIDLSB")).getValue();
            } else {
                Fawe.debug("Skipping entity without uuid.");
                return;
            }
            List<DoubleTag> pos = (List<DoubleTag>) map.get("Pos").getValue();
            int x = (int) Math.round(pos.get(0).getValue());
            int y = (int) Math.round(pos.get(1).getValue());
            int z = (int) Math.round(pos.get(2).getValue());
            UUID uuid = new UUID(most, least);
            fwee.getQueue().removeEntity(x, y, z, uuid);
        } else {
            Fawe.debug("FAWE doesn't support: " + context + " for " + getClass() + " (bug Empire92)");
        }
    }

    public void create(UndoContext context) {
        Extent extent = context.getExtent();
        if (extent.getClass() == FastWorldEditExtent.class) {
            FastWorldEditExtent fwee = (FastWorldEditExtent) extent;
            Map<String, Tag> map = tag.getValue();
            List<DoubleTag> pos = (List<DoubleTag>) map.get("Pos").getValue();
            int x = (int) Math.round(pos.get(0).getValue());
            int y = (int) Math.round(pos.get(1).getValue());
            int z = (int) Math.round(pos.get(2).getValue());
            fwee.getQueue().setEntity(x, y, z, tag);
        } else {
            Fawe.debug("FAWE doesn't support: " + context + " for " + getClass() + " (bug Empire92)");
        }
    }
}
