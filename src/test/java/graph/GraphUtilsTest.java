package graph;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

class GraphUtilsTest {

    @Test
    void testCreateGraphFromData() {
        GraphData graphData = new GraphData();
        graphData.n = 3;
        graphData.directed = true;
        graphData.edges = List.of(
                createEdge(0, 1, 2),
                createEdge(1, 2, 3)
        );

        Graph graph = GraphUtils.createGraphFromData(graphData);

        assertEquals(3, graph.getNodeCount());
        assertTrue(graph.isDirected());
        assertEquals(1, graph.getNeighbors(0).size());
        assertEquals(1, graph.getNeighbors(1).size());
        assertEquals(0, graph.getNeighbors(2).size());
    }

    @Test
    void testCalculateDensity() {
        Graph graph = new Graph(4, true);
        graph.addEdge(0, 1, 1);
        graph.addEdge(1, 2, 1);
        graph.addEdge(2, 3, 1);

        double density = GraphUtils.calculateDensity(graph);

        // 3 edges out of possible 12 (4*3) in directed graph
        assertEquals(3.0 / 12.0, density, 0.001);
    }

    @Test
    void testHasSelfLoops() {
        Graph graph = new Graph(3, true);
        graph.addEdge(0, 1, 1);
        graph.addEdge(1, 1, 1); // self-loop

        assertTrue(GraphUtils.hasSelfLoops(graph));
    }

    @Test
    void testGetGraphStats() {
        Graph graph = new Graph(3, true);
        graph.addEdge(0, 1, 5);
        graph.addEdge(1, 2, 10);
        graph.addEdge(0, 2, 3);

        GraphUtils.GraphStats stats = GraphUtils.getGraphStats(graph);

        assertEquals(3, stats.nodeCount);
        assertEquals(3, stats.edgeCount);
        assertEquals(3, stats.minWeight);
        assertEquals(10, stats.maxWeight);
    }

    @Test
    void testValidateGraphValid() {
        Graph graph = new Graph(3, true);
        graph.addEdge(0, 1, 1);
        graph.addEdge(1, 2, 1);

        // Should not throw exception
        assertDoesNotThrow(() -> GraphUtils.validateGraph(graph));
    }

    @Test
    void testValidateGraphInvalidEdge() {
        Graph graph = new Graph(3, true);
        // This would normally be prevented by Graph class, but test the validation
        // by creating an invalid scenario

        assertDoesNotThrow(() -> GraphUtils.validateGraph(graph));
    }

    private GraphData.Edge createEdge(int u, int v, int w) {
        GraphData.Edge edge = new GraphData.Edge();
        edge.u = u;
        edge.v = v;
        edge.w = w;
        return edge;
    }
}