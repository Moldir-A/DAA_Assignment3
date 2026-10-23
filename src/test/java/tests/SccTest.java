package tests;

import algorithms.*;
import graph.Graph;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Random;
import metrics.Metrics;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SccTest {
    private boolean[] reachable(Graph graph, int start) {
        boolean[] visited = new boolean[graph.n()];
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        queue.add(start);
        visited[start] = true;
        while (!queue.isEmpty()) {
            int v = queue.remove();
            for (var edge : graph.adjacent(v)) if (!visited[edge.to()]) {
                visited[edge.to()] = true;
                queue.add(edge.to());
            }
        }
        return visited;
    }

    @Test void randomGraphsAgreeWithBfsBothDirections() {
        Random random = new Random(91);
        for (int trial = 0; trial < 60; trial++) {
            int n = trial == 59 ? 200 : 1 + random.nextInt(60);
            Graph graph = new Graph(n, true);
            int m = random.nextInt(6 * n);
            for (int i = 0; i < m; i++) graph.addEdge(random.nextInt(n), random.nextInt(n), 1);
            Metrics km = new Metrics(), tm = new Metrics();
            SccResult k = Kosaraju.run(graph, km), t = Tarjan.run(graph, tm);
            assertTrue(k.samePartition(t));
            assertEquals(2L * m, km.edgeScans);
            assertEquals(m, tm.edgeScans);
            Graph reverse = graph.reversed();
            for (int u = 0; u < n; u++) {
                boolean[] forward = reachable(graph, u), backward = reachable(reverse, u);
                for (int v = 0; v < n; v++)
                    assertEquals(forward[v] && backward[v], k.componentOf(u) == k.componentOf(v));
            }
        }
    }

    @Test void cyclicFixtureAndEmptyGraph() throws Exception {
        Graph graph = Graph.read(Path.of("data/cyclic.txt"));
        SccResult result = Kosaraju.run(graph, new Metrics());
        assertEquals(4, result.count());
        assertTrue(result.samePartition(Tarjan.run(graph, new Metrics())));
        assertEquals(result.componentOf(0), result.componentOf(2));
        assertNotEquals(result.componentOf(4), result.componentOf(5));
        assertEquals(0, Kosaraju.run(new Graph(0, true), new Metrics()).count());
        assertEquals(0, Tarjan.run(new Graph(0, true), new Metrics()).count());
    }

    @Test void hundredThousandVertexPathIsStackSafe() {
        Graph graph = new Graph(100_000, true);
        for (int i = 1; i < graph.n(); i++) graph.addEdge(i - 1, i, 1);
        Metrics km = new Metrics(), tm = new Metrics();
        assertEquals(100_000, Kosaraju.run(graph, km).count());
        assertEquals(100_000, Tarjan.run(graph, tm).count());
        assertEquals(100_000, km.maxDepth);
        assertEquals(100_000, tm.maxDepth);
        assertEquals(199_998, km.edgeScans);
        assertEquals(99_999, tm.edgeScans);
    }
}
