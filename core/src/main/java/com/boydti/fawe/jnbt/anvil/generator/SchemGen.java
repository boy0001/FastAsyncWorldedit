package com.boydti.fawe.jnbt.anvil.generator;

import com.boydti.fawe.object.PseudoRandom;
import com.boydti.fawe.object.schematic.Schematic;
import com.sk89q.worldedit.MutableBlockVector;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.math.transform.Transform;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.registry.WorldData;
import java.util.List;

public class SchemGen extends Resource {

    private final Extent extent;
    private final WorldData worldData;
    private final List<ClipboardHolder> clipboards;
    private final boolean randomRotate;
    private final Mask mask;

    private MutableBlockVector mutable = new MutableBlockVector();

    public SchemGen(Mask mask, Extent extent, WorldData worldData, List<ClipboardHolder> clipboards, boolean randomRotate) {
        this.mask = mask;
        this.extent = extent;
        this.worldData = worldData;
        this.clipboards = clipboards;
        this.randomRotate = randomRotate;
    }

    @Override
    public boolean spawn(PseudoRandom random, int x, int z) throws WorldEditException {
        mutable.mutX(x);
        mutable.mutZ(z);
        int y = extent.getNearestSurfaceTerrainBlock(x, z, mutable.getBlockY(), 0, 255);
        if (y == -1) return false;
        mutable.mutY(y);
        if (!mask.test(mutable)) {
            return false;
        }
        mutable.mutY(y + 1);
        ClipboardHolder holder = clipboards.get(PseudoRandom.random.random(clipboards.size()));
        if (randomRotate) {
            holder.setTransform(new AffineTransform().rotateY(PseudoRandom.random.random(4) * 90));
        }
        Clipboard clipboard = holder.getClipboard();
        Schematic schematic = new Schematic(clipboard);
        Transform transform = holder.getTransform();
        if (transform.isIdentity()) {
            schematic.paste(extent, mutable, false);
        } else {
            schematic.paste(extent, worldData, mutable, false, transform);
        }
        mutable.mutY(y);
        return true;
    }
}
