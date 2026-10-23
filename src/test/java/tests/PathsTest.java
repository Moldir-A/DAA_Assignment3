package tests;

import algorithms.*;
import graph.Graph;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import metrics.Metrics;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PathsTest {
    @Test void topologicalOrderAndCycleDetection() throws Exception {
        Graph dag = Graph.read(Path.of("data/dag_negative.txt"));
        Metrics metrics = new Metrics();
        var result = Kahn.run(dag, metrics);
        assertFalse(result.hasCycle());
        assertTrue(Kahn.valid(dag, result.order()));
        assertEquals(dag.m(), metrics.edgeScans);
        assertFalse(Kahn.valid(dag, new int[]{0, 0, 2, 3, 4, 5, 6}));
        Graph cycle = Graph.read(Path.of("data/cyclic.txt"));
        assertTrue(Kahn.run(cycle, new Metrics()).hasCycle());
        assertEquals(0, Kahn.run(cycle, new Metrics()).order().length);
        assertThrows(IllegalArgumentException.class, () -> DagShortestPath.run(cycle, new Metrics()));
        Graph loop = new Graph(1, true);
        loop.addEdge(0, 0, 1);
        assertTrue(Kahn.run(loop, new Metrics()).hasCycle());
    }

    @Test void negativeFixtureRestoresActualPath() throws Exception {
        Graph graph = Graph.read(Path.of("data/dag_negative.txt"));
        PathResult result = DagShortestPath.run(graph, new Metrics());
        assertArrayEquals(new long[]{0, 0, -2, 1, -4, 0, 0}, result.distances());
        assertEquals(List.of(1, 2, 3, 4), result.pathTo(4));
        assertEquals(List.of(6), result.pathTo(6));
        assertEquals(-5, result.sum());
    }

    @Test void hundredDagsAgreeWithBellmanFord() {
        Random random = new Random(33);
        for (int trial = 0; trial < 100; trial++) {
            int n = 2 + random.nextInt(45);
            int[] permutation = new int[n];
            for (int i = 0; i < n; i++) permutation[i] = i;
            for (int i = n - 1; i > 0; i--) {
                int j = random.nextInt(i + 1), t = permutation[i];
                permutation[i] = permutation[j]; permutation[j] = t;
            }
            Graph graph = new Graph(n, true);
            for (int i = 0; i < 5 * n; i++) {
                int a = random.nextInt(n), b = random.nextInt(n);
                if (a == b) continue;
                graph.addEdge(permutation[Math.min(a, b)], permutation[Math.max(a, b)], random.nextInt(101) - 50);
            }
            long[] expected = new long[n];
            Arrays.fill(expected, Long.MAX_VALUE);
            int[] degree = graph.indegrees();
            for (int i = 0; i < n; i++) if (degree[i] == 0) expected[i] = 0;
            for (int pass = 1; pass < n; pass++) {
                boolean changed = false;
                for (var edge : graph.edges()) {
                    if (expected[edge.from()] == Long.MAX_VALUE) continue;
                    long candidate = expected[edge.from()] + edge.weight();
                    if (candidate < expected[edge.to()]) { expected[edge.to()] = candidate; changed = true; }
                }
                if (!changed) break;
            }
            Metrics metrics = new Metrics();
            PathResult actual = DagShortestPath.run(graph, metrics);
            assertArrayEquals(expected, actual.distances());
            assertEquals(graph.m(), metrics.edgeScans);
            assertTrue(Kahn.valid(graph, Kahn.run(graph, new Metrics()).order()));
            for (int v = 0; v < n; v++) {
                var path = actual.pathTo(v);
                assertEquals(0, degree[path.get(0)]);
                for (int i = 1; i < path.size(); i++) {
                    int a = path.get(i - 1), b = path.get(i);
                    assertTrue(graph.adjacent(a).stream().anyMatch(e -> e.to() == b && actual.distanceTo(a) + e.weight() == actual.distanceTo(b)));
                }
            }
        }
    }

    @Test void emptyGraphAndLongDistances() {
        assertEquals(0, DagShortestPath.run(new Graph(0, true), new Metrics()).sum());
        assertTrue(Kahn.valid(new Graph(0, true), new int[0]));
        Graph graph = new Graph(3, true);
        graph.addEdge(0, 1, Integer.MAX_VALUE);
        graph.addEdge(1, 2, Integer.MAX_VALUE);
        assertEquals(2L * Integer.MAX_VALUE, DagShortestPath.run(graph, new Metrics()).distanceTo(2));
    }

    @Test void condensationAndCriticalPath() throws Exception {
        Graph graph = Graph.read(Path.of("data/cyclic.txt"));
        SccResult scc = Kosaraju.run(graph, new Metrics());
        Graph dag = Condensation.build(graph, scc);
        assertEquals(4, dag.n());
        assertEquals(2, dag.m());
        assertTrue(Kahn.valid(dag, Kahn.run(dag, new Metrics()).order()));
        PathResult longest = Condensation.longestPaths(dag, new Metrics());
        assertEquals(9, longest.distanceTo(scc.componentOf(5)));
        assertEquals(3, longest.pathTo(scc.componentOf(5)).size());
    }

    @Test void longestPathMatchesExhaustiveEnumeration() {
        Random random = new Random(122);
        for (int trial = 0; trial < 50; trial++) {
            Graph graph = new Graph(8, true);
            for (int a = 0; a < 8; a++) for (int b = a + 1; b < 8; b++)
                if (random.nextBoolean()) graph.addEdge(a, b, random.nextInt(21) - 10);
            long[] expected = new long[8];
            Arrays.fill(expected, Long.MIN_VALUE);
            int[] indegree = graph.indegrees();
            for (int v = 0; v < 8; v++) if (indegree[v] == 0) enumerate(graph, v, 0, expected);
            assertArrayEquals(expected, Condensation.longestPaths(graph, new Metrics()).distances());
        }
    }

    private void enumerate(Graph graph, int v, long length, long[] best) {
        best[v] = Math.max(best[v], length);
        for (var edge : graph.adjacent(v)) enumerate(graph, edge.to(), length + edge.weight(), best);
    }
}
