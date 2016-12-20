package com.boydti.fawe.object.brush;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.function.pattern.Patterns;

public class LineBrush implements DoubleActionBrush {

    private final boolean shell, select, flat;
    private Vector pos1;

    public LineBrush(boolean shell, boolean select, boolean flat) {
        this.shell = shell;
        this.select = select;
        this.flat = flat;
    }

    @Override
    public void build(DoubleActionBrushTool.BrushAction action, EditSession editSession, Vector position, final Pattern pattern, double size) throws MaxChangedBlocksException {
        switch (action) {
            case PRIMARY:
                if (pos1 == null) {
                    pos1 = position;
                    return;
                }
                editSession.drawLine(Patterns.wrap(pattern), pos1, position, size, !shell, flat);
                if (!select) {
                    return;
                }
            case SECONDARY:
                pos1 = position;
                return;
        }
    }
}
