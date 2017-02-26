package com.boydti.fawe.object.brush;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.command.tool.BrushTool;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.sk89q.worldedit.function.pattern.Pattern;

public interface DoubleActionBrush extends Brush {

    @Override
    default void build(EditSession editSession, Vector position, Pattern pattern, double size) throws MaxChangedBlocksException {
        build(BrushTool.BrushAction.PRIMARY, editSession, position, pattern, size);
    }

    public void build(BrushTool.BrushAction action, EditSession editSession, Vector position, Pattern pattern, double size) throws MaxChangedBlocksException;
}
