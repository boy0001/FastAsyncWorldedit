//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.boydti.fawe.object.function.block;

import com.google.common.base.Preconditions;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.RegionFunction;
import com.sk89q.worldedit.patterns.Pattern;

public class LegacyBlockReplace implements RegionFunction {
    private final Extent extent;
    private Pattern pattern;

    public LegacyBlockReplace(Extent extent, Pattern pattern) {
        Preconditions.checkNotNull(extent);
        Preconditions.checkNotNull(pattern);
        this.extent = extent;
        this.pattern = pattern;
    }

    public boolean apply(Vector position) throws WorldEditException {
        return this.extent.setBlock(position, this.pattern.next(position));
    }
}
