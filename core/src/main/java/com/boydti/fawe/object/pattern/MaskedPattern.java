package com.boydti.fawe.object.pattern;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.pattern.AbstractPattern;
import com.sk89q.worldedit.function.pattern.Pattern;
import java.util.Arrays;
import java.util.List;

public class MaskedPattern extends AbstractPattern {

    private final PatternExtent patternExtent;
    private final Pattern secondaryPattern;
    private final List<Pattern> patterns;
    private Mask mask;

    public MaskedPattern(Mask mask, PatternExtent primary, Pattern secondary) {
        this.mask = mask;
        this.patternExtent = primary;
        this.secondaryPattern = secondary;
        this.patterns = Arrays.asList(primary, secondary);
    }


    @Override
    public BaseBlock apply(Vector position) {
        patternExtent.setTarget(position);
        if (mask.test(position)) {
            return patternExtent.getAndResetTarget();
        }
        return secondaryPattern.apply(position);
    }

    @Override
    public boolean apply(Extent extent, Vector set, Vector get) throws WorldEditException {
        patternExtent.setTarget(get);
        if (mask.test(get)) {
            return patternExtent.getAndResetTarget(extent, set, get);
        }
        return secondaryPattern.apply(extent, set, get);
    }
}
