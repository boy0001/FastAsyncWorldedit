package com.boydti.fawe.object.changeset;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.history.changeset.ChangeSet;

public interface FaweChangeSet extends ChangeSet {
    void flush();

    void add(Vector location, BaseBlock from, BaseBlock to);
}
