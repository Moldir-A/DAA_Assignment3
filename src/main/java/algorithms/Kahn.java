package algorithms;

import graph.Edge;
import graph.Graph;
import java.util.ArrayDeque;
import java.util.Arrays;
import metrics.Metrics;

public final class Kahn {
    private Kahn() {}
    public record Result(int[] order, boolean hasCycle) {}

    public static Result run(Graph graph, Metrics metrics) {
        graph.requireDirected();
        int[] degree = graph.indegrees();
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        for (int v = 0; v < graph.n(); v++) if (degree[v] == 0) queue.add(v);
        int[] order = new int[graph.n()];
        int count = 0;
        while (!queue.isEmpty()) {
            int v = queue.remove();
            order[count++] = v;
            for (Edge edge : graph.adjacent(v)) {
                metrics.edgeScans++;
                if (--degree[edge.to()] == 0) queue.add(edge.to());
            }
        }
        return count == graph.n() ? new Result(order, false) : new Result(new int[0], true);
    }

    public static boolean valid(Graph graph, int[] order) {
        if (!graph.directed() || order.length != graph.n()) return false;
        int[] position = new int[graph.n()];
        Arrays.fill(position, -1);
        for (int i = 0; i < order.length; i++) {
            int v = order[i];
            if (v < 0 || v >= graph.n() || position[v] != -1) return false;
            position[v] = i;
        }
        for (Edge e : graph.edges()) if (position[e.from()] >= position[e.to()]) return false;
        return true;
    }
}
