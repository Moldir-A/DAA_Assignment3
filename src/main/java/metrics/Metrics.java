package metrics;

public final class Metrics {
    public long findCalls;
    public long successfulUnions;
    public long edgeScans;
    public long comparisons;
    public int maxDepth;

    public long dsuOps() { return findCalls + successfulUnions; }
    public long operations() { return dsuOps() + edgeScans + comparisons; }
    public void depth(int depth) { maxDepth = Math.max(maxDepth, depth); }

    public String counters() {
        return dsuOps() + "," + edgeScans + "," + comparisons + "," + maxDepth;
    }
}
