package graph;

import graph.scc.StronglyConnectedComponents;
import graph.topo.TopologicalSort;
import graph.dagsp.DAGShortestPath;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

// Tests for edge cases and boundary conditions
class EdgeCaseTests {
    private Metrics metrics = new Metrics();

    @Test
    void testEmptyGraph() {
        Graph graph = new Graph(0, true);

        StronglyConnectedComponents scc = new StronglyConnectedComponents(graph, metrics);
        List<List<Integer>> components = scc.findSCCs();

        assertTrue(components.isEmpty());

        TopologicalSort topo = new TopologicalSort(graph, metrics);
        List<Integer> order = topo.topologicalOrder();

        assertTrue(order.isEmpty());
    }

    @Test
    void testSingleNodeGraph() {
        Graph graph = new Graph(1, true);

        // SCC
        StronglyConnectedComponents scc = new StronglyConnectedComponents(graph, metrics);
        List<List<Integer>> components = scc.findSCCs();

        assertEquals(1, components.size());
        assertEquals(1, components.get(0).size());
        assertEquals(0, components.get(0).get(0));

        // Topological Sort
        TopologicalSort topo = new TopologicalSort(graph, metrics);
        List<Integer> order = topo.topologicalOrder();

        assertEquals(1, order.size());
        assertEquals(0, order.get(0));

        // Path Finding
        DAGShortestPath sp = new DAGShortestPath(graph, metrics);
        int[] dist = sp.shortestPaths(0, order);

        assertEquals(0, dist[0]);

        List<Integer> path = sp.reconstructPath(0);
        assertEquals(List.of(0), path);
    }

    @Test
    void testDisconnectedGraph() {
        Graph graph = new Graph(4, true);
        // No edges - all nodes disconnected

        StronglyConnectedComponents scc = new StronglyConnectedComponents(graph, metrics);
        List<List<Integer>> components = scc.findSCCs();

        assertEquals(4, components.size());
        for (List<Integer> component : components) {
            assertEquals(1, component.size());
        }

        TopologicalSort topo = new TopologicalSort(graph, metrics);
        List<Integer> order = topo.topologicalOrder();

        assertEquals(4, order.size());
        // Any order is valid for disconnected nodes
    }

    @Test
    void testGraphWithOnlySelfLoops() {
        Graph graph = new Graph(3, true);
        graph.addEdge(0, 0, 1);
        graph.addEdge(1, 1, 2);
        graph.addEdge(2, 2, 3);

        StronglyConnectedComponents scc = new StronglyConnectedComponents(graph, metrics);
        List<List<Integer>> components = scc.findSCCs();

        assertEquals(3, components.size());
        for (List<Integer> component : components) {
            assertEquals(1, component.size());
        }

        // Self-loops don't create cycles between different nodes
        TopologicalSort topo = new TopologicalSort(graph, metrics);
        List<Integer> order = topo.topologicalOrder();

        assertEquals(3, order.size());
    }

    @Test
    void testNegativeWeightsInDAG() {
        Graph graph = new Graph(4, true);
        graph.addEdge(0, 1, -2);
        graph.addEdge(1, 2, 3);
        graph.addEdge(0, 3, 1);
        graph.addEdge(3, 2, 4);

        TopologicalSort topo = new TopologicalSort(graph, metrics);
        List<Integer> order = topo.topologicalOrder();

        DAGShortestPath sp = new DAGShortestPath(graph, metrics);
        int[] dist = sp.shortestPaths(0, order);

        // Should handle negative weights correctly in DAG
        assertEquals(0, dist[0]);
        assertEquals(-2, dist[1]);
        assertEquals(1, dist[2]); // 0->1->2 = -2+3=1, 0->3->2 = 1+4=5
        assertEquals(1, dist[3]);
    }

    @Test
    void testLargeWeights() {
        Graph graph = new Graph(3, true);
        graph.addEdge(0, 1, Integer.MAX_VALUE - 1);
        graph.addEdge(1, 2, 1);

        TopologicalSort topo = new TopologicalSort(graph, metrics);
        List<Integer> order = topo.topologicalOrder();

        DAGShortestPath sp = new DAGShortestPath(graph, metrics);
        int[] dist = sp.shortestPaths(0, order);

        // Should handle large weights without integer overflow
        assertEquals(0, dist[0]);
        assertEquals(Integer.MAX_VALUE - 1, dist[1]);
        assertEquals(Integer.MAX_VALUE, dist[2]);
    }
}