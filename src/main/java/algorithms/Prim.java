package algorithms;

import graph.Edge;
import graph.Graph;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import metrics.Metrics;

public final class Prim {
    private Prim() {}

    public static MstResult run(Graph graph, Metrics metrics) {
        graph.requireUndirected();
        boolean[] visited = new boolean[graph.n()];
        List<Edge> chosen = new ArrayList<>();
        PriorityQueue<Edge> queue = new PriorityQueue<>((a, b) -> {
            metrics.comparisons++;
            int order = Integer.compare(a.weight(), b.weight());
            if (order == 0) order = Integer.compare(a.from(), b.from());
            if (order == 0) order = Integer.compare(a.to(), b.to());
            return order;
        });
        long weight = 0;
        int components = 0;
        for (int root = 0; root < graph.n(); root++) {
            if (visited[root]) continue;
            components++;
            visit(graph, root, visited, queue, metrics);
            while (!queue.isEmpty()) {
                Edge edge = queue.remove();
                if (visited[edge.to()]) continue;
                weight += edge.weight();
                chosen.add(edge);
                visit(graph, edge.to(), visited, queue, metrics);
            }
        }
        return new MstResult(weight, chosen, components);
    }

    private static void visit(Graph graph, int v, boolean[] visited,
                              PriorityQueue<Edge> queue, Metrics metrics) {
        visited[v] = true;
        for (Edge edge : graph.adjacent(v)) {
            metrics.edgeScans++;
            if (!visited[edge.to()]) queue.add(edge);
        }
    }
}
