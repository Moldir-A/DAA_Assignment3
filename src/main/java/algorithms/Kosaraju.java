package algorithms;

import graph.Graph;
import java.util.Arrays;
import metrics.Metrics;

public final class Kosaraju {
    private Kosaraju() {}

    public static SccResult run(Graph graph, Metrics metrics) {
        graph.requireDirected();
        int n = graph.n();
        boolean[] seen = new boolean[n];
        int[] stack = new int[n], next = new int[n], finish = new int[n];
        int finished = 0;
        for (int root = 0; root < n; root++) {
            if (seen[root]) continue;
            int top = 0;
            stack[top++] = root;
            seen[root] = true;
            metrics.depth(top);
            while (top > 0) {
                int v = stack[top - 1];
                if (next[v] < graph.adjacent(v).size()) {
                    int w = graph.adjacent(v).get(next[v]++).to();
                    metrics.edgeScans++;
                    if (!seen[w]) {
                        seen[w] = true;
                        stack[top++] = w;
                        metrics.depth(top);
                    }
                } else {
                    finish[finished++] = v;
                    top--;
                }
            }
        }
        Graph reverse = graph.reversed();
        int[] id = new int[n];
        Arrays.fill(id, -1);
        Arrays.fill(next, 0);
        int count = 0;
        for (int i = n - 1; i >= 0; i--) {
            int root = finish[i];
            if (id[root] != -1) continue;
            int top = 0;
            stack[top++] = root;
            id[root] = count;
            metrics.depth(top);
            while (top > 0) {
                int v = stack[top - 1];
                if (next[v] < reverse.adjacent(v).size()) {
                    int w = reverse.adjacent(v).get(next[v]++).to();
                    metrics.edgeScans++;
                    if (id[w] == -1) {
                        id[w] = count;
                        stack[top++] = w;
                        metrics.depth(top);
                    }
                } else top--;
            }
            count++;
        }
        return new SccResult(id, count);
    }
}
