# Подготовка к защите DAA Assignment 3

Moldir Amanbek, SE-2521. Сначала разберись в объяснениях и запусти команды самостоятельно. Ниже указано поведение именно этого проекта.

## Что показать преподавателю

Открой папку с `pom.xml` в VS Code. В терминале выполни:

```bash
mvn clean verify
java -jar target/city-network-1.0.jar demo
java -Xmx2g -Xss256k -jar target/city-network-1.0.jar benchmark --full
```

Первая команда должна закончиться `BUILD SUCCESS` и `Tests run: 21, Failures: 0, Errors: 0, Skipped: 0`. Вторая показывает примеры MST, SCC, обнаружение цикла и восстановление пути. Третья воспроизводит все контрольные значения. Для короткой репетиции можно использовать `benchmark --quick`, но полные результаты находятся в `results/results.csv`.

Открой `REPORT.pdf`, `results/plots/`, `results/reference_check.txt` и `results/test_summary.txt`. Если работаешь с восстановленным Git-репозиторием, покажи:

```bash
git log --graph --oneline --all
git branch --all
git tag
```

## Короткое выступление на простом английском

Hello. My name is Moldir Amanbek, and I am from group SE-2521. My project is called City Network and Job Scheduler. It solves two kinds of problems: connecting city districts at minimum cost and analysing job dependencies.

I use one Graph class for both directed and undirected graphs. It stores adjacency lists. For an undirected edge, I store two adjacency entries. The project can also read graphs from text files.

My DSU uses union by size and path compression. Union by size attaches the smaller tree to the larger one. Path compression makes future find operations faster.

I implemented Kruskal and Prim to find a minimum spanning tree. Kruskal sorts the edges and uses DSU to avoid cycles. Prim starts from vertex zero and uses a priority queue. If the graph is disconnected, both algorithms return a minimum spanning forest and the number of components.

For directed graphs, I use Kosaraju to find strongly connected components. Both DFS passes are iterative. This avoids a stack overflow on a path with one hundred thousand vertices. Kahn's algorithm produces a topological order. If it cannot remove all vertices, it reports a cycle.

For shortest paths in a DAG, every vertex with in-degree zero starts at distance zero. I relax the edges in topological order. Negative weights are allowed because the graph has no cycles. I also keep parent pointers to restore an actual path.

I tested the algorithms against independent references. The project has twenty-one JUnit tests, and all of them pass. The complete benchmark matches every reference value in the assignment. I measure the median of five runs, but I use operation counters for the main complexity analysis.

For the bonus tasks, I implemented iterative Tarjan and a condensation graph with a longest component path. Tarjan scans each edge once, while Kosaraju scans each edge twice in its DFS passes. I used AI assistance to prepare the implementation and documentation, and I acknowledge it in the README.

## Как объяснить основные файлы

| Файл | Что делает и что показать |
| --- | --- |
| `graph/Graph.java` | `addEdge` добавляет ребро, а для undirected ещё обратную запись; `read` читает n, m, тип и строки u v w; `indegrees` хранит число входящих рёбер. |
| `algorithms/DSU.java` | В `find` первый цикл ищет корень, второй направляет посещённые вершины прямо к корню. В `union` меньший размер присоединяется к большему. |
| `algorithms/Kruskal.java` | Собственная сортировка слиянием, затем `dsu.union`. Успешное объединение означает, что ребро соединяет разные компоненты и не создаёт цикл. |
| `algorithms/Prim.java` | Очередь выбирает минимальный вес. Ребро пропускается, если его конечная вершина уже посещена. Внешний цикл начинает новую компоненту после завершения предыдущей. |
| `algorithms/Kosaraju.java` | Первый проход сохраняет вершину в `finish` только после просмотра всех её исходящих рёбер. Второй проходит обратный граф в обратном порядке завершения. |
| `algorithms/Kahn.java` | В очередь попадают вершины с нулевой входящей степенью. После удаления вершины степени её соседей уменьшаются. |
| `algorithms/DagShortestPath.java` | Все источники получают 0. `candidate = distance[v] + edge.weight()` предлагает новый путь; при улучшении обновляется расстояние и родитель. |
| `algorithms/PathResult.java` | `pathTo` идёт по `parent` от цели к источнику, затем переворачивает список. |
| `algorithms/Tarjan.java` | `index` хранит время открытия, `low` — достижимый ранний индекс. Равенство `low[v] == index[v]` означает корень SCC. |
| `algorithms/Condensation.java` | Каждая SCC становится вершиной; внутренние рёбра убираются. Для параллельных переходов выбирается максимальный вес, так как дальше ищется длиннейший путь. |
| `benchmark/Generator.java` | Для каждого типа графа заново создаётся `Random(42)`. Порядок вызовов генератора влияет на точное совпадение чисел. |
| `benchmark/Benchmark.java` | Один прогрев, пять измерений, медиана времени, автоматические проверки, затем запись CSV. |

Все пути в таблице относятся к `src/main/java/`.

## Частые вопросы и короткие ответы

**What is a minimum spanning tree?**

A minimum spanning tree connects all vertices of a connected undirected graph, has no cycles, and has minimum total weight. It contains V minus one edges.

**What happens if the graph is disconnected?**

The result is a minimum spanning forest. Each connected component has its own tree. The total number of chosen edges is V minus the number of components.

**Why do Kruskal and Prim return the same weight?**

Both use the cut property. A lightest edge crossing a safe cut can be part of a minimum spanning tree. Their chosen edges may differ when weights are equal, but the optimal total weight is the same.

**What is alpha of V?**

It is the inverse Ackermann function. It grows extremely slowly. With union by size and path compression, the amortized time of find and union is almost constant. This is not a strict O(1) worst-case bound for each call.

**Why is DFS iterative?**

A recursive call for each vertex can overflow the JVM call stack. I store the DFS frames in arrays on the heap and count the maximum active depth. On the long path, the measured depth is exactly one hundred thousand.

**What is a strongly connected component?**

It is a maximal group of vertices where every vertex can reach every other vertex by directed paths. A single isolated vertex is also a component.

**How does Kahn detect a cycle?**

If fewer than V vertices leave the queue, some vertices still have incoming edges from the remaining graph. Therefore, there is a directed cycle. I report it and return no valid order.

**Why are negative weights allowed in a DAG?**

A DAG has no cycles, so it cannot contain a negative cycle. The topological order ensures that all incoming paths to a vertex are considered before its outgoing edges are processed. Negative edges in a general graph are not automatically invalid, but negative cycles can make shortest distances undefined.

**Why do all sources start at zero?**

The assignment uses a virtual super-source. It has a zero-weight edge to every vertex with in-degree zero. Every vertex in a finite DAG is reachable from at least one source, so all distances are finite.

**Why use long?**

Individual weights fit in int, but their sum may exceed the int limit. Total MST weights and path distances use long.

**Why are counters better than time?**

Time depends on hardware, the JVM, garbage collection, and other running programs. Counters show the work performed by the algorithm and are more reproducible. I still report the median of five timings.

**What exactly is counted as an edge scan?**

It is a scan in the algorithm phase defined in the README. Kosaraju counts its two DFS passes, not construction of the reversed graph. The DAG row counts its relaxation pass, while topological preprocessing is separate. Prim scans both adjacency entries of each undirected edge.

**Why is the Kruskal scan slope not the same as its time complexity?**

The logarithmic factor comes from sorting comparisons. The scan count only measures how many sorted edges are examined before the tree is complete. Its slope also depends on the changing early-stop fraction.

**Which algorithm would you choose for a dense graph?**

It depends on the representation and the implementation. For a truly dense graph, a matrix-based Prim algorithm can run in O(V squared). My implementation uses a lazy priority queue, so I do not claim that it is always better for dense graphs. On the assignment's generated graphs, Kruskal uses fewer measured comparisons.

**Why can the longest path be easy in a DAG?**

Topological order lets us maximize distances in one pass. There are no cycles to revisit. The longest simple path problem in a general graph is much harder; an unrestricted positive cycle can make a walk arbitrarily long.

**What does the condensation critical path mean?**

It is a path between SCC groups. It does not include arbitrary paths inside the original components. Cyclic job dependencies still need to be resolved before execution can be scheduled.

## Полезные числа для демонстрации

| Пример | Результат |
| --- | --- |
| `connected.txt` | MST weight 10, одна компонента, 4 выбранных ребра. |
| `disconnected.txt` | Forest weight 8, три компоненты, 3 выбранных ребра. Петля веса -100 в дерево не входит. |
| `cyclic.txt` | Четыре SCC; Kahn сообщает о цикле. В condensation самый длинный путь имеет вес 9. |
| `dag_negative.txt` | Расстояния `[0, 0, -2, 1, -4, 0, 0]`, сумма -5. Путь к 4: `[1, 2, 3, 4]`. |
| n = 100,000, m = 2,000,000 | MST 3,056,681; одна SCC; DAG sum 23,512,087. |

## Что важно не перепутать

- MST относится к неориентированному графу; SCC и топологический порядок — к ориентированному.
- Минимальный остов минимизирует суммарный вес всех выбранных рёбер. Он не обязан давать кратчайшие пути между каждой парой вершин.
- Равные веса MST не означают одинаковые списки рёбер.
- Номера SCC могут различаться в Kosaraju и Tarjan; сравнивается состав компонент.
- `m = 20n` назван dense в задании, но это всё ещё линейное число рёбер относительно n, а не n squared.
- В DAG результат начинается от всех источников; выбирать только вершину 0 было бы ошибкой.
- Счётчики не записаны из формулы: они увеличиваются внутри выполняемых циклов и компараторов.
- Оценка зависит также от понимания кода и ответов на защите.
