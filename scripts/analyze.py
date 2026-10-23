from pathlib import Path
import csv
import json
import math
import statistics
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt

ROOT = Path(__file__).resolve().parents[1]
rows = list(csv.DictReader((ROOT / 'results/results.csv').open()))
for r in rows:
    for key in ['n', 'm', 'dsu_ops', 'edge_scans', 'comparisons', 'max_depth', 'result']:
        r[key] = int(r[key])
    r['time_ms'] = float(r['time_ms'])
    r['ops'] = r['dsu_ops'] + r['edge_scans'] + r['comparisons']
algorithms = ['Kruskal', 'Prim', 'Kosaraju', 'Kahn', 'DagShortestPath']
colors = ['#2661a0', '#b85036', '#21856f', '#8165a5', '#c89420']
markers = ['o', 's', '^', 'o', 'x']
plt.rcParams.update({'font.family':'DejaVu Sans', 'font.size':10, 'axes.spines.top':False,
                     'axes.spines.right':False, 'axes.titleweight':'bold', 'figure.facecolor':'white'})
output = ROOT / 'results/plots'
output.mkdir(parents=True, exist_ok=True)

def subset(algorithm, density):
    return sorted((r for r in rows if r['algorithm'] == algorithm and r['graph'].endswith(density)), key=lambda r:r['n'])

def slope(series, key):
    x = [math.log(r['n']) for r in series]
    y = [math.log(r[key]) for r in series]
    xm, ym = statistics.mean(x), statistics.mean(y)
    return sum((a-xm)*(b-ym) for a,b in zip(x,y))/sum((a-xm)**2 for a in x)

for metric, filename, ylabel, title in [
    ('ops', 'operations_vs_n.png', 'Counted operations', 'Observed work as graphs grow'),
    ('scans', 'scans_per_edge.png', 'edgeScans / m', 'Traversal scans per input edge')]:
    fig, axes = plt.subplots(1, 2, figsize=(10.5,4.0), sharey=True)
    for ax, density in zip(axes, ['sparse', 'dense']):
        for i, algorithm in enumerate(algorithms):
            data = subset(algorithm, density)
            y = [r['ops'] if metric == 'ops' else r['edge_scans']/r['m'] for r in data]
            ax.plot([r['n'] for r in data], y, color=colors[i], marker=markers[i],
                    markerfacecolor='none' if i == 3 else colors[i], markersize=7 if i == 3 else 4,
                    linestyle='--' if i in [2,4] else '-', linewidth=1.6, label=algorithm)
        ax.set_xscale('log')
        if metric == 'ops': ax.set_yscale('log')
        else: ax.set_ylim(0, 2.25)
        ax.set_title(density.capitalize() + ('  m = 4n' if density == 'sparse' else '  m = 20n'))
        ax.set_xlabel('Vertices n')
        ax.grid(True, alpha=.18, which='major')
    axes[0].set_ylabel(ylabel)
    handles, labels = axes[0].get_legend_handles_labels()
    fig.legend(handles, labels, loc='lower center', ncol=5, frameon=False, fontsize=9)
    fig.suptitle(title, fontsize=14, fontweight='bold')
    fig.tight_layout(rect=(0,.11,1,.92))
    fig.savefig(output / filename, dpi=200)
    plt.close(fig)

fig, ax = plt.subplots(figsize=(10.5,3.7))
for density, color, style in [('sparse','#2661a0','-'), ('dense','#b85036','--')]:
    data = subset('Kruskal', density)
    y = [r['edge_scans']/r['m'] for r in data]
    ax.plot([r['n'] for r in data], y, marker='o', color=color, linestyle=style, label=density.capitalize())
    for r, value in zip(data,y):
        ax.annotate(f'{value:.1%}', (r['n'],value), xytext=(0,8), textcoords='offset points', ha='center',fontsize=9)
ax.set_xscale('log')
ax.set_ylim(0,1.10)
ax.set_xlabel('Vertices n')
ax.set_ylabel('edgeScans / m')
ax.set_title('Share of sorted edges examined by Kruskal')
ax.grid(True, alpha=.18)
ax.legend(frameon=False, loc='lower right')
fig.tight_layout()
fig.savefig(output/'kruskal_edge_share.png', dpi=200)
plt.close(fig)

analysis = {'slopes':[], 'ratios':[], 'time_largest':[], 'test_count':0}
for algorithm in algorithms:
    for density in ['sparse','dense']:
        data = subset(algorithm,density)
        analysis['slopes'].append({'algorithm':algorithm,'density':density,
                                  'scans':slope(data,'edge_scans'),'operations':slope(data,'ops')})
        if algorithm in ['Kruskal','Prim']:
            ratio = [r['comparisons']/(r['m']*math.log2(r['m'])) for r in data]
            formula = 'comparisons / (m log2 m)'
        else:
            ratio = [r['edge_scans']/(r['n']+r['m']) for r in data]
            formula = 'edgeScans / (n + m)'
        analysis['ratios'].append({'algorithm':algorithm,'density':density,'formula':formula,
                                  'values':ratio, 'min':min(ratio), 'max':max(ratio)})
        analysis['time_largest'].append({'algorithm':algorithm,'density':density,'ms':data[-1]['time_ms']})
analysis['kruskal_dsu_per_m'] = {density:[r['dsu_ops']/r['m'] for r in subset('Kruskal',density)] for density in ['sparse','dense']}
analysis['kruskal_share'] = {density:[r['edge_scans']/r['m'] for r in subset('Kruskal',density)] for density in ['sparse','dense']}
from xml.etree import ElementTree as ET
suites=[]
for file in sorted((ROOT/'target/surefire-reports').glob('TEST-*.xml')):
    a=ET.parse(file).getroot().attrib
    suites.append({k:a[k] for k in ['name','tests','errors','failures','skipped']})
analysis['test_count'] = sum(int(s['tests']) for s in suites)
if suites:
    (ROOT/'results/test_summary.txt').write_text('\n'.join(str(s) for s in suites)+'\nTotal: '+str(analysis['test_count'])+' tests\n')
(ROOT/'results/analysis.json').write_text(json.dumps(analysis,indent=2)+'\n')
print(json.dumps(analysis,indent=2))
