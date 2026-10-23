package benchmark;

import graph.Graph;
import java.util.Random;

public final class Generator {
    private Generator() {}
    public enum Kind { UNDIRECTED, DIRECTED, DAG }

    public static Graph generate(int n, int m, Kind kind) {
        if (n < 2 || m < 0 || (kind == Kind.UNDIRECTED && m < n - 1))
            throw new IllegalArgumentException("Invalid generator size");
        Random random = new Random(42);
        Graph graph = new Graph(n, kind != Kind.UNDIRECTED);
        int[] rank = new int[n];
        for (int i = 0; i < n; i++) rank[i] = i;
        if (kind != Kind.DIRECTED) {
            for (int i = n - 1; i > 0; i--) {
                int j = random.nextInt(i + 1), t = rank[i];
                rank[i] = rank[j];
                rank[j] = t;
            }
        }
        if (kind == Kind.UNDIRECTED)
            for (int i = 1; i < n; i++)
                graph.addEdge(rank[random.nextInt(i)], rank[i], 1 + random.nextInt(1000));
        while (graph.m() < m) {
            int a = random.nextInt(n), b;
            do { b = random.nextInt(n); } while (b == a);
            if (kind == Kind.DAG && rank[a] > rank[b]) { int t = a; a = b; b = t; }
            graph.addEdge(a, b, 1 + random.nextInt(1000));
        }
        return graph;
    }
}
