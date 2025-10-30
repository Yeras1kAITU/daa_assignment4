package graph;

import graph.scc.StronglyConnectedComponents;
import graph.topo.TopologicalSort;
import graph.dagsp.DAGShortestPath;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

// Integration tests for the complete algorithm pipeline
class IntegrationTest {
    private Metrics metrics;

    @BeforeEach
    void setUp() {
        metrics = new Metrics();
    }

    @Test
    void testCompletePipelineOnSimpleDAG() {
        // Create a simple DAG: 0->1->2, 0->3->2
        Graph graph = new Graph(4, true);
        graph.addEdge(0, 1, 2);
        graph.addEdge(1, 2, 3);
        graph.addEdge(0, 3, 1);
        graph.addEdge(3, 2, 4);

        // Step 1: SCC Detection
        StronglyConnectedComponents scc = new StronglyConnectedComponents(graph, metrics);
        List<List<Integer>> components = scc.findSCCs();

        // In a DAG, each node should be its own SCC
        assertEquals(4, components.size());
        for (List<Integer> component : components) {
            assertEquals(1, component.size());
        }

        // Step 2: Topological Sort
        Graph condensation = scc.getCondensationGraph();
        TopologicalSort topo = new TopologicalSort(condensation, metrics);
        List<Integer> topoOrder = topo.topologicalOrder();

        assertFalse(topoOrder.isEmpty());
        assertEquals(4, topoOrder.size());

        // Step 3: Path Finding
        DAGShortestPath sp = new DAGShortestPath(condensation, metrics);
        int[] shortestDist = sp.shortestPaths(0, topoOrder);

        assertEquals(0, shortestDist[0]);
        assertEquals(2, shortestDist[1]);
        assertEquals(1, shortestDist[3]);
        assertEquals(5, shortestDist[2]); // 0->1->2 = 2+3=5, 0->3->2 = 1+4=5

        // Critical path
        DAGShortestPath.CriticalPath criticalPath = sp.findCriticalPath(0, topoOrder);
        assertEquals(5, criticalPath.length);
        assertTrue(criticalPath.path.contains(2));
    }

    @Test
    void testPipelineOnCyclicGraph() {
        // Create graph with cycle: 0->1->2->0
        Graph graph = new Graph(3, true);
        graph.addEdge(0, 1, 1);
        graph.addEdge(1, 2, 1);
        graph.addEdge(2, 0, 1);

        // Step 1: SCC Detection
        StronglyConnectedComponents scc = new StronglyConnectedComponents(graph, metrics);
        List<List<Integer>> components = scc.findSCCs();

        // Should have one SCC containing all nodes
        assertEquals(1, components.size());
        assertEquals(3, components.get(0).size());

        // Step 2: Topological Sort on condensation graph (single node)
        Graph condensation = scc.getCondensationGraph();
        assertEquals(1, condensation.getNodeCount());

        TopologicalSort topo = new TopologicalSort(condensation, metrics);
        List<Integer> topoOrder = topo.topologicalOrder();

        // Single node DAG should have valid topological order
        assertFalse(topoOrder.isEmpty());
        assertEquals(1, topoOrder.size());

        // Step 3: Path Finding
        DAGShortestPath sp = new DAGShortestPath(condensation, metrics);
        int[] shortestDist = sp.shortestPaths(0, topoOrder);

        assertEquals(0, shortestDist[0]);
    }

    @Test
    void testPipelineOnMultipleSCCs() {
        // Graph with two cycles connected by edges
        Graph graph = new Graph(6, true);
        // First cycle: 0->1->2->0
        graph.addEdge(0, 1, 1);
        graph.addEdge(1, 2, 1);
        graph.addEdge(2, 0, 1);
        // Second cycle: 3->4->5->3
        graph.addEdge(3, 4, 1);
        graph.addEdge(4, 5, 1);
        graph.addEdge(5, 3, 1);
        // Connection between cycles
        graph.addEdge(0, 3, 2);

        // Step 1: SCC Detection
        StronglyConnectedComponents scc = new StronglyConnectedComponents(graph, metrics);
        List<List<Integer>> components = scc.findSCCs();

        assertEquals(2, components.size());

        // Step 2: Topological Sort on condensation
        Graph condensation = scc.getCondensationGraph();
        TopologicalSort topo = new TopologicalSort(condensation, metrics);
        List<Integer> topoOrder = topo.topologicalOrder();

        assertFalse(topoOrder.isEmpty());
        assertEquals(2, topoOrder.size());

        // Step 3: Path Finding
        DAGShortestPath sp = new DAGShortestPath(condensation, metrics);
        int[] shortestDist = sp.shortestPaths(0, topoOrder);

        assertEquals(0, shortestDist[0]);
        assertEquals(2, shortestDist[1]); // Weight of edge from first to second SCC
    }

    @Test
    void testPipelineWithUnreachableComponents() {
        Graph graph = new Graph(5, true);
        // Component 1: 0->1
        graph.addEdge(0, 1, 1);
        // Component 2: 2->3->4 (disconnected from component 1)
        graph.addEdge(2, 3, 1);
        graph.addEdge(3, 4, 1);

        // Step 1: SCC Detection
        StronglyConnectedComponents scc = new StronglyConnectedComponents(graph, metrics);
        List<List<Integer>> components = scc.findSCCs();

        // Should have 5 components (all single nodes in this DAG)
        assertEquals(5, components.size());

        // Step 2: Topological Sort
        Graph condensation = scc.getCondensationGraph();
        TopologicalSort topo = new TopologicalSort(condensation, metrics);
        List<Integer> topoOrder = topo.topologicalOrder();

        assertFalse(topoOrder.isEmpty());

        // Step 3: Path Finding - nodes 2,3,4 should be unreachable from node 0
        DAGShortestPath sp = new DAGShortestPath(condensation, metrics);
        int[] shortestDist = sp.shortestPaths(0, topoOrder);

        assertEquals(0, shortestDist[0]);
        assertEquals(1, shortestDist[1]);
        assertEquals(Integer.MAX_VALUE, shortestDist[2]);
        assertEquals(Integer.MAX_VALUE, shortestDist[3]);
        assertEquals(Integer.MAX_VALUE, shortestDist[4]);
    }
}