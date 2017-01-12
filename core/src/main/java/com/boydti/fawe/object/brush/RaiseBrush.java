package com.boydti.fawe.object.brush;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.function.pattern.Pattern;

public class RaiseBrush implements DoubleActionBrush {


    @Override
    public void build(DoubleActionBrushTool.BrushAction action, EditSession editSession, Vector position, Pattern pattern, double size) throws MaxChangedBlocksException {
        switch (action) {
            case PRIMARY:
                break;
            case SECONDARY:
                break;
        }
    }
}