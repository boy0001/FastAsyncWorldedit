package com.boydti.fawe.object.extent;

import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import java.util.Arrays;
import java.util.Collection;

public class LinearTransform extends SelectTransform {

    private final Collection<ResettableExtent> extents;
    private final ResettableExtent[] extentsArray;
    private int index;

    public LinearTransform(ResettableExtent[] extents) {
        this.extentsArray = extents;
        this.extents = Arrays.asList(extents);
    }

    @Override
    public ResettableExtent setExtent(Extent extent) {
        for (ResettableExtent cur : extentsArray) {
            cur.setExtent(extent);
        }
        return this;
    }

    @Override
    public AbstractDelegateExtent getExtent(int x, int y, int z) {
        if (index == extentsArray.length) {
            index = 0;
        }
        return extentsArray[index];
    }

    @Override
    public AbstractDelegateExtent getExtent(int x, int z) {
        return getExtent(x, 0, z);
    }
}
