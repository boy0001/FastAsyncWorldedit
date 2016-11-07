package com.sk89q.worldedit.function.visitor;

import com.boydti.fawe.config.BBC;
import com.boydti.fawe.object.IntegerTrio;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.function.RegionFunction;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.RunContext;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class BreadthFirstSearch implements Operation {

    private final RegionFunction function;
    private final List<Vector> directions = new ArrayList<>();
    private final Map<Node, Integer> visited;
    private final ArrayDeque<Node> queue;
    private final int maxDepth;
    private int affected = 0;

    public BreadthFirstSearch(final RegionFunction function) {
        this(function, Integer.MAX_VALUE);
    }

    public BreadthFirstSearch(final RegionFunction function, int maxDepth) {
        this.queue = new ArrayDeque<>();
        this.visited = new LinkedHashMap<>();
        this.function = function;
        this.directions.add(new Vector(0, -1, 0));
        this.directions.add(new Vector(0, 1, 0));
        this.directions.add(new Vector(-1, 0, 0));
        this.directions.add(new Vector(1, 0, 0));
        this.directions.add(new Vector(0, 0, -1));
        this.directions.add(new Vector(0, 0, 1));
        this.maxDepth = maxDepth;
    }

    public abstract boolean isVisitable(Vector from, Vector to);

    public Collection<Vector> getDirections() {
        return this.directions;
    }

    private IntegerTrio[] getIntDirections() {
        IntegerTrio[] array = new IntegerTrio[directions.size()];
        for (int i = 0; i < array.length; i++) {
            Vector dir = directions.get(i);
            array[i] = new IntegerTrio(dir.getBlockX(), dir.getBlockY(), dir.getBlockZ());
        }
        return array;
    }

    public void visit(final Vector pos) {
        Node node = new Node((int) pos.x, (int) pos.y, (int) pos.z);
        if (!this.visited.containsKey(node)) {
            isVisitable(pos, pos); // Ignore this, just to initialize mask on this point
            visited.put(node, 0);
            queue.add(node);
        }
    }

    @Override
    public Operation resume(RunContext run) throws WorldEditException {
        Node from;
        Node adjacent;
        Vector mutable = new Vector();
        Vector mutable2 = new Vector();
        boolean shouldTrim = false;
        IntegerTrio[] dirs = getIntDirections();
        for (int layer = 0; !queue.isEmpty() && layer <= maxDepth; layer++) {
            int size = queue.size();
            if (layer == maxDepth) {
                visited.clear();
                for (Node current : queue) {
                    mutable.x = current.getX();
                    mutable.y = current.getY();
                    mutable.z = current.getZ();
                    function.apply(mutable);
                    affected++;
                }
                break;
            }
            for (int i = 0; i < size; i++) {
                from = queue.poll();
                mutable.x = from.getX();
                mutable.y = from.getY();
                mutable.z = from.getZ();
                function.apply(mutable);
                affected++;
                for (IntegerTrio direction : dirs) {
                    mutable2.x = from.getX() + direction.x;
                    mutable2.y = from.getY() + direction.y;
                    mutable2.z = from.getZ() + direction.z;
                    if (isVisitable(mutable, mutable2)) {
                        adjacent = new Node(mutable2.getBlockX(), mutable2.getBlockY(), mutable2.getBlockZ());
                        if (!visited.containsKey(adjacent)) {
                            visited.put(adjacent, layer);
                            queue.add(adjacent);
                        }
                    }
                }
            }
            int lastLayer = layer - 1;
            size = visited.size();
            if (shouldTrim || (shouldTrim = size > 16384)) {
                Iterator<Map.Entry<Node, Integer>> iter = visited.entrySet().iterator();
                while (iter.hasNext()) {
                    Map.Entry<Node, Integer> entry = iter.next();
                    Integer val = entry.getValue();
                    if (val < lastLayer) {
                        iter.remove();
                    } else {
                        break;
                    }
                }
            }
        }
        return null;
    }

    @Override
    public void addStatusMessages(List<String> messages) {
        messages.add(BBC.VISITOR_BLOCK.format(getAffected()));
    }

    public int getAffected() {
        return this.affected;
    }

    @Override
    public void cancel() {
    }

    public static final class Node {
        private int x,y,z;

        public Node(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public Node(Node node) {
            this.x = node.x;
            this.y = node.y;
            this.z = node.z;
        }

        public Node() {}

        public final void set(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public final void set(Node node) {
            this.x = node.x;
            this.y = node.y;
            this.z = node.z;
        }

        @Override
        public final int hashCode() {
            return (x ^ (z << 12)) ^ (y << 24);
        }

        public final int getX() {
            return x;
        }

        public final int getY() {
            return y;
        }

        public final int getZ() {
            return z;
        }

        @Override
        public String toString() {
            return x + "," + y + "," + z;
        }

        @Override
        public boolean equals(Object obj) {
            Node other = (Node) obj;
            return other.x == x && other.z == z && other.y == y;
        }
    }

    public static Class<?> inject() {
        return BreadthFirstSearch.class;
    }
}
