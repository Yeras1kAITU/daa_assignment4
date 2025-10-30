package graph;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Utility class for graph operations, file loading, and validation
 */
public class GraphUtils {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // Creates a Graph instance from GraphData
    public static Graph createGraphFromData(GraphData graphData) {
        if (graphData == null) {
            throw new IllegalArgumentException("GraphData cannot be null");
        }

        Graph graph = new Graph(graphData.n, graphData.directed);

        if (graphData.edges != null) {
            for (GraphData.Edge edge : graphData.edges) {
                graph.addEdge(edge.u, edge.v, edge.w);
            }
        }

        return graph;
    }

    // Loads graph data from JSON file
    public static GraphData loadGraphData(String filePath) throws IOException {
        return objectMapper.readValue(new File(filePath), GraphData.class);
    }

    // Creates graph directly from JSON file
    public static Graph loadGraphFromFile(String filePath) throws IOException {
        GraphData graphData = loadGraphData(filePath);
        return createGraphFromData(graphData);
    }

    // Validates that graph meets basic requirements
    public static void validateGraph(Graph graph) {
        if (graph == null) {
            throw new IllegalArgumentException("Graph cannot be null");
        }

        int n = graph.getNodeCount();
        for (int i = 0; i < n; i++) {
            // Validate that all neighbor nodes are within bounds
            for (Graph.Edge edge : graph.getNeighbors(i)) {
                if (edge.target < 0 || edge.target >= n) {
                    throw new IllegalStateException(
                            "Node " + i + " has edge to invalid node " + edge.target
                    );
                }
            }
        }
    }

    // Calculates graph density
    public static double calculateDensity(Graph graph) {
        int n = graph.getNodeCount();
        if (n <= 1) return 0.0;

        int edgeCount = 0;
        for (int i = 0; i < n; i++) {
            edgeCount += graph.getNeighbors(i).size();
        }

        int maxEdges = graph.isDirected() ? n * (n - 1) : n * (n - 1) / 2;
        return (double) edgeCount / maxEdges;
    }

    // Checks if graph has any self-loops
    public static boolean hasSelfLoops(Graph graph) {
        for (int i = 0; i < graph.getNodeCount(); i++) {
            for (Graph.Edge edge : graph.getNeighbors(i)) {
                if (edge.target == i) {
                    return true;
                }
            }
        }
        return false;
    }

    // Gets basic graph statistics
    public static GraphStats getGraphStats(Graph graph) {
        int nodes = graph.getNodeCount();
        int edges = 0;
        int minWeight = Integer.MAX_VALUE;
        int maxWeight = Integer.MIN_VALUE;

        for (int i = 0; i < nodes; i++) {
            List<Graph.Edge> neighbors = graph.getNeighbors(i);
            edges += neighbors.size();

            for (Graph.Edge edge : neighbors) {
                minWeight = Math.min(minWeight, edge.weight);
                maxWeight = Math.max(maxWeight, edge.weight);
            }
        }

        return new GraphStats(nodes, edges, minWeight, maxWeight, calculateDensity(graph));
    }

    public static class GraphStats {
        public final int nodeCount;
        public final int edgeCount;
        public final int minWeight;
        public final int maxWeight;
        public final double density;

        public GraphStats(int nodeCount, int edgeCount, int minWeight, int maxWeight, double density) {
            this.nodeCount = nodeCount;
            this.edgeCount = edgeCount;
            this.minWeight = minWeight;
            this.maxWeight = maxWeight;
            this.density = density;
        }

        @Override
        public String toString() {
            return String.format(
                    "GraphStats{nodes=%d, edges=%d, weight=[%d,%d], density=%.3f}",
                    nodeCount, edgeCount, minWeight, maxWeight, density
            );
        }
    }
}