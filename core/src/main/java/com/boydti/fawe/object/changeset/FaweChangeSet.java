package com.boydti.fawe.object.changeset;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.history.changeset.ChangeSet;

public interface FaweChangeSet extends ChangeSet {
    boolean flush();

    int getCompressedSize();

    void add(Vector location, BaseBlock from, BaseBlock to);

    void add(int x, int y, int z, int combinedId4DataFrom, BaseBlock to);

    void add(int x, int y, int z, int combinedId4DataFrom, int combinedId4DataTo);
}
