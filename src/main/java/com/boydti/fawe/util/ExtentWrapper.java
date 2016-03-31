package com.boydti.fawe.util;

import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;

public class ExtentWrapper extends AbstractDelegateExtent {

    public ExtentWrapper(final Extent extent) {
        super(extent);
    }
}
