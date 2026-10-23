package tests;

import algorithms.*;
import graph.Graph;
import java.nio.file.Path;
import java.util.Random;
import metrics.Metrics;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MstTest {
    @Test void hundredConnectedGraphsAgree() {
        Random random = new Random(15);
        for (int trial = 0; trial < 100; trial++) {
            int n = 2 + random.nextInt(70);
            Graph graph = new Graph(n, false);
            for (int v = 1; v < n; v++) graph.addEdge(v, random.nextInt(v), random.nextInt(101) - 50);
            for (int i = 0; i < 4 * n; i++)
                graph.addEdge(random.nextInt(n), random.nextInt(n), random.nextInt(101) - 50);
            MstResult k = Kruskal.run(graph, new Metrics());
            MstResult p = Prim.run(graph, new Metrics());
            assertEquals(k.weight(), p.weight());
            for (MstResult result : new MstResult[]{k, p}) {
                assertEquals(n - 1, result.edges().size());
                assertEquals(1, result.components());
                DSU check = new DSU(n);
                result.edges().forEach(e -> assertTrue(check.union(e.from(), e.to())));
                assertEquals(1, check.components());
            }
        }
    }

    @Test void fixturesLoopsParallelEdgesAndIsolatedVertex() throws Exception {
        Graph connected = Graph.read(Path.of("data/connected.txt"));
        Graph disconnected = Graph.read(Path.of("data/disconnected.txt"));
        assertEquals(10, Kruskal.run(connected, new Metrics()).weight());
        for (MstResult r : new MstResult[]{Kruskal.run(disconnected, new Metrics()), Prim.run(disconnected, new Metrics())}) {
            assertEquals(8, r.weight());
            assertEquals(3, r.components());
            assertEquals(3, r.edges().size());
        }
    }

    @Test void agreesWithExhaustiveSubsetsOnTinyGraphs() {
        Random random = new Random(817);
        for (int trial = 0; trial < 35; trial++) {
            Graph graph = new Graph(5, false);
            for (int v = 1; v < 5; v++) graph.addEdge(v - 1, v, random.nextInt(21) - 10);
            for (int i = 0; i < 5; i++) graph.addEdge(random.nextInt(5), random.nextInt(5), random.nextInt(21) - 10);
            long best = Long.MAX_VALUE;
            for (int mask = 0; mask < (1 << graph.m()); mask++) {
                if (Integer.bitCount(mask) != 4) continue;
                DSU dsu = new DSU(5);
                long sum = 0;
                for (int i = 0; i < graph.m(); i++) if ((mask & (1 << i)) != 0) {
                    var edge = graph.edges().get(i);
                    dsu.union(edge.from(), edge.to());
                    sum += edge.weight();
                }
                if (dsu.components() == 1) best = Math.min(best, sum);
            }
            assertEquals(best, Kruskal.run(graph, new Metrics()).weight());
            assertEquals(best, Prim.run(graph, new Metrics()).weight());
        }
    }

    @Test void emptySingleAndLongTotals() {
        assertEquals(0, Kruskal.run(new Graph(0, false), new Metrics()).components());
        assertEquals(0, Prim.run(new Graph(0, false), new Metrics()).components());
        assertEquals(1, Prim.run(new Graph(1, false), new Metrics()).components());
        Graph graph = new Graph(3, false);
        graph.addEdge(0, 1, Integer.MAX_VALUE);
        graph.addEdge(1, 2, Integer.MAX_VALUE);
        assertEquals(2L * Integer.MAX_VALUE, Kruskal.run(graph, new Metrics()).weight());
        assertEquals(2L * Integer.MAX_VALUE, Prim.run(graph, new Metrics()).weight());
        assertThrows(IllegalArgumentException.class, () -> Prim.run(new Graph(2, true), new Metrics()));
        assertThrows(IllegalArgumentException.class, () -> Kruskal.run(new Graph(2, true), new Metrics()));
    }
}
