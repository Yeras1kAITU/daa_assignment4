package graph;

import graph.scc.StronglyConnectedComponents;
import graph.topo.TopologicalSort;
import graph.dagsp.DAGShortestPath;

import java.util.ArrayList;
import java.util.List;

// Analyzes performance of graph algorithms and provides optimization insights
public class PerformanceAnalyzer {
    private final List<PerformanceResult> results;

    public PerformanceAnalyzer() {
        this.results = new ArrayList<>();
    }

    // Runs complete algorithm pipeline and collects performance metrics
    public PerformanceResult analyzeGraph(Graph graph, String graphName) {
        Metrics metrics = new Metrics();
        PerformanceResult result = new PerformanceResult(graphName, graph.getNodeCount(),
                countEdges(graph));

        // SCC Analysis
        metrics.reset();
        StronglyConnectedComponents scc = new StronglyConnectedComponents(graph, metrics);
        List<List<Integer>> components = scc.findSCCs();
        result.setSccMetrics(metrics, components.size());

        // Topological Sort on condensation graph
        metrics.reset();
        Graph condensation = scc.getCondensationGraph();
        TopologicalSort topo = new TopologicalSort(condensation, metrics);
        List<Integer> topoOrder = topo.topologicalOrder();
        result.setTopoMetrics(metrics, !topoOrder.isEmpty());

        // Path Finding (if DAG)
        if (!topoOrder.isEmpty()) {
            metrics.reset();
            DAGShortestPath sp = new DAGShortestPath(condensation, metrics);
            sp.shortestPaths(0, topoOrder);
            result.setPathMetrics(metrics);
        }

        results.add(result);
        return result;
    }

    // Generates performance report comparing all analyzed graphs
    public void generateReport() {
        System.out.println("\n" + "=".repeat(30));
        System.out.println("PERFORMANCE ANALYSIS REPORT");
        System.out.println("=".repeat(30));

        for (PerformanceResult result : results) {
            System.out.println("\nGraph: " + result.graphName);
            System.out.println("  Size: " + result.nodeCount + " nodes, " + result.edgeCount + " edges");
            System.out.println("  SCC: " + result.sccTime + "ms, " + result.sccComponents + " components");
            System.out.println("  Topo: " + result.topoTime + "ms, " + (result.isDag ? "DAG" : "Cyclic"));
            if (result.isDag) {
                System.out.println("  Path: " + result.pathTime + "ms");
            }
            System.out.println("  Total: " + result.getTotalTime() + "ms");
        }
    }

    private int countEdges(Graph graph) {
        int edges = 0;
        for (int i = 0; i < graph.getNodeCount(); i++) {
            edges += graph.getNeighbors(i).size();
        }
        return edges;
    }

    public static class PerformanceResult {
        public final String graphName;
        public final int nodeCount;
        public final int edgeCount;

        // SCC Metrics
        public double sccTime;
        public int sccComponents;
        public int sccOperations;

        // Topological Sort Metrics
        public double topoTime;
        public boolean isDag;
        public int topoOperations;

        // Path Finding Metrics
        public double pathTime;
        public int pathOperations;

        public PerformanceResult(String graphName, int nodeCount, int edgeCount) {
            this.graphName = graphName;
            this.nodeCount = nodeCount;
            this.edgeCount = edgeCount;
        }

        public void setSccMetrics(Metrics metrics, int componentCount) {
            this.sccTime = metrics.getElapsedTimeMillis();
            this.sccComponents = componentCount;
            this.sccOperations = metrics.getDfsVisits() + metrics.getEdgeTraversals();
        }

        public void setTopoMetrics(Metrics metrics, boolean isDag) {
            this.topoTime = metrics.getElapsedTimeMillis();
            this.isDag = isDag;
            this.topoOperations = metrics.getQueuePushes() + metrics.getQueuePops();
        }

        public void setPathMetrics(Metrics metrics) {
            this.pathTime = metrics.getElapsedTimeMillis();
            this.pathOperations = metrics.getRelaxOperations();
        }

        public double getTotalTime() {
            return sccTime + topoTime + pathTime;
        }
    }
}