package algorithms;

import graph.Edge;
import graph.Graph;
import java.util.ArrayList;
import java.util.List;
import metrics.Metrics;

public final class Kruskal {
    private Kruskal() {}

    public static MstResult run(Graph graph, Metrics metrics) {
        graph.requireUndirected();
        Edge[] edges = graph.edges().toArray(Edge[]::new);
        sort(edges, metrics);
        DSU dsu = new DSU(graph.n(), metrics);
        List<Edge> chosen = new ArrayList<>();
        long weight = 0;
        for (Edge edge : edges) {
            if (dsu.components() <= 1) break;
            metrics.edgeScans++;
            if (dsu.union(edge.from(), edge.to())) {
                chosen.add(edge);
                weight += edge.weight();
            }
        }
        return new MstResult(weight, chosen, dsu.components());
    }

    private static void sort(Edge[] edges, Metrics metrics) {
        Edge[] buffer = new Edge[edges.length];
        for (long width = 1; width < edges.length; width *= 2) {
            for (long start = 0; start < edges.length; start += 2 * width) {
                int left = (int) start;
                int middle = (int) Math.min(start + width, edges.length);
                int right = (int) Math.min(start + 2 * width, edges.length);
                int i = left, j = middle, k = left;
                while (i < middle && j < right) {
                    metrics.comparisons++;
                    if (edges[i].weight() <= edges[j].weight()) buffer[k++] = edges[i++];
                    else buffer[k++] = edges[j++];
                }
                while (i < middle) buffer[k++] = edges[i++];
                while (j < right) buffer[k++] = edges[j++];
            }
            System.arraycopy(buffer, 0, edges, 0, edges.length);
        }
    }
}
