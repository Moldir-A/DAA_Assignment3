from pathlib import Path
import csv
import json
from xml.sax.saxutils import escape
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.lib import colors
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, Image, PageBreak

ROOT = Path(__file__).resolve().parents[1]
a = json.loads((ROOT/'results/analysis.json').read_text())
rows = list(csv.DictReader((ROOT/'results/results.csv').open()))
from matplotlib.font_manager import FontProperties, findfont
pdfmetrics.registerFont(TTFont('ProjectSans', findfont(FontProperties(family='DejaVu Sans'))))
pdfmetrics.registerFont(TTFont('ProjectSans-Bold', findfont(FontProperties(family='DejaVu Sans', weight='bold'))))
styles = getSampleStyleSheet()
styles.add(ParagraphStyle(name='BodyCustom', fontName='ProjectSans', fontSize=8.8, leading=11.9, spaceAfter=6))
styles.add(ParagraphStyle(name='SmallCustom', fontName='ProjectSans', fontSize=7.7, leading=10, spaceAfter=5))
styles.add(ParagraphStyle(name='CellCustom', fontName='ProjectSans', fontSize=7.9, leading=10.1))
styles.add(ParagraphStyle(name='HeadCustom', fontName='ProjectSans-Bold', fontSize=13, leading=16, spaceBefore=9, spaceAfter=7, textColor=colors.HexColor('#153b53')))
styles['Title'].fontName='ProjectSans-Bold'
styles['Title'].fontSize=21
styles['Title'].leading=25
styles['Title'].alignment=0
styles['Title'].spaceAfter=8
flow=[]
md=[]

def text(s, small=False):
    flow.append(Paragraph(escape(s), styles['SmallCustom' if small else 'BodyCustom']))
    md.append(s+'\n')

def head(s):
    flow.append(Paragraph(escape(s), styles['HeadCustom']))
    md.append('## '+s+'\n')

def table(headers, data, widths):
    entries=[[Paragraph(escape(str(c)),styles['CellCustom']) for c in row] for row in [headers]+data]
    t=Table(entries, colWidths=widths, repeatRows=1, hAlign='LEFT')
    t.setStyle(TableStyle([
        ('BACKGROUND',(0,0),(-1,0),colors.HexColor('#e7eef3')),
        ('VALIGN',(0,0),(-1,-1),'TOP'),
        ('LEFTPADDING',(0,0),(-1,-1),6),('RIGHTPADDING',(0,0),(-1,-1),6),
        ('TOPPADDING',(0,0),(-1,-1),5),('BOTTOMPADDING',(0,0),(-1,-1),5),
        ('ROWBACKGROUNDS',(0,1),(-1,-1),[colors.white,colors.HexColor('#f6f8fa')]),
        ('LINEBELOW',(0,0),(-1,0),.6,colors.HexColor('#acbdc8'))
    ]))
    flow.extend([t,Spacer(1,7)])
    md.extend(['| '+' | '.join(headers)+' |','| '+' | '.join(['---']*len(headers))+' |'])
    md.extend('| '+' | '.join(str(c) for c in row)+' |' for row in data)
    md.append('')

def figure(filename, height, caption):
    p=ROOT/'results/plots'/filename
    from PIL import Image as PILImage
    w,h=PILImage.open(p).size
    width=499
    flow.append(Image(str(p), width=width, height=width*h/w))
    text(caption, True)
    md.append('!['+caption+'](results/plots/'+filename+')\n')

def page(): flow.append(PageBreak())

flow.append(Paragraph('City Network and Job Scheduler',styles['Title']))
md.append('# City Network and Job Scheduler\n')
text('DAA Assignment 3 | Moldir Amanbek | SE-2521 | 23 October 2026')
text(f'The project implements one shared graph model, all required algorithms, and both bonus tasks. All 30 reference results match the assignment. The full benchmark contains 50 core rows and 10 Tarjan rows; all {a["test_count"]} JUnit 5 tests passed, including iterative SCC on a 100,000-vertex path with a 256 KiB JVM stack.')
head('Implementation and complexity')
text('Let V be vertices and E be logical input edges. Graph storage is O(V + E); an undirected edge has two adjacency entries. Space below is additional working space, including returned results but excluding the input graph. log(E + 1) also covers empty graphs.')
table(['Operation','Time','Extra space','Reason'],[
 ['DSU makeSet','O(V)','O(V)','Initialize parent and size arrays.'],
 ['DSU find / union','O(alpha(V)) amortized','O(1) per call','Size-based union bounds tree height; full iterative path compression flattens visited paths. The DSU owns O(V) storage; one call can take O(log V).'],
 ['Kruskal','O(V + E log(E + 1))','O(V + E)','Own bottom-up merge sort; each examined edge performs two finds and at most one successful union.'],
 ['Prim with lazy PQ','O(V + E log(E + 1))','O(V + E)','Each adjacency entry is read once; queued edges, including stale entries, require heap comparisons.'],
 ['Kosaraju','O(V + E)','O(V + E)','Two iterative DFS passes plus a reversed graph; at most V active frames.'],
 ['Kahn','O(V + E)','O(V)','Copy stored in-degrees, queue each vertex once, decrement once per outgoing edge.'],
 ['DAG shortest paths','O(V + E)','O(V)','Topological ordering plus one relaxation pass; long distances and parent pointers.'],
 ['Tarjan bonus','O(V + E)','O(V)','One iterative DFS with discovery indices, low links, and an active-vertex stack.'],
 ['Condensation bonus','O(V + E log(E + 1))','O(V + E)','Ordered map deduplicates component pairs; the subsequent longest-path pass is O(Vc + Ec).']
],[86,112,67,234])
head('What the counters measure')
text('dsu_ops = find calls + successful unions, including finds made inside union. edge_scans counts the Kruskal selection loop, Prim adjacency entries, the two Kosaraju DFS passes, Kahn outgoing edges, or DAG relaxations. comparisons counts edge-weight comparator calls in sorting and the priority queue; it excludes distance comparisons. max_depth is the largest active DFS frame stack in either SCC pass, with root depth 1.')
text('Graph construction and Kosaraju reversal are not traversal scans. Graph caches in-degrees when inserting edges. DAG timing includes a separate Kahn preprocessing pass, but the DAG row records only its relaxation scans, as requested. Operations in plots means dsu_ops + edge_scans + comparisons; it is a reproducible work proxy, not a count of every CPU instruction.')

page()
head('Correctness arguments')
text('Cut property. Consider a cut that does not split any already selected tree component. Let e be a minimum-weight edge crossing that cut. If a minimum spanning tree T already contains e, it is safe to keep it. Otherwise, adding e to T creates a cycle, and that cycle contains another edge f crossing the cut. Because weight(e) <= weight(f), replacing f with e preserves connectivity and cannot increase the total weight, so a minimum spanning tree containing e exists. Kruskal uses this rule when its next sorted edge joins two DSU components, while Prim uses the cut between visited and unvisited vertices. Applying the argument independently in each connected component gives a minimum spanning forest, and tied weights may produce different trees with the same optimal total. [1]')
text('Topological induction. Initialize every in-degree-zero vertex at distance 0, which is equivalent to adding a virtual source with zero-weight edges. Every vertex in a finite DAG is reachable from at least one such source. Assume all vertices before v in a valid topological order already have their correct shortest distances. Every incoming edge to v comes from one of those earlier vertices, so its relaxation has already proposed the cost of the best path ending with that edge. Taking the minimum of these proposals therefore establishes the correct distance to v. Relaxing v then prepares the candidates needed by later vertices, completing the induction after one pass. Negative edges do not break the argument because a DAG has no cycles; in a general graph a reachable negative cycle can make distances unbounded below, although Bellman-Ford handles negative edges when no relevant negative cycle exists. [2]')
head('Which algorithm when')
table(['Situation','Choice and reason'],[
 ['Sparse undirected graph; edge list','Kruskal: sorting is convenient and DSU rejects cycles; disconnected input naturally produces a forest.'],
 ['Undirected adjacency list','Prim: grows a tree from vertex 0 and restarts at unvisited vertices; useful when adjacency access is already available.'],
 ['Very dense graph, E near V squared','An O(V squared) array/matrix Prim variant can avoid heap costs; the submitted lazy-PQ Prim is not that variant. For the given m = 20n instances, measured counters favor Kruskal.'],
 ['Directed graph; cycles possible','Kosaraju for a clear two-pass SCC decomposition; Tarjan if avoiding the transpose and halving DFS edge scans matters.'],
 ['Dependency scheduling','Kahn provides an order or explicitly reports a cycle; SCCs locate mutually dependent groups.'],
 ['Weighted DAG, including negative edges','Topological shortest-path relaxation; for the critical path, replace minimum with maximum. Dijkstra is not the default for negative weights.']
],[159,340])
head('Bonus interpretation and validation')
text('Tarjan returns the same partition as Kosaraju, compared through a bijection of component labels rather than numeric IDs. It scans E edges instead of 2E and avoids the transpose. Condensation drops internal edges and keeps the maximum weight among parallel component transitions for longest-path analysis. The critical path is a component-level abstraction; internal cyclic job dependencies still require resolution before a real schedule is feasible. In a DAG, maximizing in topological order is equivalent to minimizing negated weights without negating int values. In a general graph, longest simple path is NP-hard; unrestricted walks can be unbounded with a positive cycle. [2, 3]')
text('Independent checks include 100 random MST comparisons plus exhaustive tiny spanning trees; random SCCs against BFS in both directions up to 200 vertices; 100 negative-weight DAGs against Bellman-Ford; path reconstruction; brute-force longest paths; malformed input; loops, parallel edges, isolated vertices, empty graphs, and 64-bit totals.',True)

page()
head('Exact reference agreement')
reference=[]
for n in [1000,3162,10000,31623,100000]:
    for density in ['sparse','dense']:
        selected={r['algorithm']:r for r in rows if int(r['n'])==n and r['graph'].endswith(density)}
        reference.append([f'{n:,}',f'{int(selected["Kruskal"]["m"]):,}',f'{int(selected["Kruskal"]["result"]):,}',selected['Kosaraju']['result'],f'{int(selected["DagShortestPath"]["result"]):,}'])
table(['n','m','MST weight','SCCs','DAG distance sum'],reference,[75,85,112,65,162])
text('Both MST algorithms agree with every listed weight and return V - 1 edges. Kahn returns V vertices, and every input edge satisfies pos[u] < pos[v]. All five measured runs reproduce the same result and counters. Any disagreement aborts the benchmark before a new CSV is written.')
head('Counter divided by complexity formula')
text('The five numeric columns follow n = 1,000; 3,162; 10,000; 31,623; 100,000. K = Kruskal and P = Prim use comparisons / (E log2 E). S = Kosaraju, T = Kahn, and D = DAG relaxation use edgeScans / (V + E).')
ratio_rows=[]
short={'Kruskal':'K','Prim':'P','Kosaraju':'S','Kahn':'T','DagShortestPath':'D'}
for r in a['ratios']:
    ratio_rows.append([short[r['algorithm']],r['density']]+[f'{v:.3f}' for v in r['values']])
table(['Alg.','Density','1,000','3,162','10,000','31,623','100,000'],ratio_rows,[35,74,78,78,78,78,78])
text('Traversal ratios are exactly constant: Kosaraju is 1.600 or 1.905, and Kahn/DAG relaxation is 0.800 or 0.952. Comparator ratios stay in narrow bands, consistent with E log E work; finite samples support rather than prove the asymptotic bounds. DSU call counts alone cannot experimentally establish alpha(V), because internal parent-pointer traversals are not counted.')
text('Kruskal dsu_ops / E, sparse: '+', '.join(f'{v:.3f}' for v in a['kruskal_dsu_per_m']['sparse'])+'. Dense: '+', '.join(f'{v:.3f}' for v in a['kruskal_dsu_per_m']['dense'])+'. These are bounded, but not constant: the early-stop position changes with the graph. Exactly dsu_ops = 2 * edgeScans + V - 1 on these connected inputs.',True)
text('Scope resolution: the detailed benchmark section specifies ten size/density pairs and 50 rows; this is followed despite the deliverables section saying "four generated graphs". Each pair uses three graph kinds, with a fresh Random(42) for each generator call.',True)

page()
head('Measured work and linear traversal scans')
figure('operations_vs_n.png',190,'Figure 1. Log-log work curves, with one line per algorithm and density. Kahn and DAG relaxation overlap because each counts exactly E scans. The two MST curves include weight comparisons.')
text('At both density settings, traversal work grows in direct proportion to n because m = cn. The assignment calls m = 20n dense, but both benchmark families remain asymptotically linear in n; this is not an E = Theta(V squared) experiment.')
figure('scans_per_edge.png',190,'Figure 2. Scans per logical edge. Prim and Kosaraju overlap at 2; Kahn and DAG relaxation overlap at 1. Kruskal is below 1 when early termination avoids scanning the sorted suffix.')
text('Prim scans both adjacency entries of every undirected edge, even though each useful crossing edge enters the queue only once. Kosaraju scans each directed edge once in each DFS pass. Kahn and DAG relaxation inspect each outgoing edge once; the DAG function also performs an unreported Kahn preprocessing scan to obtain its order.')
text('The flat scan ratios verify traversal work independently of laptop speed. Prim still has non-linear comparison work because its priority queue holds candidate edges. Kruskal sorts all E edges even if its selection loop stops early, so a small examined fraction does not remove the sorting cost.')

page()
head('Kruskal early stopping and slope check')
figure('kruskal_edge_share.png',170,'Figure 3. Fraction of sorted edges examined before one DSU component remains. All edges are still included in sorting.')
text('Sparse inputs examine 77.1% to 99.9% of edges; dense inputs examine 20.7% to 37.2%. The denser family offers more cheap connections before expensive edges are reached. The submitted lazy Prim always scans 2E adjacency entries and performs more weight comparisons on these data; this is implementation-specific evidence, not a universal claim about every Prim variant.')
slope_rows=[]
for alg in ['Kruskal','Prim','Kosaraju','Kahn','DagShortestPath']:
    rr=[r for r in a['slopes'] if r['algorithm']==alg]
    slope_rows.append([alg]+[f'{r[metric]:.3f}' for metric in ['scans','operations'] for r in rr])
table(['Algorithm','Scan sparse','Scan dense','Work sparse','Work dense'],slope_rows,[139,90,90,90,90])
text('Slopes are ordinary least-squares fits of ln(counter) against ln(n). Traversals give 1.000 exactly. Kruskal scan slopes are 1.055 and 1.119, while its total counted-work slopes are 1.096 and 1.088. The scan slope changes because the examined fraction changes; the E log E factor belongs to sorting comparisons. Conflating the two would misinterpret the assignment wording.')
head('Time and reproducibility')
env = (ROOT/'results/environment.txt').read_text().splitlines()
large_times = ', '.join(r['algorithm'] + ' ' + format(r['ms'], ',.1f') + ' ms' for r in a['time_largest'] if r['density']=='dense')
text(f'Times are medians of five runs after one warm-up. Environment: {env[0]}; {env[2]}; {env[4]}. At n = 100,000 and m = 2,000,000: {large_times}. Small cases show JVM warm-up and timing noise. Hardware, allocation, garbage collection, and background load affect elapsed time, so conclusions use counters. Result values and deterministic loop counts reproduce; priority-queue comparator counts can depend on the JDK implementation.')
text('Run bash scripts/reproduce.sh after installing the Python requirements. It builds, runs JUnit, benchmarks all ten pairs, and regenerates plots, analysis, and this report. Git contains main, the five required feature branches, and release tag v1.0. AI assistance in implementation, testing, and report preparation is acknowledged in README.md.',True)
head('References')
text('[1] Sedgewick and Wayne, Algorithms, 4.3 Minimum Spanning Trees: https://algs4.cs.princeton.edu/43mst/\n[2] Sedgewick and Wayne, Algorithms, 4.4 Shortest Paths: https://algs4.cs.princeton.edu/44sp/\n[3] Sedgewick and Wayne, Algorithms, 4.2 Directed Graphs: https://algs4.cs.princeton.edu/42digraph/\n[4] Taubakabyl Nurlybek, DAA Assignment 3 Graphs, supplied specification, pp. 1-7.',True)

def footer(canvas,doc):
    canvas.setFont('ProjectSans',8)
    canvas.setFillColor(colors.HexColor('#526876'))
    canvas.drawString(48,28,'Moldir Amanbek | SE-2521 | DAA Assignment 3')
    canvas.drawRightString(A4[0]-48,28,str(doc.page))

doc=SimpleDocTemplate(str(ROOT/'REPORT.pdf'),pagesize=A4,rightMargin=48,leftMargin=48,topMargin=38,bottomMargin=43,
                      title='City Network and Job Scheduler',author='Moldir Amanbek')
doc.build(flow,onFirstPage=footer,onLaterPages=footer)
(ROOT/'REPORT.md').write_text('\n'.join(md))
print('Created REPORT.pdf and REPORT.md')
