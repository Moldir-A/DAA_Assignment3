package algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PathResult {
    private final long[] distance;
    private final int[] parent;

    public PathResult(long[] distance, int[] parent) {
        this.distance = distance.clone();
        this.parent = parent.clone();
    }

    public long distanceTo(int v) { return distance[v]; }
    public long[] distances() { return distance.clone(); }
    public int parentOf(int v) { return parent[v]; }
    public long sum() {
        long sum = 0;
        for (long d : distance) sum = Math.addExact(sum, d);
        return sum;
    }

    public List<Integer> pathTo(int target) {
        if (target < 0 || target >= parent.length) throw new IllegalArgumentException("Invalid target");
        List<Integer> path = new ArrayList<>();
        for (int v = target; v != -1; v = parent[v]) path.add(v);
        Collections.reverse(path);
        return List.copyOf(path);
    }
}
