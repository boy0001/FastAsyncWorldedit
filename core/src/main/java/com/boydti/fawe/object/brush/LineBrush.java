package com.boydti.fawe.object.brush;

import com.boydti.fawe.object.brush.visualization.VisualExtent;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.sk89q.worldedit.function.pattern.Pattern;

public class LineBrush implements Brush, ResettableTool {

    private final boolean shell, select, flat;
    private Vector pos1;

    public LineBrush(boolean shell, boolean select, boolean flat) {
        this.shell = shell;
        this.select = select;
        this.flat = flat;
    }

    @Override
    public void build(EditSession editSession, Vector position, final Pattern pattern, double size) throws MaxChangedBlocksException {
        boolean visual = (editSession.getExtent() instanceof VisualExtent);
        if (pos1 == null) {
            if (!visual) pos1 = position;
            return;
        }
        editSession.drawLine(pattern, pos1, position, size, !shell, flat);
        if (!select && !visual) {
            pos1 = null;
            return;
        }
    }

    @Override
    public boolean reset() {
        pos1 = null;
        return true;
    }
}
