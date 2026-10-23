package benchmark;

import algorithms.*;
import graph.Graph;
import java.nio.file.Path;
import java.util.Arrays;
import metrics.Metrics;

public final class Main {
    private Main() {}

    public static void main(String[] args) throws Exception {
        String command = args.length == 0 ? "demo" : args[0];
        if (command.equals("benchmark")) {
            boolean quick = args.length > 1 && args[1].equals("--quick");
            Path output = args.length > 2 ? Path.of(args[2]) : Path.of(quick ? "target/quick-results" : "results");
            Benchmark.run(quick, output);
        } else if (command.equals("demo")) {
            if (args.length > 1) demo(Path.of(args[1]));
            else for (String file : new String[]{"connected", "disconnected", "cyclic", "dag_negative"})
                demo(Path.of("data/" + file + ".txt"));
        } else throw new IllegalArgumentException("Usage: demo [file] | benchmark [--quick|--full] [output-directory]");
    }

    private static void demo(Path file) throws Exception {
        Graph graph = Graph.read(file);
        System.out.println("\n" + file + ": n=" + graph.n() + " m=" + graph.m());
        if (!graph.directed()) {
            for (MstResult result : new MstResult[]{Kruskal.run(graph, new Metrics()), Prim.run(graph, new Metrics())})
                System.out.println("MST/forest: weight=" + result.weight() + " components=" + result.components() + " edges=" + result.edges());
        } else {
            Metrics k = new Metrics(), t = new Metrics();
            SccResult scc = Kosaraju.run(graph, k), tarjan = Tarjan.run(graph, t);
            System.out.println("SCC count=" + scc.count() + " ids=" + Arrays.toString(scc.ids()));
            System.out.println("Tarjan agrees=" + scc.samePartition(tarjan) + "; scans: Kosaraju=" + k.edgeScans + " Tarjan=" + t.edgeScans);
            Kahn.Result order = Kahn.run(graph, new Metrics());
            if (order.hasCycle()) System.out.println("Cycle detected: no topological order or DAG shortest paths");
            else {
                System.out.println("Topological order=" + Arrays.toString(order.order()));
                PathResult shortest = DagShortestPath.run(graph, new Metrics());
                for (int v = 0; v < graph.n(); v++)
                    System.out.println("Vertex " + v + ": distance=" + shortest.distanceTo(v) + " path=" + shortest.pathTo(v));
            }
            Graph dag = Condensation.build(graph, scc);
            PathResult longest = Condensation.longestPaths(dag, new Metrics());
            int endpoint = -1;
            for (int v = 0; v < dag.n(); v++) if (endpoint == -1 || longest.distanceTo(v) > longest.distanceTo(endpoint)) endpoint = v;
            System.out.println("Condensation: vertices=" + dag.n() + " edges=" + dag.m());
            if (endpoint != -1)
                System.out.println("Critical component path=" + longest.pathTo(endpoint) + " weight=" + longest.distanceTo(endpoint));
        }
    }
}
