package algorithms;

import graph.Edge;
import graph.Graph;
import java.util.Arrays;
import metrics.Metrics;

public final class DagShortestPath {
    private DagShortestPath() {}
    public static final long INF = Long.MAX_VALUE;

    public static PathResult run(Graph graph, Metrics metrics) {
        Kahn.Result order = Kahn.run(graph, new Metrics());
        if (order.hasCycle()) throw new IllegalArgumentException("Shortest paths require a DAG; cycle detected");
        long[] distance = new long[graph.n()];
        int[] parent = new int[graph.n()];
        Arrays.fill(distance, INF);
        Arrays.fill(parent, -1);
        int[] indegree = graph.indegrees();
        for (int v = 0; v < graph.n(); v++) if (indegree[v] == 0) distance[v] = 0;
        for (int v : order.order()) {
            for (Edge edge : graph.adjacent(v)) {
                metrics.edgeScans++;
                long candidate = distance[v] + edge.weight();
                if (candidate < distance[edge.to()]) {
                    distance[edge.to()] = candidate;
                    parent[edge.to()] = v;
                }
            }
        }
        return new PathResult(distance, parent);
    }
}
