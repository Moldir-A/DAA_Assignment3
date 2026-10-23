package benchmark;

import algorithms.*;
import graph.Graph;
import metrics.Metrics;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.ToLongFunction;

public final class Benchmark {
    private Benchmark() {}
    public static final int[] SIZES = {1000, 3162, 10000, 31623, 100000};
    public static final long[][] REFERENCES = {
        {147970,27,396274}, {30363,1,252411},
        {468911,137,1271911}, {97589,1,723157},
        {1486139,373,3941828}, {309550,1,2381342},
        {4715515,1188,12551891}, {968517,1,7376612},
        {15012094,3923,40096539}, {3056681,1,23512087}
    };
    private static final String HEADER = "algorithm,graph,n,m,time_ms,dsu_ops,edge_scans,comparisons,max_depth,result";
    private record Measurement(double milliseconds, Metrics metrics, long value) {}

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private static <T> Measurement measure(Function<Metrics,T> algorithm, ToLongFunction<T> value, Consumer<T> validate) {
        validate.accept(algorithm.apply(new Metrics()));
        long[] nanos = new long[5];
        Metrics first = null;
        long firstValue = 0;
        for (int run = 0; run < 5; run++) {
            Metrics metrics = new Metrics();
            long start = System.nanoTime();
            T result = algorithm.apply(metrics);
            nanos[run] = System.nanoTime() - start;
            validate.accept(result);
            long actual = value.applyAsLong(result);
            if (first == null) { first = metrics; firstValue = actual; }
            else require(first.counters().equals(metrics.counters()) && firstValue == actual, "Non-deterministic result or counters");
        }
        Arrays.sort(nanos);
        return new Measurement(nanos[2] / 1_000_000.0, first, firstValue);
    }

    private static String row(String algorithm, String kind, int n, int m, Measurement result) {
        return String.format(Locale.ROOT, "%s,%s,%d,%d,%.6f,%s,%d", algorithm, kind, n, m,
                result.milliseconds, result.metrics.counters(), result.value);
    }

    private static void mst(int n, int m, String density, long expected, List<String> rows) {
        Graph graph = Generator.generate(n, m, Generator.Kind.UNDIRECTED);
        Consumer<MstResult> valid = r -> require(r.weight() == expected && r.components() == 1 && r.edges().size() == n - 1,
                "MST reference or forest invariant failed at " + n + "/" + m + ": " + r.weight());
        Measurement k = measure(metrics -> Kruskal.run(graph, metrics), MstResult::weight, valid);
        Measurement p = measure(metrics -> Prim.run(graph, metrics), MstResult::weight, valid);
        require(k.value == p.value, "Kruskal and Prim disagree");
        require(p.metrics.edgeScans == 2L * m, "Prim must scan both adjacency entries");
        rows.add(row("Kruskal", "undirected_" + density, n, m, k));
        rows.add(row("Prim", "undirected_" + density, n, m, p));
    }

    private static void scc(int n, int m, String density, long expected, List<String> rows, List<String> bonus) {
        Graph graph = Generator.generate(n, m, Generator.Kind.DIRECTED);
        SccResult reference = Kosaraju.run(graph, new Metrics());
        Consumer<SccResult> valid = r -> require(r.count() == expected && r.samePartition(reference), "SCC partition/reference mismatch");
        Measurement k = measure(metrics -> Kosaraju.run(graph, metrics), SccResult::count, valid);
        Measurement t = measure(metrics -> Tarjan.run(graph, metrics), SccResult::count, valid);
        require(k.metrics.edgeScans == 2L * m, "Kosaraju must scan 2m edges");
        require(t.metrics.edgeScans == m, "Tarjan must scan m edges");
        rows.add(row("Kosaraju", "directed_" + density, n, m, k));
        bonus.add(row("Tarjan", "directed_" + density, n, m, t));
        Graph condensed = Condensation.build(graph, reference);
        require(Kahn.valid(condensed, Kahn.run(condensed, new Metrics()).order()), "Condensation must be acyclic");
        Condensation.longestPaths(condensed, new Metrics());
    }

    private static void dag(int n, int m, String density, long expected, List<String> rows) {
        Graph graph = Generator.generate(n, m, Generator.Kind.DAG);
        Measurement k = measure(metrics -> Kahn.run(graph, metrics), r -> r.order().length,
                r -> require(!r.hasCycle() && Kahn.valid(graph, r.order()), "Invalid topological order"));
        Measurement d = measure(metrics -> DagShortestPath.run(graph, metrics), PathResult::sum,
                r -> require(r.sum() == expected, "DAG distance reference mismatch at " + n + "/" + m + ": " + r.sum()));
        require(k.metrics.edgeScans == m && d.metrics.edgeScans == m, "Kahn and DAG relaxation must each scan m edges");
        rows.add(row("Kahn", "dag_" + density, n, m, k));
        rows.add(row("DagShortestPath", "dag_" + density, n, m, d));
    }

    public static void run(boolean quick, Path output) throws IOException {
        List<String> rows = new ArrayList<>(), bonus = new ArrayList<>(), referenceLog = new ArrayList<>();
        rows.add(HEADER);
        bonus.add(HEADER);
        int limit = quick ? 1 : SIZES.length;
        for (int i = 0; i < limit; i++) {
            int n = SIZES[i];
            for (int density = 0; density < 2; density++) {
                int m = n * (density == 0 ? 4 : 20);
                String label = density == 0 ? "sparse" : "dense";
                long[] expected = REFERENCES[2 * i + density];
                System.out.println("Measuring " + n + " / " + m + " (one warm-up, five measured runs)");
                mst(n, m, label, expected[0], rows);
                scc(n, m, label, expected[1], rows, bonus);
                dag(n, m, label, expected[2], rows);
                String line = String.format(Locale.ROOT, "%d / %d: MST=%d SCC=%d DAG_SUM=%d OK", n, m, expected[0], expected[1], expected[2]);
                referenceLog.add(line);
                System.out.println(line);
            }
        }
        require(rows.size() == 1 + 10 * limit, "Wrong CSV row count");
        Files.createDirectories(output);
        write(output.resolve("results.csv"), rows);
        write(output.resolve("bonus.csv"), bonus);
        write(output.resolve("reference_check.txt"), referenceLog);
        write(output.resolve("environment.txt"), List.of(
            "Java: " + System.getProperty("java.version"),
            "VM: " + System.getProperty("java.vm.name"),
            "OS: " + System.getProperty("os.name") + " " + System.getProperty("os.arch"),
            "Available processors: " + Runtime.getRuntime().availableProcessors(),
            "Max heap bytes: " + Runtime.getRuntime().maxMemory(),
            "Timing: one warm-up, median of five runs; generation, validation and file IO excluded",
            "DagShortestPath timing includes Kahn preprocessing; its edgeScans counts relaxation only",
            "Counters are observed inside algorithms, not reconstructed from formulas"));
        System.out.println("PASS: " + (rows.size() - 1) + " core rows and " + (bonus.size() - 1) + " bonus rows saved to " + output);
    }

    private static void write(Path file, List<String> lines) throws IOException {
        Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
        Files.write(temporary, lines);
        Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
    }
}
