package com.boydti.fawe.object.extent;

import com.boydti.fawe.util.ExtentTraverser;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.world.World;


import static com.google.common.base.Preconditions.checkNotNull;

public class ResettableExtent extends AbstractDelegateExtent {
    public ResettableExtent(Extent parent) {
        super(parent);
    }

    public ResettableExtent setExtent(Extent extent) {
        checkNotNull(extent);
        Extent next = getExtent();
        if (!(next instanceof NullExtent) && !(next instanceof World) && next instanceof ResettableExtent) {
            ((ResettableExtent) next).setExtent(extent);
        } else {
            new ExtentTraverser(this).setNext(new AbstractDelegateExtent(extent));
        }
        return this;
    }
}