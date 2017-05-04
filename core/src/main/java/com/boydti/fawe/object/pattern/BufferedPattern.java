package com.boydti.fawe.object.pattern;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.collection.LocalBlockVectorSet;
import com.boydti.fawe.util.FaweTimer;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.pattern.AbstractPattern;
import com.sk89q.worldedit.function.pattern.Pattern;

public class BufferedPattern extends AbstractPattern implements ResettablePattern {
    private final Pattern pattern;
    private final LocalBlockVectorSet set = new LocalBlockVectorSet();
    private final FaweTimer timer;
    private final long[] actionTime;

    public BufferedPattern(FawePlayer fp, Pattern parent) {
        long[] actionTime = fp.getMeta("lastActionTime");
        if (actionTime == null) fp.setMeta("lastActionTime", actionTime = new long[2]);
        this.actionTime = actionTime;
        this.pattern = parent;
        this.timer = Fawe.get().getTimer();
    }

    @Override
    public BaseBlock apply(Vector position) {
        return pattern.apply(position);
    }

    @Override
    public boolean apply(Extent extent, Vector setPosition, Vector getPosition) throws WorldEditException {
        long now = timer.getTick();
        try {
            if (!set.add(setPosition)) {
                return false;
            }
            return pattern.apply(extent, setPosition, getPosition);
        } catch (UnsupportedOperationException ignore) {}
        return false;
    }

    @Override
    public void reset() {
        long now = timer.getTick();
        if (now - actionTime[1] > 5) {
            set.clear();
        }
        actionTime[1] = actionTime[0];
        actionTime[0] = now;
    }
}
