package com.boydti.fawe.object.brush.scroll;

import com.sk89q.worldedit.command.tool.BrushTool;

public abstract class ScrollAction implements ScrollableBrush {
    public final BrushTool tool;

    public ScrollAction(BrushTool tool) {
        this.tool = tool;
    }
}
