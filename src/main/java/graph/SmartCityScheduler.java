package graph;

import graph.scc.StronglyConnectedComponents;
import graph.topo.TopologicalSort;
import graph.dagsp.DAGShortestPath;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.util.*;

public class SmartCityScheduler {
    private final ObjectMapper objectMapper;
    private final ResultExporter resultExporter;
    private final List<Map<String, Object>> allResults;

    public SmartCityScheduler() {
        this.objectMapper = new ObjectMapper();
        this.resultExporter = new ResultExporter();
        this.allResults = new ArrayList<>();
    }

    public void processGraph(String filename) {
        try {
            System.out.println("\n" + "=".repeat(60));
            System.out.println("PROCESSING: " + filename);
            System.out.println("=".repeat(60));

            GraphData graphData = loadGraphData("data/" + filename);
            Graph graph = GraphUtils.createGraphFromData(graphData);
            GraphUtils.validateGraph(graph);

            printGraphSummary(graphData, graph);

            Map<String, Object> result = processSCCs(graph, graphData.source, filename, graphData);
            if (result != null) {
                allResults.add(result);
            }

        } catch (Exception e) {
            System.err.println("Error processing " + filename + ": " + e.getMessage());
        }
    }

    private Map<String, Object> processSCCs(Graph graph, int originalSource, String filename, GraphData graphData) {
        System.out.println("\n1. STRONGLY CONNECTED COMPONENTS ANALYSIS");
        System.out.println("-".repeat(50));

        Metrics metrics = new Metrics();
        StronglyConnectedComponents scc = new StronglyConnectedComponents(graph, metrics);
        List<List<Integer>> components = scc.findSCCs();

        Map<String, Object> result = new HashMap<>();
        result.put("filename", filename);
        result.put("original_source", originalSource);
        result.put("components", components);
        result.put("node_count", graphData.n);
        result.put("edge_count", graphData.edges.size());
        result.put("directed", graphData.directed);
        result.put("scc_count", components.size());

        printSCCResults(components);
        printMetrics("SCC Detection", metrics);

        result.put("scc_time_ms", metrics.getElapsedTimeMillis());
        result.put("scc_dfs_visits", metrics.getDfsVisits());
        result.put("scc_edge_traversals", metrics.getEdgeTraversals());

        Graph condensation = scc.getCondensationGraph();
        return processTopologicalSort(condensation, components, scc.getComponentIds(), originalSource, filename, result, graphData);
    }

    private Map<String, Object> processTopologicalSort(Graph condensation, List<List<Integer>> components,
                                                       int[] componentIds, int originalSource, String filename,
                                                       Map<String, Object> result, GraphData graphData) {
        System.out.println("\n2. TOPOLOGICAL SORT & TASK SCHEDULING");
        System.out.println("-".repeat(50));

        Metrics metrics = new Metrics();
        TopologicalSort topo = new TopologicalSort(condensation, metrics);
        List<Integer> topoOrder = topo.topologicalOrder();

        result.put("topological_order", topoOrder);
        result.put("is_dag", !topoOrder.isEmpty());
        result.put("topo_time_ms", metrics.getElapsedTimeMillis());
        result.put("topo_queue_pushes", metrics.getQueuePushes());
        result.put("topo_queue_pops", metrics.getQueuePops());

        if (topoOrder.isEmpty()) {
            System.out.println("Graph contains cycles - cannot compute topological order");
            resultExporter.exportSCCResults(filename, components, graphData);
            return result;
        }

        printTopologicalResults(topoOrder, components);
        printMetrics("Topological Sort", metrics);

        return processPathFinding(condensation, topoOrder, components, componentIds, originalSource, filename, result);
    }

    private Map<String, Object> processPathFinding(Graph condensation, List<Integer> topoOrder,
                                                   List<List<Integer>> components, int[] componentIds,
                                                   int originalSource, String filename, Map<String, Object> result) {
        System.out.println("\n3. PATH FINDING & CRITICAL PATH ANALYSIS");
        System.out.println("-".repeat(50));

        int sourceComponent = mapSourceToCondensation(originalSource, componentIds, components);
        System.out.println("Using source node " + originalSource + " -> Component " + sourceComponent);

        result.put("source_component", sourceComponent);

        Metrics shortestMetrics = new Metrics();
        DAGShortestPath shortestSp = new DAGShortestPath(condensation, shortestMetrics);
        int[] shortestDist = shortestSp.shortestPaths(sourceComponent, topoOrder);
        result.put("shortest_distances", shortestDist);
        result.put("shortest_paths_time_ms", shortestMetrics.getElapsedTimeMillis());

        printShortestPathResults(shortestDist, sourceComponent, components);
        printMetrics("Shortest Paths", shortestMetrics);

        Metrics longestMetrics = new Metrics();
        DAGShortestPath longestSp = new DAGShortestPath(condensation, longestMetrics);
        int[] longestDist = longestSp.longestPaths(sourceComponent, topoOrder);
        DAGShortestPath.CriticalPath criticalPath = longestSp.findCriticalPath(sourceComponent, topoOrder);
        result.put("longest_distances", longestDist);
        result.put("critical_path", criticalPath);
        result.put("longest_paths_time_ms", longestMetrics.getElapsedTimeMillis());

        printCriticalPathResults(criticalPath, components);
        printMetrics("Longest Paths", longestMetrics);

        double sccTime = (Double) result.get("scc_time_ms");
        double topoTime = (Double) result.get("topo_time_ms");
        double shortestTime = (Double) result.get("shortest_paths_time_ms");
        double longestTime = (Double) result.get("longest_paths_time_ms");
        double totalTime = sccTime + topoTime + shortestTime + longestTime;
        int totalRelaxOperations = shortestMetrics.getRelaxOperations() + longestMetrics.getRelaxOperations();

        result.put("total_time_ms", totalTime);
        result.put("relax_operations", totalRelaxOperations);

        resultExporter.exportGraphResults(result, condensation.getNodeCount());

        return result;
    }

    private int mapSourceToCondensation(int originalSource, int[] componentIds, List<List<Integer>> components) {
        if (originalSource < 0 || originalSource >= componentIds.length) {
            System.out.println("Source node " + originalSource + " is invalid, using component 0");
            return 0;
        }

        int sourceComponent = componentIds[originalSource];
        if (sourceComponent < 0 || sourceComponent >= components.size()) {
            System.out.println("Source node " + originalSource + " not found in any component, using component 0");
            return 0;
        }

        System.out.println("Source node " + originalSource + " mapped to component " + sourceComponent);
        return sourceComponent;
    }

    private GraphData loadGraphData(String filePath) throws Exception {
        return objectMapper.readValue(new File(filePath), GraphData.class);
    }

    private void printGraphSummary(GraphData graphData, Graph graph) {
        GraphUtils.GraphStats stats = GraphUtils.getGraphStats(graph);
        System.out.println("Graph Summary:");
        System.out.println("   Nodes: " + stats.nodeCount);
        System.out.println("   Edges: " + stats.edgeCount);
        System.out.println("   Density: " + String.format("%.3f", stats.density));
        System.out.println("   Weight Range: [" + stats.minWeight + ", " + stats.maxWeight + "]");
        System.out.println("   Directed: " + graphData.directed);
        System.out.println("   Source Node: " + graphData.source);
    }

    private void printSCCResults(List<List<Integer>> components) {
        System.out.println("Found " + components.size() + " strongly connected components:");

        for (int i = 0; i < components.size(); i++) {
            List<Integer> component = components.get(i);
            String type = component.size() == 1 ? " (Single)" : " (Cycle)";
            System.out.println("   Component " + i + type + ": " + component + " [size: " + component.size() + "]");
        }

        int cycleComponents = (int) components.stream().filter(c -> c.size() > 1).count();
        int singleNodeComponents = components.size() - cycleComponents;

        System.out.println("SCC Analysis:");
        System.out.println("   Total Components: " + components.size());
        System.out.println("   Cycle Components: " + cycleComponents);
        System.out.println("   Single Node Components: " + singleNodeComponents);
    }

    private void printTopologicalResults(List<Integer> topoOrder, List<List<Integer>> components) {
        System.out.println("Valid topological order of components:");
        System.out.println("   Component Order: " + topoOrder);

        List<Integer> taskOrder = new ArrayList<>();
        for (int compId : topoOrder) {
            taskOrder.addAll(components.get(compId));
        }

        System.out.println("   Task Execution Order: " + taskOrder);
        System.out.println("Scheduling Implications:");
        System.out.println("   " + topoOrder.size() + " components can be scheduled in order");
        System.out.println("   " + taskOrder.size() + " total tasks to execute");
    }

    private void printShortestPathResults(int[] shortestDist, int source, List<List<Integer>> components) {
        System.out.println("Shortest paths from component " + source + ":");

        int reachableCount = 0;
        for (int i = 0; i < shortestDist.length; i++) {
            if (shortestDist[i] != Integer.MAX_VALUE) {
                System.out.println("   -> Component " + i + ": " + shortestDist[i] + " units");
                reachableCount++;
            } else {
                System.out.println("   -> Component " + i + ": unreachable");
            }
        }
        System.out.println("   Reachable components: " + reachableCount + "/" + shortestDist.length);
    }

    private void printCriticalPathResults(DAGShortestPath.CriticalPath criticalPath, List<List<Integer>> components) {
        System.out.println("Critical Path Analysis:");
        System.out.println("   Critical Path Length: " + criticalPath.length + " units");
        System.out.println("   Critical Path Components: " + criticalPath.path);

        List<Integer> taskPath = new ArrayList<>();
        for (int compId : criticalPath.path) {
            taskPath.addAll(components.get(compId));
        }

        System.out.println("   Critical Task Sequence: " + taskPath);
        System.out.println("Scheduling Insight:");
        System.out.println("   This path determines the minimum project duration");
    }

    private void printMetrics(String operation, Metrics metrics) {
        System.out.println(operation + " Metrics:");
        System.out.println("   Time: " + String.format("%.3f", metrics.getElapsedTimeMillis()) + " ms");
        System.out.println("   DFS Visits: " + metrics.getDfsVisits());
        System.out.println("   Edge Traversals: " + metrics.getEdgeTraversals());
        System.out.println("   Queue Pushes: " + metrics.getQueuePushes());
        System.out.println("   Queue Pops: " + metrics.getQueuePops());
        System.out.println("   Relax Operations: " + metrics.getRelaxOperations());
    }

    public static void main(String[] args) {
        SmartCityScheduler scheduler = new SmartCityScheduler();

        String[] datasets = {
                "small_dag.json", "small_cycle.json", "small_mixed.json",
                "medium_multiple_scc.json", "medium_complex_dag.json", "medium_mixed.json",
                "large_sparse.json", "large_medium.json", "large_complex_scc.json"
        };

        System.out.println("Smart City Scheduling - Graph Algorithms Pipeline");
        System.out.println("Results will be saved to: results/");
        System.out.println("Algorithms: SCC -> Topological Sort -> DAG Shortest/Longest Paths");

        long startTime = System.currentTimeMillis();

        for (String dataset : datasets) {
            scheduler.processGraph(dataset);
        }

        long endTime = System.currentTimeMillis();

        scheduler.resultExporter.createSummaryReports(scheduler.allResults);

        System.out.println("\n" + "=".repeat(50));
        System.out.println("ALL DATASETS PROCESSED SUCCESSFULLY!");
        System.out.println("=".repeat(50));
        System.out.println("Performance Summary:");
        System.out.println("   Total datasets processed: " + datasets.length);
        System.out.println("   Total processing time: " + (endTime - startTime) + " ms");
        System.out.println("   Average time per dataset: " + (endTime - startTime) / datasets.length + " ms");
        System.out.println("Results exported to: " + scheduler.resultExporter.getResultsDirectory());
        System.out.println("=".repeat(50));
    }
}