package com.boydti.fawe.object.brush.visualization;

import com.boydti.fawe.object.brush.DoubleActionBrush;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.command.tool.BrushTool;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.sk89q.worldedit.function.pattern.Pattern;

public class DelegateVisualBrush extends VisualBrush {
    private final Brush brush;

    public DelegateVisualBrush(BrushTool tool, Brush brush) {
        super(tool);
        this.brush = brush;
    }

    @Override
    public void build(BrushTool.BrushAction action, EditSession editSession, Vector position, Pattern pattern, double size) throws MaxChangedBlocksException {
        switch (action) {
            case PRIMARY:
                brush.build(editSession, position, pattern, size);
                break;
            case SECONDARY:
                if (brush instanceof DoubleActionBrush) {
                    ((DoubleActionBrush) brush).build(BrushTool.BrushAction.SECONDARY, editSession, position, pattern, size);
                }
                break;
        }
    }
}
