package com.boydti.fawe.object.brush;

import com.boydti.fawe.config.BBC;
import com.boydti.fawe.object.exception.FaweException;
import com.boydti.fawe.object.mask.IdMask;
import com.boydti.fawe.object.visitor.DFSRecursiveVisitor;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.function.RegionFunction;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.MaskIntersection;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.function.pattern.Patterns;
import com.sk89q.worldedit.math.interpolation.Interpolation;
import com.sk89q.worldedit.math.interpolation.KochanekBartelsInterpolation;
import com.sk89q.worldedit.math.interpolation.Node;
import com.sk89q.worldedit.math.transform.AffineTransform;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SplineBrush implements DoubleActionBrush {

    public static int MAX_POINTS = 15;
    private ArrayList<ArrayList<Vector>> positionSets;
    private int numSplines;

    private final DoubleActionBrushTool tool;
    private final LocalSession session;
    private final Player player;

    public SplineBrush(Player player, LocalSession session, DoubleActionBrushTool tool) {
        this.tool = tool;
        this.session = session;
        this.player = player;
        this.positionSets = new ArrayList<>();
    }

    @Override
    public void build(DoubleActionBrushTool.BrushAction action, EditSession editSession, final Vector position, Pattern pattern, double size) throws MaxChangedBlocksException {
        Mask mask = tool.getMask();
        if (mask == null) {
            mask = new IdMask(editSession);
        } else {
            mask = new MaskIntersection(mask, new IdMask(editSession));
        }
        switch (action) {
            case PRIMARY: { // Right
                if (positionSets.size() >= MAX_POINTS) {
                    throw new FaweException(BBC.WORLDEDIT_CANCEL_REASON_MAX_CHECKS);
                }
                final ArrayList<Vector> points = new ArrayList<>();
                DFSRecursiveVisitor visitor = new DFSRecursiveVisitor(mask, new RegionFunction() {
                    @Override
                    public boolean apply(Vector p) throws WorldEditException {
                        points.add(new Vector(p));
                        return true;
                    }
                }, (int) size, 1);
                visitor.visit(position);
                Operations.completeBlindly(visitor);
                if (points.size() > numSplines) {
                    numSplines = points.size();
                }
                this.positionSets.add(points);
                player.print(BBC.getPrefix() + BBC.BRUSH_SPLINE_PRIMARY.s());
                break;
            }
            case SECONDARY: {
                if (positionSets.size() < 2) {
                    player.print(BBC.getPrefix() + BBC.BRUSH_SPLINE_SECONDARY_ERROR.s());
                    return;
                }
                List<Vector> centroids = new ArrayList<>();
                for (List<Vector> points : positionSets) {
                    centroids.add(getCentroid(points));
                }

                double tension = 0;
                double bias = 0;
                double continuity = 0;
                double quality = 10;

                final List<Node> nodes = new ArrayList<Node>(centroids.size());

                final Interpolation interpol = new KochanekBartelsInterpolation();
                for (final Vector nodevector : centroids) {
                    final Node n = new Node(nodevector);
                    n.setTension(tension);
                    n.setBias(bias);
                    n.setContinuity(continuity);
                    nodes.add(n);
                }

                Vector up = new Vector(0, 1, 0);
                AffineTransform transform = new AffineTransform();

                // TODO index offset based on transform

                int samples = numSplines;
                for (int i = 0; i < numSplines; i++) {
                    List<Vector> currentSpline = new ArrayList<>();
                    for (ArrayList<Vector> points : positionSets) {
                        int listSize = points.size();
                        int index = (int) (i * listSize / (double) (numSplines));
                        currentSpline.add(points.get(index));
                    }
                    editSession.drawSpline(Patterns.wrap(pattern), currentSpline, 0, 0, 0, 10, 0, true);
                }
                player.print(BBC.getPrefix() + BBC.BRUSH_SPLINE_SECONDARY.s());
                positionSets.clear();
                numSplines = 0;
                break;
            }
        }
    }

    private Vector getCentroid(Collection<Vector> points) {
        Vector sum = new Vector();
        for (Vector p : points) {
            sum.x += p.x;
            sum.y += p.y;
            sum.z += p.z;
        }
        return sum.multiply(1.0 / points.size());
    }

    private Vector normal(Collection<Vector> points, Vector centroid) {
        int n = points.size();
        switch (n) {
            case 1: {
                return null;
            }
            case 2: {
                return null;
            }
        }

        // Calc full 3x3 covariance matrix, excluding symmetries:
        double xx = 0.0; double xy = 0.0; double xz = 0.0;
        double yy = 0.0; double yz = 0.0; double zz = 0.0;

        Vector r = new Vector();
        for (Vector p : points) {
            r.x = p.x - centroid.x;
            r.y = p.y - centroid.y;
            r.z = p.z - centroid.z;
            xx += r.x * r.x;
            xy += r.x * r.y;
            xz += r.x * r.z;
            yy += r.y * r.y;
            yz += r.y * r.z;
            zz += r.z * r.z;
        }

        double det_x = yy*zz - yz*yz;
        double det_y = xx*zz - xz*xz;
        double det_z = xx*yy - xy*xy;

        double det_max = Math.max(Math.max(det_x, det_y), det_z);
        if (det_max <= 0.0) {
            return null;
        }

        // Pick path with best conditioning:
        Vector dir;
        if (det_max == det_x) {
            double a = (xz*yz - xy*zz) / det_x;
            double b = (xy*yz - xz*yy) / det_x;
            dir = new Vector(1.0, a, b);
        } else if (det_max == det_y) {
            double a = (yz*xz - xy*zz) / det_y;
            double b = (xy*xz - yz*xx) / det_y;
            dir = new Vector(a, 1.0, b);
        } else {
            double a = (yz*xy - xz*yy) / det_z;
            double b = (xz*xy - yz*xx) / det_z;
            dir = new Vector(a, b, 1.0);
        };
        return dir.normalize();
    }
}
