package com.boydti.fawe.object.extent;

import com.boydti.fawe.util.ExtentTraverser;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;


import static com.google.common.base.Preconditions.checkNotNull;

public class TransformExtent extends AbstractDelegateExtent {
    public TransformExtent(Extent parent) {
        super(parent);
    }

    public TransformExtent setExtent(Extent extent) {
        checkNotNull(extent);
        if (getExtent() instanceof TransformExtent) {
            ((TransformExtent) getExtent()).setExtent(extent);
        } else {
            new ExtentTraverser(this).setNext(extent);
        }
        return this;
    }
}