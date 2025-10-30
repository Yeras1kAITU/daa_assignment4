package graph.topo;

import graph.Graph;
import graph.Metrics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

class TopologicalSortTest {
    private Metrics metrics;

    @BeforeEach
    void setUp() {
        metrics = new Metrics();
    }

    @Test
    void testSimpleDAG() {
        Graph graph = new Graph(4, true);
        graph.addEdge(0, 1, 1);
        graph.addEdge(0, 2, 1);
        graph.addEdge(1, 3, 1);
        graph.addEdge(2, 3, 1);

        TopologicalSort topo = new TopologicalSort(graph, metrics);
        List<Integer> order = topo.topologicalOrder();

        assertEquals(4, order.size());
        assertTrue(order.indexOf(0) < order.indexOf(1));
        assertTrue(order.indexOf(0) < order.indexOf(2));
        assertTrue(order.indexOf(1) < order.indexOf(3));
        assertTrue(order.indexOf(2) < order.indexOf(3));
        assertTrue(topo.isDAG());
    }

    @Test
    void testSingleNode() {
        Graph graph = new Graph(1, true);

        TopologicalSort topo = new TopologicalSort(graph, metrics);
        List<Integer> order = topo.topologicalOrder();

        assertEquals(1, order.size());
        assertEquals(0, order.get(0));
        assertTrue(topo.isDAG());
    }

    @Test
    void testCycleDetection() {
        Graph graph = new Graph(3, true);
        graph.addEdge(0, 1, 1);
        graph.addEdge(1, 2, 1);
        graph.addEdge(2, 0, 1); // Creates cycle

        TopologicalSort topo = new TopologicalSort(graph, metrics);
        List<Integer> order = topo.topologicalOrder();

        assertTrue(order.isEmpty());
        assertFalse(topo.isDAG());
    }

    @Test
    void testDisconnectedDAG() {
        Graph graph = new Graph(5, true);
        // First component: 0->1->2
        graph.addEdge(0, 1, 1);
        graph.addEdge(1, 2, 1);
        // Second component: 3->4
        graph.addEdge(3, 4, 1);

        TopologicalSort topo = new TopologicalSort(graph, metrics);
        List<Integer> order = topo.topologicalOrder();

        assertEquals(5, order.size());
        assertTrue(topo.isDAG());

        // Verify constraints
        assertTrue(order.indexOf(0) < order.indexOf(1));
        assertTrue(order.indexOf(1) < order.indexOf(2));
        assertTrue(order.indexOf(3) < order.indexOf(4));
    }

    @Test
    void testLinearChain() {
        Graph graph = new Graph(5, true);
        graph.addEdge(0, 1, 1);
        graph.addEdge(1, 2, 1);
        graph.addEdge(2, 3, 1);
        graph.addEdge(3, 4, 1);

        TopologicalSort topo = new TopologicalSort(graph, metrics);
        List<Integer> order = topo.topologicalOrder();

        assertEquals(5, order.size());
        // Should be in order 0,1,2,3,4
        for (int i = 0; i < 5; i++) {
            assertEquals(i, order.get(i));
        }
        assertTrue(topo.isDAG());
    }
}