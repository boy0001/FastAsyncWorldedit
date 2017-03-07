package com.boydti.fawe.object.brush;

import com.boydti.fawe.object.PseudoRandom;
import com.boydti.fawe.object.collection.LocalBlockVectorSet;
import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.MutableBlockVector;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.Masks;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.function.visitor.BreadthFirstSearch;
import com.sk89q.worldedit.regions.CuboidRegion;

public class ShatterBrush implements Brush {
    private final int count;

    public ShatterBrush(int count) {
        this.count = count;
    }

    @Override
    public void build(EditSession editSession, final Vector position, Pattern pattern, double size) throws MaxChangedBlocksException {
        // We'll want this to be somewhat circular, so the cuboid needs to fit.
        int r2Radius = (int) Math.ceil(size * Math.sqrt(2));
        int radius2 = (int) (Math.ceil(r2Radius * r2Radius));
        Vector bot = new MutableBlockVector(position.subtract(size, size, size));
        Vector top = new MutableBlockVector(position.add(size, size, size));
        CuboidRegion region = new CuboidRegion(bot, top);
        // We'll want to use a fast random
        PseudoRandom random = new PseudoRandom();
        // We don't need double precision, so use a BlockVector
        BlockVector min = region.getMinimumPoint().toBlockVector();
        BlockVector max = region.getMaximumPoint().toBlockVector();
        // Let's keep it inside the brush radius
        int dx = max.getBlockX() - min.getBlockX() + 1;
        int dy = max.getBlockY() - min.getBlockY() + 1;
        int dz = max.getBlockZ() - min.getBlockZ() + 1;
        // We'll store the points in a set
        LocalBlockVectorSet queue = new LocalBlockVectorSet();
        // User could select a single block and try to create 10 points = infinite loop
        // To avoid being stuck in an infinite loop, let's stop after 5 collisions
        int maxFails = 5;
        for (int added = 0; added < count;) {
            int x = (int) (random.nextDouble() * dx) + min.getBlockX();
            int z = (int) (random.nextDouble() * dz) + min.getBlockZ();
            int y = editSession.getHighestTerrainBlock(x, z, 0, 255);
            // Check the adjacent blocks efficiently (loops over the adjacent blocks, or the set; whichever is faster)
            if (!queue.containsRadius(x, y, z, 1)) {
                added++;
                queue.add(x, y, z);
            } else if (maxFails-- <= 0) {
                break;
            }
        }
        // Ideally we'd calculate all the bisecting planes, but that's complex to program
        // With this algorithm compute time depends on the number of blocks rather than the number of points
        //  - Expand from each point (block by block) until there is a collision
        {
            // Keep track of where we've visited
            LocalBlockVectorSet visited = queue;
            LocalBlockVectorSet tmp = new LocalBlockVectorSet();
            // Individual frontier for each point
            LocalBlockVectorSet[] frontiers = new LocalBlockVectorSet[queue.size()];
            // Keep track of where each frontier has visited
            LocalBlockVectorSet[] frontiersVisited = new LocalBlockVectorSet[queue.size()];
            // Initiate the frontier with the starting points
            int i = 0;
            for (Vector pos : queue) {
                LocalBlockVectorSet set = new LocalBlockVectorSet();
                set.add(pos);
                frontiers[i] = set;
                frontiersVisited[i] = set.clone();
                i++;
            }
            // Mask
            Mask mask = editSession.getMask();
            if (mask == null) {
                mask = Masks.alwaysTrue();
            }
            final Mask finalMask = mask;
            // Expand
            boolean notEmpty = true;
            while (notEmpty) {
                notEmpty = false;
                for (i = 0; i < frontiers.length; i++) {
                    LocalBlockVectorSet frontier = frontiers[i];
                    notEmpty |= !frontier.isEmpty();
                    final LocalBlockVectorSet frontierVisited = frontiersVisited[i];
                    // This is a temporary set with the next blocks the frontier will visit
                    final LocalBlockVectorSet finalTmp = tmp;
                    frontier.forEach(new LocalBlockVectorSet.BlockVectorSetVisitor() {
                        @Override
                        public void run(int x, int y, int z, int index) {
                            if (PseudoRandom.random.random(2) == 0) {
                                finalTmp.add(x, y, z);
                                return;
                            }
                            for (Vector direction : BreadthFirstSearch.DEFAULT_DIRECTIONS) {
                                int x2 = x + direction.getBlockX();
                                int y2 = y + direction.getBlockY();
                                int z2 = z + direction.getBlockZ();
                                // Check boundary
                                int dx = position.getBlockX() - x2;
                                int dy = position.getBlockY() - y2;
                                int dz = position.getBlockZ() - z2;
                                int dSqr = (dx * dx) + (dy * dy) + (dz * dz);
                                if (dSqr <= radius2) {
                                    if (finalMask.test(MutableBlockVector.get(x2, y2, z2))) {
                                        // (collision) If it's visited and part of another frontier, set the block
                                        if (!visited.add(x2, y2, z2)) {
                                            if (!frontierVisited.contains(x2, y2, z2)) {
                                                editSession.setBlock(x2, y2, z2, pattern);
                                            }
                                        } else {
                                            // Hasn't visited and not a collision = add it
                                            finalTmp.add(x2, y2, z2);
                                            frontierVisited.add(x2, y2, z2);
                                        }
                                    }
                                }
                            }
                        }
                    });
                    // Swap the frontier with the temporary set
                    frontiers[i] = tmp;
                    tmp = frontier;
                    tmp.clear();
                }
            }
        }
    }
}
