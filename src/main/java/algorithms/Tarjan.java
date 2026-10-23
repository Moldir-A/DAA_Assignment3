package algorithms;

import graph.Graph;
import java.util.Arrays;
import metrics.Metrics;

public final class Tarjan {
    private Tarjan() {}

    public static SccResult run(Graph graph, Metrics metrics) {
        graph.requireDirected();
        int n = graph.n();
        int[] index = new int[n], low = new int[n], id = new int[n];
        int[] frames = new int[n], next = new int[n], active = new int[n];
        boolean[] onStack = new boolean[n];
        Arrays.fill(index, -1);
        int clock = 0, count = 0, activeSize = 0;
        for (int root = 0; root < n; root++) {
            if (index[root] != -1) continue;
            int depth = 0;
            frames[depth++] = root;
            index[root] = low[root] = clock++;
            active[activeSize++] = root;
            onStack[root] = true;
            metrics.depth(depth);
            while (depth > 0) {
                int v = frames[depth - 1];
                if (next[v] < graph.adjacent(v).size()) {
                    int w = graph.adjacent(v).get(next[v]++).to();
                    metrics.edgeScans++;
                    if (index[w] == -1) {
                        frames[depth++] = w;
                        index[w] = low[w] = clock++;
                        active[activeSize++] = w;
                        onStack[w] = true;
                        metrics.depth(depth);
                    } else if (onStack[w]) low[v] = Math.min(low[v], index[w]);
                } else {
                    depth--;
                    if (low[v] == index[v]) {
                        int w;
                        do {
                            w = active[--activeSize];
                            onStack[w] = false;
                            id[w] = count;
                        } while (w != v);
                        count++;
                    }
                    if (depth > 0) {
                        int parent = frames[depth - 1];
                        low[parent] = Math.min(low[parent], low[v]);
                    }
                }
            }
        }
        return new SccResult(id, count);
    }
}
