package tests;

import algorithms.DSU;
import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DSUTest {
    @Test void randomUnionsAgreeWithLabels() {
        Random random = new Random(73);
        for (int n = 1; n <= 80; n++) {
            DSU dsu = new DSU(n);
            int[] labels = new int[n];
            for (int i = 0; i < n; i++) labels[i] = i;
            for (int step = 0; step < 200; step++) {
                int a = random.nextInt(n), b = random.nextInt(n);
                int old = labels[b], next = labels[a];
                assertEquals(old != next, dsu.union(a, b));
                for (int i = 0; i < n; i++) if (labels[i] == old) labels[i] = next;
                assertEquals(Arrays.stream(labels).distinct().count(), dsu.components());
                for (int i = 0; i < n; i++)
                    assertEquals(labels[i] == labels[a], dsu.find(i) == dsu.find(a));
            }
        }
    }

    @Test void countersResetAndOnlySuccessfulUnionsCount() {
        DSU dsu = new DSU(4);
        assertTrue(dsu.union(0, 1));
        assertFalse(dsu.union(1, 0));
        dsu.find(1);
        assertEquals(5, dsu.metrics().findCalls);
        assertEquals(1, dsu.metrics().successfulUnions);
        assertEquals(6, dsu.metrics().dsuOps());
        dsu.makeSet(2);
        assertEquals(2, dsu.components());
        assertEquals(0, dsu.metrics().dsuOps());
        assertThrows(IllegalArgumentException.class, () -> dsu.find(2));
        assertEquals(0, new DSU(0).components());
    }
}
