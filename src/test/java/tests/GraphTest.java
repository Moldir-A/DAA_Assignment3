package tests;

import graph.Graph;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class GraphTest {
    @TempDir Path temporary;

    @Test void inputPreservesLoopsParallelEdgesAndDegrees() throws Exception {
        Graph graph = Graph.read(Path.of("data/cyclic.txt"));
        assertEquals(7, graph.n());
        assertEquals(9, graph.m());
        assertTrue(graph.directed());
        assertEquals(2, graph.adjacent(0).size());
        assertEquals(2, graph.indegrees()[1]);
        assertEquals(0, graph.adjacent(6).size());
        Graph reverse = graph.reversed();
        for (var e : graph.edges())
            assertTrue(reverse.adjacent(e.to()).stream().anyMatch(r -> r.to() == e.from() && r.weight() == e.weight()));
        assertThrows(UnsupportedOperationException.class, () -> graph.edges().clear());
        assertThrows(UnsupportedOperationException.class, () -> graph.adjacent(0).clear());
        int[] degree = graph.indegrees();
        degree[1] = 900;
        assertEquals(2, graph.indegrees()[1]);
    }

    @Test void invalidInputsFailClearly() throws Exception {
        for (String input : new String[]{"", "3 1 wrong\n0 1 2\n", "3 1 directed\n", "3 1 directed\n0 3 2\n",
                "3 -1 directed\n", "3 0 directed\n0 1 2\n", "3 1 directed\n0 1\n", "3 directed\n"}) {
            Path path = temporary.resolve("invalid.txt");
            Files.writeString(path, input);
            assertThrows(IllegalArgumentException.class, () -> Graph.read(path));
        }
        assertThrows(IllegalArgumentException.class, () -> new Graph(-1, true));
    }

    @Test void undirectedLoopHasTwoAdjacencyEntries() {
        Graph graph = new Graph(2, false);
        graph.addEdge(0, 0, 5);
        graph.addEdge(0, 1, 2);
        assertEquals(2, graph.m());
        assertEquals(3, graph.adjacent(0).size());
        assertEquals(1, graph.adjacent(1).size());
        assertArrayEquals(new int[]{3, 1}, graph.indegrees());
    }
}
