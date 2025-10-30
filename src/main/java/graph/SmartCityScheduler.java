package graph;

import graph.scc.StronglyConnectedComponents;
import graph.topo.TopologicalSort;
import graph.dagsp.DAGShortestPath;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.util.*;

/**
 * Main application orchestrator for smart city scheduling using graph algorithms.
 * SCC detection, topological sorting, and path finding for task scheduling.
 */
public class SmartCityScheduler {
    private final Metrics metrics;
    private final ObjectMapper objectMapper;

    public SmartCityScheduler() {
        this.metrics = new Metrics();
        this.objectMapper = new ObjectMapper();
    }

    // Processes a graph file through the complete algorithm pipeline
    public void processGraph(String filename) {
        try {
            System.out.println("\n" + "=".repeat(60));
            System.out.println("PROCESSING: " + filename);
            System.out.println("=".repeat(60));

            // Load and validate graph
            GraphData graphData = loadGraphData("data/" + filename);
            Graph graph = GraphUtils.createGraphFromData(graphData);
            GraphUtils.validateGraph(graph);

            printGraphSummary(graphData, graph);

            // Algorithm pipeline
            processSCCs(graph);

        } catch (Exception e) {
            System.err.println("!!!Error processing " + filename + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void processSCCs(Graph graph) {
        System.out.println("\n1. STRONGLY CONNECTED COMPONENTS ANALYSIS");
        System.out.println("-".repeat(50));

        metrics.reset();
        StronglyConnectedComponents scc = new StronglyConnectedComponents(graph, metrics);
        List<List<Integer>> components = scc.findSCCs();

        printSCCResults(components);
        printMetrics("SCC Detection");

        // Build condensation graph and continue pipeline
        Graph condensation = scc.getCondensationGraph();
        processTopologicalSort(condensation, components, scc.getComponentIds());
    }

    private void processTopologicalSort(Graph condensation, List<List<Integer>> components, int[] componentIds) {
        System.out.println("\n2. TOPOLOGICAL SORT & TASK SCHEDULING");
        System.out.println("-".repeat(50));

        metrics.reset();
        TopologicalSort topo = new TopologicalSort(condensation, metrics);
        List<Integer> topoOrder = topo.topologicalOrder();

        if (topoOrder.isEmpty()) {
            System.out.println("!!!Graph contains cycles - cannot compute topological order");
            return;
        }

        printTopologicalResults(topoOrder, components);
        printMetrics("Topological Sort");

        // Continue with path finding on the DAG
        processPathFinding(condensation, topoOrder, components);
    }

    private void processPathFinding(Graph condensation, List<Integer> topoOrder, List<List<Integer>> components) {
        System.out.println("\n3. PATH FINDING & CRITICAL PATH ANALYSIS");
        System.out.println("-".repeat(50));

        // Shortest paths
        metrics.reset();
        DAGShortestPath sp = new DAGShortestPath(condensation, metrics);
        int sourceComponent = 0; // Use first component as source

        int[] shortestDist = sp.shortestPaths(sourceComponent, topoOrder);
        printShortestPathResults(shortestDist, sourceComponent, components);
        printMetrics("Shortest Paths");

        // Longest paths (critical path)
        metrics.reset();
        DAGShortestPath.CriticalPath criticalPath = sp.findCriticalPath(sourceComponent, topoOrder);
        printCriticalPathResults(criticalPath, components);
        printMetrics("Longest Paths");
    }

    private GraphData loadGraphData(String filePath) throws Exception {
        return objectMapper.readValue(new File(filePath), GraphData.class);
    }

    private void printGraphSummary(GraphData graphData, Graph graph) {
        GraphUtils.GraphStats stats = GraphUtils.getGraphStats(graph);
        System.out.println("Graph Summary:");
        System.out.println("   • Nodes: " + stats.nodeCount);
        System.out.println("   • Edges: " + stats.edgeCount);
        System.out.println("   • Density: " + String.format("%.3f", stats.density));
        System.out.println("   • Weight Range: [" + stats.minWeight + ", " + stats.maxWeight + "]");
        System.out.println("   • Directed: " + graphData.directed);
        System.out.println("   • Source Node: " + graphData.source);
    }

    private void printSCCResults(List<List<Integer>> components) {
        System.out.println("Found " + components.size() + " strongly connected components:");

        for (int i = 0; i < components.size(); i++) {
            List<Integer> component = components.get(i);
            String type = component.size() == 1 ? " (Single)" : " (Cycle)";
            System.out.println("   Component " + i + type + ": " + component + " [size: " + component.size() + "]");
        }

        // Analysis
        int cycleComponents = (int) components.stream().filter(c -> c.size() > 1).count();
        int singleNodeComponents = components.size() - cycleComponents;

        System.out.println("\nSCC Analysis:");
        System.out.println("   • Total Components: " + components.size());
        System.out.println("   • Cycle Components: " + cycleComponents);
        System.out.println("   • Single Node Components: " + singleNodeComponents);
    }

    private void printTopologicalResults(List<Integer> topoOrder, List<List<Integer>> components) {
        System.out.println("Valid topological order of components:");
        System.out.println("   Component Order: " + topoOrder);

        // Create task execution order
        List<Integer> taskOrder = new ArrayList<>();
        for (int compId : topoOrder) {
            taskOrder.addAll(components.get(compId));
        }

        System.out.println("   Task Execution Order: " + taskOrder);
        System.out.println("\nScheduling Implications:");
        System.out.println("   • " + topoOrder.size() + " components can be scheduled in order");
        System.out.println("   • " + taskOrder.size() + " total tasks to execute");
    }

    private void printShortestPathResults(int[] shortestDist, int source, List<List<Integer>> components) {
        System.out.println("Shortest paths from component " + source + " (" + components.get(source) + "):");

        for (int i = 0; i < shortestDist.length; i++) {
            if (shortestDist[i] != Integer.MAX_VALUE) {
                System.out.println("   → Component " + i + " (" + components.get(i) + "): " +
                        shortestDist[i] + " units");
            } else {
                System.out.println("   → Component " + i + " (" + components.get(i) + "): unreachable");
            }
        }
    }

    private void printCriticalPathResults(DAGShortestPath.CriticalPath criticalPath, List<List<Integer>> components) {
        System.out.println("⚡ Critical Path Analysis:");
        System.out.println("   • Critical Path Length: " + criticalPath.length + " units");
        System.out.println("   • Critical Path Components: " + criticalPath.path);

        // Convert component path to task path
        List<Integer> taskPath = new ArrayList<>();
        for (int compId : criticalPath.path) {
            taskPath.addAll(components.get(compId));
        }

        System.out.println("   • Critical Task Sequence: " + taskPath);
        System.out.println("\nScheduling Insight:");
        System.out.println("   This path determines the minimum project duration");
        System.out.println("   Tasks on this path cannot be delayed without affecting overall timeline");
    }

    private void printMetrics(String operation) {
        System.out.println("\n" + operation + " Metrics:");
        System.out.println("   • Time: " + String.format("%.3f", metrics.getElapsedTimeMillis()) + " ms");
        System.out.println("   • DFS Visits: " + metrics.getDfsVisits());
        System.out.println("   • Edge Traversals: " + metrics.getEdgeTraversals());
        System.out.println("   • Queue Operations: " +
                (metrics.getQueuePushes() + metrics.getQueuePops()));
        System.out.println("   • Relax Operations: " + metrics.getRelaxOperations());
    }

    public static void main(String[] args) {
        SmartCityScheduler scheduler = new SmartCityScheduler();

        // Process all datasets
        String[] datasets = {
                "small_dag.json", "small_cycle.json", "small_mixed.json",
                "medium_multiple_scc.json", "medium_complex_dag.json", "medium_mixed.json",
                "large_sparse.json", "large_medium.json", "large_complex_scc.json"
        };

        System.out.println("Smart City Scheduling - Graph Algorithms Pipeline");
        System.out.println("Repository: https://github.com/Yeras1kAITU/daa_assignment4");
        System.out.println("Algorithms: SCC (Kosaraju) → Topological Sort (Kahn) → DAG Shortest/Longest Paths");

        for (String dataset : datasets) {
            scheduler.processGraph(dataset);
        }

        System.out.println("\n" + "-".repeat(7));
        System.out.println("All datasets processed successfully!");
        System.out.println("-".repeat(7));
    }
}