package com.boydti.fawe.object.brush;

import com.boydti.fawe.object.brush.visualization.VisualBrush;
import com.boydti.fawe.object.collection.LocalBlockVectorSet;
import com.boydti.fawe.wrappers.LocationMaskedPlayerWrapper;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.command.tool.BrushTool;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.transform.AffineTransform;

public class CircleBrush extends VisualBrush {
    private final Player player;

    public CircleBrush(BrushTool tool, Player player) {
        super(tool);
        this.player = LocationMaskedPlayerWrapper.unwrap(player);
    }

    @Override
    public void build(BrushTool.BrushAction action, EditSession editSession, Vector position, Pattern pattern, double size) throws MaxChangedBlocksException {
        switch (action) {
            case PRIMARY:
                LocalBlockVectorSet set = new LocalBlockVectorSet();
                int radius = (int) size;
                Vector normal = position.subtract(player.getPosition());
                editSession.makeCircle(position, pattern, size, size, size, false, normal);
                break;
            case SECONDARY:
                break;
        }
    }

    private static Vector any90Rotate(Vector normal) {
        normal = normal.normalize();
        if (normal.getX() == 1 || normal.getY() == 1 || normal.getZ() == 1) {
            return new Vector(normal.getZ(), normal.getX(), normal.getY());
        }
        AffineTransform affine = new AffineTransform();
        affine = affine.rotateX(90);
        affine = affine.rotateY(90);
        affine = affine.rotateZ(90);
        Vector random = affine.apply(normal);
        return random.cross(normal).normalize();
    }
}
