package graph;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Graph {
    private final boolean directed;
    private final List<List<Edge>> adjacency;
    private final List<List<Edge>> views;
    private final List<Edge> edges = new ArrayList<>();
    private final int[] indegrees;

    public Graph(int n, boolean directed) {
        if (n < 0) throw new IllegalArgumentException("Negative vertex count");
        this.directed = directed;
        adjacency = new ArrayList<>(n);
        views = new ArrayList<>(n);
        indegrees = new int[n];
        for (int i = 0; i < n; i++) {
            List<Edge> list = new ArrayList<>();
            adjacency.add(list);
            views.add(Collections.unmodifiableList(list));
        }
    }

    public int n() { return adjacency.size(); }
    public int m() { return edges.size(); }
    public boolean directed() { return directed; }
    public List<Edge> edges() { return Collections.unmodifiableList(edges); }
    public List<Edge> adjacent(int v) { checkVertex(v); return views.get(v); }
    public int[] indegrees() { return indegrees.clone(); }

    public void addEdge(int u, int v, int weight) {
        checkVertex(u);
        checkVertex(v);
        Edge edge = new Edge(u, v, weight);
        edges.add(edge);
        adjacency.get(u).add(edge);
        indegrees[v]++;
        if (!directed) {
            adjacency.get(v).add(new Edge(v, u, weight));
            indegrees[u]++;
        }
    }

    public Graph reversed() {
        Graph result = new Graph(n(), directed);
        for (Edge e : edges) result.addEdge(e.to(), e.from(), e.weight());
        return result;
    }

    public void requireDirected() {
        if (!directed) throw new IllegalArgumentException("A directed graph is required");
    }

    public void requireUndirected() {
        if (directed) throw new IllegalArgumentException("An undirected graph is required");
    }

    public void checkVertex(int v) {
        if (v < 0 || v >= n()) throw new IllegalArgumentException("Invalid vertex: " + v);
    }

    public static Graph read(Path path) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String first = reader.readLine();
            if (first == null) throw new IllegalArgumentException("Missing graph header");
            String[] header = first.trim().split("\\s+");
            if (header.length != 3) throw new IllegalArgumentException("Expected n m directed|undirected");
            int n = Integer.parseInt(header[0]);
            int m = Integer.parseInt(header[1]);
            if (m < 0) throw new IllegalArgumentException("Negative edge count");
            if (!header[2].equals("directed") && !header[2].equals("undirected"))
                throw new IllegalArgumentException("Invalid graph type");
            Graph graph = new Graph(n, header[2].equals("directed"));
            for (int i = 0; i < m; i++) {
                String line = reader.readLine();
                if (line == null) throw new IllegalArgumentException("Missing edge " + i);
                String[] parts = line.trim().split("\\s+");
                if (parts.length != 3) throw new IllegalArgumentException("Expected u v w");
                graph.addEdge(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
            }
            String line;
            while ((line = reader.readLine()) != null)
                if (!line.isBlank()) throw new IllegalArgumentException("Extra input after m edges");
            return graph;
        }
    }
}
