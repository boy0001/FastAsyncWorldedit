package com.boydti.fawe.object.pattern;

import com.boydti.fawe.object.PseudoRandom;
import com.boydti.fawe.object.schematic.Schematic;
import com.sk89q.worldedit.MutableBlockVector;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.pattern.AbstractPattern;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.registry.WorldData;


import static com.google.common.base.Preconditions.checkNotNull;

public class RandomFullClipboardPattern extends AbstractPattern {
    private final Extent extent;
    private final ClipboardHolder[] clipboards;
    private final MutableBlockVector mutable = new MutableBlockVector();
    private boolean randomRotate;
    private WorldData worldData;

    public RandomFullClipboardPattern(Extent extent, WorldData worldData, ClipboardHolder[] clipboards, boolean randomRotate) {
        checkNotNull(clipboards);
        this.clipboards = clipboards;
        this.extent = extent;
        this.randomRotate = randomRotate;
        this.worldData = worldData;
    }

    @Override
    public boolean apply(Extent extent, Vector setPosition, Vector getPosition) throws WorldEditException {
        ClipboardHolder holder = clipboards[PseudoRandom.random.random(clipboards.length)];
        if (randomRotate) {
            holder.setTransform(new AffineTransform().rotateY(PseudoRandom.random.random(4) * 90));
        }
        Clipboard clipboard = holder.getClipboard();
        Schematic schematic = new Schematic(clipboard);
        if (holder.getTransform().isIdentity()) {
            schematic.paste(extent, setPosition, false);
        } else {
            schematic.paste(extent, worldData, setPosition, false, holder.getTransform());
        }
        return true;
    }

    @Override
    public BaseBlock apply(Vector position) {
        throw new IllegalStateException("Incorrect use. This pattern can only be applied to an extent!");
    }
}
