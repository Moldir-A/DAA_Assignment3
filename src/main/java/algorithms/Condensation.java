package algorithms;

import graph.Edge;
import graph.Graph;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;
import metrics.Metrics;

public final class Condensation {
    private Condensation() {}

    public static Graph build(Graph graph, SccResult scc) {
        graph.requireDirected();
        Map<Long, Integer> maximumWeight = new TreeMap<>();
        for (Edge edge : graph.edges()) {
            int a = scc.componentOf(edge.from()), b = scc.componentOf(edge.to());
            if (a == b) continue;
            long key = ((long) a << 32) | (b & 0xffffffffL);
            maximumWeight.merge(key, edge.weight(), Math::max);
        }
        Graph dag = new Graph(scc.count(), true);
        for (var entry : maximumWeight.entrySet()) {
            int a = (int) (entry.getKey() >>> 32), b = (int) entry.getKey().longValue();
            dag.addEdge(a, b, entry.getValue());
        }
        return dag;
    }

    public static PathResult longestPaths(Graph dag, Metrics metrics) {
        Kahn.Result order = Kahn.run(dag, new Metrics());
        if (order.hasCycle()) throw new IllegalArgumentException("Critical path requires a DAG");
        long[] distance = new long[dag.n()];
        int[] parent = new int[dag.n()];
        Arrays.fill(distance, Long.MIN_VALUE);
        Arrays.fill(parent, -1);
        int[] indegree = dag.indegrees();
        for (int v = 0; v < dag.n(); v++) if (indegree[v] == 0) distance[v] = 0;
        for (int v : order.order()) for (Edge edge : dag.adjacent(v)) {
            metrics.edgeScans++;
            long candidate = distance[v] + edge.weight();
            if (candidate > distance[edge.to()]) {
                distance[edge.to()] = candidate;
                parent[edge.to()] = v;
            }
        }
        return new PathResult(distance, parent);
    }
}
