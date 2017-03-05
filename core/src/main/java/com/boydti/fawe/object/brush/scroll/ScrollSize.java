package com.boydti.fawe.object.brush.scroll;

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.command.tool.BrushTool;
import com.sk89q.worldedit.entity.Player;

public class ScrollSize extends ScrollAction {
    public ScrollSize(BrushTool tool) {
        super(tool);
    }

    @Override
    public boolean increment(Player player, int amount) {
        int max = WorldEdit.getInstance().getConfiguration().maxRadius;
        double newSize = Math.max(0, Math.min(max, getTool().getSize() + amount));
        getTool().setSize(newSize);
        return true;
    }
}
