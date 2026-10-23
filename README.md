# City Network and Job Scheduler

DAA Assignment 3. **Moldir Amanbek, SE-2521.**

Java 17 / Maven project implementing DSU, Kruskal, lazy priority-queue Prim, iterative Kosaraju, Kahn, and multi-source DAG shortest paths. Both bonus tasks are included: iterative Tarjan and condensation with a longest component path.

## Verified results

- 21 JUnit 5 tests passed, with zero failures, errors, or skipped tests.
- All 30 values in the assignment's reference table matched exactly.
- `results/results.csv` has 50 rows, excluding its header.
- `results/bonus.csv` has 10 additional Tarjan rows.
- `REPORT.pdf` is the five-page report; `REPORT.md` contains its editable source text.
- Three PNG charts are in `results/plots/`.
- The 100,000-vertex path test passes with `-Xss256k`.

## Requirements

- JDK 17 or later, with `java` and Maven 3.9+ available in the terminal.
- Internet access for Maven's first dependency download.
- Python 3.10+ is needed only to regenerate plots and the report.
- Allow about 2 GiB of Java heap for the complete benchmark. The largest case is 100,000 vertices and 2,000,000 edges.

Run all commands from this project directory, the directory containing `pom.xml`.

## Build and tests

```bash
mvn clean verify
```

JUnit results appear in `target/surefire-reports/`. The executable JAR is `target/city-network-1.0.jar`. Algorithm implementations have no external runtime dependencies; JUnit is a test-only dependency. A prebuilt copy is also provided in `dist/city-network-1.0.jar` for immediate demonstration.

## Run the demo

```bash
java -jar target/city-network-1.0.jar demo
java -jar target/city-network-1.0.jar demo data/disconnected.txt
java -jar target/city-network-1.0.jar demo data/dag_negative.txt
```

If Maven has not been run yet, replace `target/` with `dist/` in those commands.

The four fixtures demonstrate a connected graph, a disconnected graph with parallel edges and a negative self-loop, a cyclic directed graph, and a DAG with negative weights and an isolated vertex. Their expected MST/forest weights are 10 and 8; the cyclic graph has four SCCs; the negative DAG has distance sum -5 and path `[1, 2, 3, 4]` to vertex 4. The demo also prints the condensation and a longest component path.

## Benchmark

```bash
java -Xmx2g -Xss256k -jar target/city-network-1.0.jar benchmark --full
```

The benchmark creates the exact `Random(42)` graph families from the assignment, performs one warm-up plus five measured runs of each algorithm, and records the median time. Generator details: shuffle via Fisher-Yates; draw the tree parent before its weight; draw `a` once, then redraw only `b` while `b == a`; draw weight after valid endpoints; interpret the shuffled DAG array as `rank[vertex]`. Parallel edges are retained. Each graph kind starts with a new `Random(42)`.

The full run checks both MSTs, all reference results, every topological edge constraint, deterministic repeated counters, Tarjan/Kosaraju partition agreement, condensation acyclicity, and exact scan identities. A violation throws an exception and exits nonzero; no new CSV is published until every case passes.

For a quick rehearsal on the two smallest density cases, without replacing the full results:

```bash
java -jar target/city-network-1.0.jar benchmark --quick
```

Quick outputs go to `target/quick-results/`. A custom output directory can be supplied after `--quick` or `--full`.

## Reproduce every measurement and figure

```bash
python3 -m pip install -r scripts/requirements.txt
bash scripts/reproduce.sh
```

After Python dependencies are installed, the second command performs the entire build, test, benchmark, plot, and report pipeline. It replaces measured output files; wall-clock times will change. To redraw from existing CSV data only:

```bash
python3 scripts/analyze.py
python3 scripts/build_report.py
```

## Input format

```text
5 7 undirected
0 1 4
0 2 2
1 2 1
1 3 5
2 3 8
3 4 2
2 4 10
```

The first line is `n m directed|undirected`, followed by exactly `m` lines `u v w`. Vertices are zero-based; weights are Java `int`. Total weights and distances are `long`. The parser rejects invalid indices, missing/extra edge lines, invalid graph types, and negative sizes. The graph supports loops, parallel edges, isolated vertices, and disconnected components. MST algorithms require undirected input; SCC/topological/path algorithms require directed input. Kahn returns `hasCycle = true` and an empty order on cyclic input; shortest paths reject a cycle explicitly.

## Counter definitions

| Column | Meaning |
| --- | --- |
| `dsu_ops` | `findCalls + successfulUnions`; find calls inside union count. Unsuccessful unions do not increment the union counter. |
| `edge_scans` | Algorithm-phase scans: Kruskal selection edges; Prim adjacency entries; two Kosaraju DFS passes; Kahn outgoing edges; DAG relaxation edges. |
| `comparisons` | Weight comparisons in the handwritten merge sort or the `PriorityQueue` comparator. Integer endpoint tie breaks and distance comparisons are excluded. |
| `max_depth` | Maximum number of active DFS frames, root depth 1. Zero for algorithms without an explicit DFS stack. |
| `result` | MST/forest weight; SCC count; Kahn order length; or sum of all DAG distances. |

Kosaraju transpose construction is included in timing and complexity, but excluded from traversal scans, to match the required `2m`. In-degrees are maintained when Graph inserts edges. DAG timing includes Kahn; the DAG row's counters measure its relaxation phase only, which gives the required `m`. Generation, result validation, and file I/O are outside timed intervals. `operations = dsu_ops + edge_scans + comparisons` is a plotted proxy, not every machine instruction. Find-call counts cannot establish the inverse-Ackermann bound empirically.

Kruskal stops after obtaining one component, so its edge fraction is data-dependent. All input edges are still sorted. Prim is a **lazy edge-queue** implementation: its bound is `O(V + E log(E + 1))`, not the indexed-heap bound of a different implementation. The report separates scan slopes from sorting work. These details resolve the assignment's ambiguous slope wording without altering measured evidence. Deterministic generator results are portable; PQ comparison counts may vary with an implementation change in the JDK.

## Tests and independent references

| Test suite | Evidence |
| --- | --- |
| `DSUTest` | Random unions versus simple label replacement, set counts, counters, reset, invalid indices. |
| `MstTest` | 100 connected random graphs, tree edge counts and acyclicity, exhaustive tiny MSTs, disconnected fixtures, loops, duplicate edges, 64-bit totals. |
| `SccTest` | Random graphs up to 200 vertices versus forward/reverse BFS, Tarjan partition equality, exact scans, empty graph, 100,000-vertex path. |
| `PathsTest` | Valid topological positions, explicit cycle detection, 100 random DAGs versus Bellman-Ford including negative weights, restored paths, exhaustive longest paths. |
| `GraphTest` | File parsing, edge cases, reversal, cached degrees, read-only adjacency views, malformed input. |
| `MetricsTest` | Both smallest official density cases, generator repeatability, non-mutation of input graphs, counter identities, condensation parallel-edge aggregation. |

The recursive exhaustive path enumerator exists only in a test on eight vertices; production SCC algorithms never use recursion.

## Bonus scope

Tarjan uses explicit DFS frames and a separate active-vertex stack. Component IDs are implementation-dependent, so comparison verifies the partition rather than raw ID arrays. The condensation merges parallel transitions by maximum weight because its purpose is the longest component path. Internal SCC edges are discarded: the result is not the longest simple path inside the original cyclic graph. Cyclic jobs require resolution before a feasible real schedule can be created.

## Git and submission

The repository history includes `main`, `feature/dsu`, `feature/mst`, `feature/scc`, `feature/paths`, and `feature/metrics`, with meaningful feature commits and merges. Release tag: `v1.0`. `repository.bundle` in the submission archive preserves all branches and tags without relying on hidden folders.

To restore it:

```bash
git clone repository.bundle restored-project
cd restored-project
git branch --all
git tag
```

The bundle's feature branches initially appear as remote branches after cloning; they can be checked out by name. To publish an existing local repository, create an empty GitHub repository and follow `SUBMISSION.md`. The required Moodle filename is `DAA_Assignment3_Moldir_Amanbek_SE-2521.zip`.

## AI use acknowledgement

OpenAI ChatGPT/Codex assisted with implementing the Java algorithms, designing JUnit tests, running benchmarks, generating plots, writing documentation, and preparing the report. All benchmark numbers were produced by actual program execution, and test/reference checks were run. No third-party graph algorithm implementation is used in production code. The student should review and understand the code before the defense and describe this assistance honestly.

## Source map

`src/main/java/graph` contains the shared graph and edge types; `algorithms` contains the implementations; `metrics` contains counters; `benchmark` contains the generator, benchmark, and command-line entry point. Tests are under `src/test/java/tests`. `DEFENSE_RU.md` explains the key code and provides simple English answers for the defense.
