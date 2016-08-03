package com.boydti.fawe.object.brush;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.function.pattern.Pattern;

public class InspectBrush implements Brush {

    private final Player player;
    private final int radius;

    public InspectBrush(Player player, double radius) {
        this.player = player;
        this.radius = (int) radius;
    }

    @Override
    public void build(EditSession editSession, Vector position, Pattern pattern, double size) throws MaxChangedBlocksException {
        // TODO
    }
}
