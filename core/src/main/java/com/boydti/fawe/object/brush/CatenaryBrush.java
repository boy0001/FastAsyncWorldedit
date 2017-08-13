package com.boydti.fawe.object.brush;

import com.boydti.fawe.config.BBC;
import com.boydti.fawe.object.brush.visualization.VisualExtent;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.sk89q.worldedit.function.pattern.Pattern;
import java.util.Arrays;
import java.util.List;

public class CatenaryBrush implements Brush, ResettableTool {

    private final boolean shell, select;
    private final double slack;
    private Vector pos1;

    public CatenaryBrush(boolean shell, boolean select, double lengthFactor) {
        this.shell = shell;
        this.select = select;
        this.slack = lengthFactor;
    }

    @Override
    public void build(EditSession editSession, Vector pos2, final Pattern pattern, double size) throws MaxChangedBlocksException {
        boolean visual = (editSession.getExtent() instanceof VisualExtent);
        if (pos1 == null || pos2.equals(pos1)) {
            if (!visual) {
                pos1 = pos2;
                BBC.BRUSH_LINE_PRIMARY.send(editSession.getPlayer(), pos2);
            }
            return;
        }
        Vector vertex = getVertex(pos1, pos2, slack);
        List<Vector> nodes = Arrays.asList(pos1, vertex, pos2);
        editSession.drawSpline(pattern, nodes, 0, 0, 0, 10, size, !shell);
        if (!visual) {
            BBC.BRUSH_LINE_SECONDARY.send(editSession.getPlayer());
            if (!select) {
                pos1 = null;
                return;
            } else {
                pos1 = pos2;
            }
        }
    }

    @Override
    public boolean reset() {
        pos1 = null;
        return true;
    }

    public Vector getVertex(Vector pos1, Vector pos2, double lenPercent) {
        double len = pos1.distance(pos2) * lenPercent;

        double dy = pos2.getY() - pos1.getY();
        double dx = pos2.getX() - pos1.getX();
        double dz = pos2.getZ() - pos1.getZ();
        double h = Math.sqrt(dx * dx + dz * dz);

        double t = Math.sqrt(len * len - dy * dy) / h;
        double z = 0.001;
        for (; Math.sinh(z) < t*z; z += 0.001); // close enough

        double a = (h / 2) / z;
        double p = (h - a * Math.log((len + dy) / (len - dy)))/2;
        double q = (dy - len * Math.cosh(z) / Math.sinh(z)) / 2;
        double y = a * 1 + q;

        return pos1.add(pos2.subtract(pos1).multiply(p / h).add(0, y, 0)).round();
    }
}
