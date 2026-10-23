package algorithms;

import java.util.Arrays;

public final class SccResult {
    private final int[] id;
    private final int count;

    public SccResult(int[] id, int count) { this.id = id.clone(); this.count = count; }
    public int count() { return count; }
    public int componentOf(int vertex) { return id[vertex]; }
    public int[] ids() { return id.clone(); }

    public boolean samePartition(SccResult other) {
        if (id.length != other.id.length || count != other.count) return false;
        int[] forward = new int[count], backward = new int[count];
        Arrays.fill(forward, -1);
        Arrays.fill(backward, -1);
        for (int v = 0; v < id.length; v++) {
            int a = id[v], b = other.id[v];
            if ((forward[a] != -1 && forward[a] != b) || (backward[b] != -1 && backward[b] != a)) return false;
            forward[a] = b;
            backward[b] = a;
        }
        return true;
    }
}
