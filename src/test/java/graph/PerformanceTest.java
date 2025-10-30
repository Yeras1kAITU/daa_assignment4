package graph;

import graph.scc.StronglyConnectedComponents;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.TimeUnit;

// Performance tests for large graphs and edge cases
class PerformanceTest {
    private Metrics metrics = new Metrics();

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testPerformanceOnLargeDAG() {
        // Create a large DAG (100 nodes)
        int n = 100;
        Graph graph = new Graph(n, true);

        // Create a linear chain with some cross edges
        for (int i = 0; i < n - 1; i++) {
            graph.addEdge(i, i + 1, 1);
            if (i < n - 2) {
                graph.addEdge(i, i + 2, 2);
            }
        }

        StronglyConnectedComponents scc = new StronglyConnectedComponents(graph, metrics);
        scc.findSCCs();

        // Should have n components in a DAG
        assertEquals(n, scc.getComponents().size());
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testPerformanceOnDenseGraph() {
        // Create a moderately dense graph (50 nodes)
        int n = 50;
        Graph graph = new Graph(n, true);

        // Add edges from each node to about half of the subsequent nodes
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n && j < i + n/2; j++) {
                graph.addEdge(i, j, (i + j) % 10 + 1);
            }
        }

        // This tests performance on a graph with ~O(n^2) edges
        StronglyConnectedComponents scc = new StronglyConnectedComponents(graph, metrics);
        scc.findSCCs();

        assertTrue(scc.getComponents().size() >= 1);
    }

    @Test
    void testMemoryEfficiency() {
        // Test that algorithms don't have memory leaks
        Runtime runtime = Runtime.getRuntime();

        long initialMemory = runtime.totalMemory() - runtime.freeMemory();

        // Create and process multiple graphs
        for (int i = 0; i < 10; i++) {
            Graph graph = new Graph(20, true);
            for (int j = 0; j < 19; j++) {
                graph.addEdge(j, j + 1, 1);
            }

            StronglyConnectedComponents scc = new StronglyConnectedComponents(graph, metrics);
            scc.findSCCs();

            System.gc();
        }

        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryIncrease = finalMemory - initialMemory;

        // less than 1MB for this test
        assertTrue(memoryIncrease < 1024 * 1024,
                "Memory increase too large: " + memoryIncrease + " bytes");
    }
}