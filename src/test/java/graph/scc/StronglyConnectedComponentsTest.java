package graph.scc;

import graph.Graph;
import graph.Metrics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

class StronglyConnectedComponentsTest {
    private Metrics metrics;

    @BeforeEach
    void setUp() {
        metrics = new Metrics();
    }

    @Test
    void testSingleNodeGraph() {
        Graph graph = new Graph(1, true);
        StronglyConnectedComponents scc = new StronglyConnectedComponents(graph, metrics);

        List<List<Integer>> components = scc.findSCCs();

        assertEquals(1, components.size());
        assertEquals(1, components.get(0).size());
        assertEquals(0, components.get(0).get(0));
    }

    @Test
    void testSimpleCycle() {
        Graph graph = new Graph(3, true);
        graph.addEdge(0, 1, 1);
        graph.addEdge(1, 2, 1);
        graph.addEdge(2, 0, 1);

        StronglyConnectedComponents scc = new StronglyConnectedComponents(graph, metrics);
        List<List<Integer>> components = scc.findSCCs();

        assertEquals(1, components.size());
        assertEquals(3, components.get(0).size());
        assertTrue(components.get(0).contains(0));
        assertTrue(components.get(0).contains(1));
        assertTrue(components.get(0).contains(2));
    }

    @Test
    void testMultipleSCCs() {
        Graph graph = new Graph(5, true);
        // First cycle: 0->1->2->0
        graph.addEdge(0, 1, 1);
        graph.addEdge(1, 2, 1);
        graph.addEdge(2, 0, 1);
        // Second cycle: 3->4->3
        graph.addEdge(3, 4, 1);
        graph.addEdge(4, 3, 1);

        StronglyConnectedComponents scc = new StronglyConnectedComponents(graph, metrics);
        List<List<Integer>> components = scc.findSCCs();

        assertEquals(2, components.size());

        // Check that each component has correct nodes
        boolean foundFirstCycle = false;
        boolean foundSecondCycle = false;

        for (List<Integer> component : components) {
            if (component.size() == 3) {
                assertTrue(component.contains(0));
                assertTrue(component.contains(1));
                assertTrue(component.contains(2));
                foundFirstCycle = true;
            } else if (component.size() == 2) {
                assertTrue(component.contains(3));
                assertTrue(component.contains(4));
                foundSecondCycle = true;
            }
        }

        assertTrue(foundFirstCycle);
        assertTrue(foundSecondCycle);
    }

    @Test
    void testDAGHasEachNodeAsSeparateSCC() {
        Graph graph = new Graph(4, true);
        graph.addEdge(0, 1, 1);
        graph.addEdge(1, 2, 1);
        graph.addEdge(2, 3, 1);

        StronglyConnectedComponents scc = new StronglyConnectedComponents(graph, metrics);
        List<List<Integer>> components = scc.findSCCs();

        assertEquals(4, components.size());
        for (List<Integer> component : components) {
            assertEquals(1, component.size());
        }
    }
}