package graph.dagsp;

import graph.Graph;
import graph.Metrics;
import graph.topo.TopologicalSort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

class DAGShortestPathTest {
    private Metrics metrics;

    @BeforeEach
    void setUp() {
        metrics = new Metrics();
    }

    @Test
    void testShortestPathLinearGraph() {
        Graph graph = new Graph(4, true);
        graph.addEdge(0, 1, 2);
        graph.addEdge(1, 2, 3);
        graph.addEdge(2, 3, 1);

        TopologicalSort topo = new TopologicalSort(graph, metrics);
        List<Integer> order = topo.topologicalOrder();

        DAGShortestPath sp = new DAGShortestPath(graph, metrics);
        int[] dist = sp.shortestPaths(0, order);

        assertEquals(0, dist[0]);
        assertEquals(2, dist[1]);
        assertEquals(5, dist[2]);
        assertEquals(6, dist[3]);

        List<Integer> path = sp.reconstructPath(3);
        assertEquals(List.of(0, 1, 2, 3), path);
    }

    @Test
    void testShortestPathWithMultiplePaths() {
        Graph graph = new Graph(4, true);
        graph.addEdge(0, 1, 5);
        graph.addEdge(0, 2, 3);
        graph.addEdge(1, 3, 2);
        graph.addEdge(2, 3, 1);

        TopologicalSort topo = new TopologicalSort(graph, metrics);
        List<Integer> order = topo.topologicalOrder();

        DAGShortestPath sp = new DAGShortestPath(graph, metrics);
        int[] dist = sp.shortestPaths(0, order);

        assertEquals(0, dist[0]);
        assertEquals(5, dist[1]);
        assertEquals(3, dist[2]);
        assertEquals(4, dist[3]); // 0->2->3 = 3+1=4 is shorter than 0->1->3 = 5+2=7

        List<Integer> path = sp.reconstructPath(3);
        assertEquals(List.of(0, 2, 3), path);
    }

    @Test
    void testLongestPath() {
        Graph graph = new Graph(4, true);
        graph.addEdge(0, 1, 5);
        graph.addEdge(0, 2, 3);
        graph.addEdge(1, 3, 2);
        graph.addEdge(2, 3, 1);

        TopologicalSort topo = new TopologicalSort(graph, metrics);
        List<Integer> order = topo.topologicalOrder();

        DAGShortestPath sp = new DAGShortestPath(graph, metrics);
        int[] dist = sp.longestPaths(0, order);

        assertEquals(0, dist[0]);
        assertEquals(5, dist[1]);
        assertEquals(3, dist[2]);
        assertEquals(7, dist[3]); // 0->1->3 = 5+2=7 is longer than 0->2->3 = 3+1=4
    }

    @Test
    void testCriticalPath() {
        Graph graph = new Graph(5, true);
        graph.addEdge(0, 1, 3);
        graph.addEdge(0, 2, 2);
        graph.addEdge(1, 3, 4);
        graph.addEdge(2, 3, 1);
        graph.addEdge(3, 4, 2);

        TopologicalSort topo = new TopologicalSort(graph, metrics);
        List<Integer> order = topo.topologicalOrder();

        DAGShortestPath sp = new DAGShortestPath(graph, metrics);
        DAGShortestPath.CriticalPath criticalPath = sp.findCriticalPath(0, order);

        assertEquals(9, criticalPath.length); // 0->1->3->4 = 3+4+2=9
        assertEquals(List.of(0, 1, 3, 4), criticalPath.path);
    }

    @Test
    void testUnreachableNodes() {
        Graph graph = new Graph(4, true);
        graph.addEdge(0, 1, 2);
        graph.addEdge(2, 3, 1); // Node 2,3 disconnected from 0,1

        TopologicalSort topo = new TopologicalSort(graph, metrics);
        List<Integer> order = topo.topologicalOrder();

        DAGShortestPath sp = new DAGShortestPath(graph, metrics);
        int[] dist = sp.shortestPaths(0, order);

        assertEquals(0, dist[0]);
        assertEquals(2, dist[1]);
        assertEquals(Integer.MAX_VALUE, dist[2]);
        assertEquals(Integer.MAX_VALUE, dist[3]);

        List<Integer> pathTo3 = sp.reconstructPath(3);
        assertTrue(pathTo3.isEmpty());
    }

    @Test
    void testSingleNodeGraph() {
        Graph graph = new Graph(1, true);

        TopologicalSort topo = new TopologicalSort(graph, metrics);
        List<Integer> order = topo.topologicalOrder();

        DAGShortestPath sp = new DAGShortestPath(graph, metrics);
        int[] dist = sp.shortestPaths(0, order);

        assertEquals(0, dist[0]);

        List<Integer> path = sp.reconstructPath(0);
        assertEquals(List.of(0), path);
    }
}