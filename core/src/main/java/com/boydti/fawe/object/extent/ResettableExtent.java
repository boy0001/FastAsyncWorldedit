package com.boydti.fawe.object.extent;

import com.boydti.fawe.util.ExtentTraverser;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;


import static com.google.common.base.Preconditions.checkNotNull;

public class ResettableExtent extends AbstractDelegateExtent {
    public ResettableExtent(Extent parent) {
        super(parent);
    }

    public ResettableExtent setExtent(Extent extent) {
        checkNotNull(extent);
        if (getExtent() instanceof ResettableExtent) {
            ((ResettableExtent) getExtent()).setExtent(extent);
        } else {
            new ExtentTraverser(this).setNext(extent);
        }
        return this;
    }
}