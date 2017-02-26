package com.boydti.fawe.object.brush.scroll;

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.command.tool.BrushTool;

public class ScrollSize extends ScrollAction {
    public ScrollSize(BrushTool tool) {
        super(tool);
    }

    @Override
    public boolean increment(int amount) {
        int max = WorldEdit.getInstance().getConfiguration().maxRadius;
        double newSize = Math.max(0, Math.min(max, tool.getSize() + amount));
        tool.setSize(newSize);
        return true;
    }
}
