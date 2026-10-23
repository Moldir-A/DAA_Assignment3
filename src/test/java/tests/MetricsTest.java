package tests;

import algorithms.*;
import benchmark.Generator;
import graph.Graph;
import metrics.Metrics;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MetricsTest {
    @Test void referenceValuesAtBothSmallDensities() {
        long[][] reference = {{4000,147970,27,396274}, {20000,30363,1,252411}};
        for (long[] expected : reference) {
            int m = (int) expected[0];
            Graph u = Generator.generate(1000, m, Generator.Kind.UNDIRECTED);
            Metrics k = new Metrics(), p = new Metrics();
            assertEquals(expected[1], Kruskal.run(u, k).weight());
            assertEquals(expected[1], Prim.run(u, p).weight());
            assertEquals(2L * k.edgeScans, k.findCalls);
            assertEquals(999, k.successfulUnions);
            assertEquals(2L * m, p.edgeScans);
            assertTrue(k.comparisons > 0);
            assertTrue(p.comparisons > 0);
            Graph d = Generator.generate(1000, m, Generator.Kind.DIRECTED);
            Metrics s = new Metrics();
            assertEquals(expected[2], Kosaraju.run(d, s).count());
            assertEquals(2L * m, s.edgeScans);
            Graph dag = Generator.generate(1000, m, Generator.Kind.DAG);
            Metrics paths = new Metrics(), top = new Metrics();
            assertEquals(expected[3], DagShortestPath.run(dag, paths).sum());
            assertTrue(Kahn.valid(dag, Kahn.run(dag, top).order()));
            assertEquals(m, paths.edgeScans);
            assertEquals(m, top.edgeScans);
        }
    }

    @Test void generatorRestartsSeedAndAlgorithmsDoNotChangeGraph() {
        for (Generator.Kind kind : Generator.Kind.values()) {
            Graph a = Generator.generate(50, 300, kind), b = Generator.generate(50, 300, kind);
            assertEquals(a.edges(), b.edges());
            if (kind == Generator.Kind.UNDIRECTED) {
                Kruskal.run(a, new Metrics());
                Prim.run(a, new Metrics());
            } else {
                Kosaraju.run(a, new Metrics());
                Tarjan.run(a, new Metrics());
                if (kind == Generator.Kind.DAG) DagShortestPath.run(a, new Metrics());
            }
            assertEquals(a.edges(), b.edges());
            assertArrayEquals(a.indegrees(), b.indegrees());
        }
    }

    @Test void condensationKeepsBestParallelTransitionForLongestPath() {
        Graph graph = new Graph(4, true);
        graph.addEdge(0, 1, 1);
        graph.addEdge(1, 0, 1);
        graph.addEdge(0, 2, 3);
        graph.addEdge(1, 2, 7);
        graph.addEdge(2, 3, -2);
        SccResult scc = Kosaraju.run(graph, new Metrics());
        Graph dag = Condensation.build(graph, scc);
        assertEquals(2, dag.m());
        assertEquals(5, Condensation.longestPaths(dag, new Metrics()).distanceTo(scc.componentOf(3)));
    }
}
