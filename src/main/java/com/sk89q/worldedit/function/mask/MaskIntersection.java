/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit.function.mask;

import com.sk89q.worldedit.Vector;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;


import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Combines several masks and requires that all masks return true
 * when a certain position is tested. It serves as a logical AND operation
 * on a list of masks.
 */
public class MaskIntersection extends AbstractMask implements Serializable {

    private transient Set<Mask> masks = new LinkedHashSet<>();
    private Mask[] masksArray;

    /**
     * Create a new intersection.
     *
     * @param masks a list of masks
     */
    public MaskIntersection(Collection<Mask> masks) {
        checkNotNull(masks);
        this.masks.addAll(masks);
        formArray();
    }

    private void formArray() {
        if (masks.isEmpty()) {
            masksArray = new Mask[]{Masks.alwaysFalse()};
        } else {
            masksArray = masks.toArray(new Mask[masks.size()]);
        }
    }

    /**
     * Create a new intersection.
     *
     * @param mask a list of masks
     */
    public MaskIntersection(Mask... mask) {
        this(Arrays.asList(checkNotNull(mask)));
    }

    /**
     * Add some masks to the list.
     *
     * @param masks the masks
     */
    public void add(Collection<Mask> masks) {
        checkNotNull(masks);
        this.masks.addAll(masks);
        formArray();
    }

    /**
     * Add some masks to the list.
     *
     * @param mask the masks
     */
    public void add(Mask... mask) {
        add(Arrays.asList(checkNotNull(mask)));
    }

    /**
     * Get the masks that are tested with.
     *
     * @return the masks
     */
    public Collection<Mask> getMasks() {
        return masks;
    }

    public final Mask[] getMasksArray() {
        return masksArray;
    }

    @Override
    public boolean test(Vector vector) {
        for (Mask mask : masksArray) {
            if (!mask.test(vector)) {
                return false;
            }
        }
        return true;
    }

    @Nullable
    @Override
    public Mask2D toMask2D() {
        List<Mask2D> mask2dList = new ArrayList<Mask2D>();
        for (Mask mask : masks) {
            Mask2D mask2d = mask.toMask2D();
            if (mask2d != null) {
                mask2dList.add(mask2d);
            } else {
                return null;
            }
        }
        return new MaskIntersection2D(mask2dList);
    }

    private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        masks = new HashSet(Arrays.asList(masksArray));
    }
}
