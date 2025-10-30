package graph;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import graph.dagsp.DAGShortestPath;

import java.io.*;
import java.util.*;

public class ResultExporter {
    private final ObjectMapper jsonMapper;
    private final String baseDir;

    public ResultExporter() {
        this.jsonMapper = new ObjectMapper();
        this.jsonMapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.baseDir = "results";
        new File(baseDir).mkdirs();
        new File(baseDir + "/csv").mkdirs();
        new File(baseDir + "/json").mkdirs();
    }

    public void exportGraphResults(Map<String, Object> results, int condensationSize) {
        String filename = (String) results.get("filename");
        String baseName = filename.replace(".json", "");

        try {
            exportGraphJson(results, baseName);
            exportGraphCsv(results, condensationSize, baseName);
            System.out.println("Results exported for: " + baseName);

        } catch (Exception e) {
            System.err.println("Export failed for " + filename + ": " + e.getMessage());
        }
    }

    public void exportSCCResults(String filename, List<List<Integer>> components, GraphData graphData) {
        String baseName = filename.replace(".json", "");

        try {
            Map<String, Object> sccData = new HashMap<>();
            sccData.put("filename", filename);
            sccData.put("components", createComponentObjects(components));
            sccData.put("component_count", components.size());
            sccData.put("node_count", graphData.n);
            sccData.put("edge_count", graphData.edges.size());
            sccData.put("source", graphData.source);
            sccData.put("has_cycles", true);

            String jsonFile = baseDir + "/json/" + baseName + "_scc.json";
            jsonMapper.writeValue(new File(jsonFile), sccData);

        } catch (Exception e) {
            System.err.println("SCC export failed for " + filename + ": " + e.getMessage());
        }
    }

    private void exportGraphJson(Map<String, Object> results, String baseName) throws IOException {
        Map<String, Object> exportData = new HashMap<>();

        exportData.put("filename", results.get("filename"));
        exportData.put("node_count", results.get("node_count"));
        exportData.put("edge_count", results.get("edge_count"));
        exportData.put("directed", results.get("directed"));
        exportData.put("original_source", results.get("original_source"));
        exportData.put("source_component", results.get("source_component"));
        exportData.put("is_dag", results.get("is_dag"));

        List<List<Integer>> components = (List<List<Integer>>) results.get("components");
        exportData.put("components", createComponentObjects(components));

        if (results.containsKey("topological_order")) {
            List<Integer> topoOrder = (List<Integer>) results.get("topological_order");
            exportData.put("componentOrder", topoOrder);
            exportData.put("taskOrder", createTaskOrder(components, topoOrder));
        }

        exportData.put("scc_time_ms", results.get("scc_time_ms"));
        exportData.put("topo_time_ms", results.get("topo_time_ms"));
        exportData.put("shortest_paths_time_ms", results.get("shortest_paths_time_ms"));
        exportData.put("longest_paths_time_ms", results.get("longest_paths_time_ms"));
        exportData.put("total_time_ms", results.get("total_time_ms"));

        exportData.put("shortest_distances", createDistanceDisplay((int[]) results.get("shortest_distances")));
        exportData.put("longest_distances", createDistanceDisplay((int[]) results.get("longest_distances")));

        if (results.containsKey("critical_path")) {
            exportData.put("critical_path", results.get("critical_path"));
        }

        Map<String, Object> metricsData = new HashMap<>();
        metricsData.put("scc_count", components.size());
        metricsData.put("dfs_visits", results.get("scc_dfs_visits"));
        metricsData.put("edge_traversals", results.get("scc_edge_traversals"));
        metricsData.put("queue_pushes", results.get("topo_queue_pushes"));
        metricsData.put("queue_pops", results.get("topo_queue_pops"));
        metricsData.put("relax_operations", results.get("relax_operations"));

        exportData.put("metrics", metricsData);

        String jsonFile = baseDir + "/json/" + baseName + "_full.json";
        jsonMapper.writeValue(new File(jsonFile), exportData);
    }

    private List<Map<String, Object>> createComponentObjects(List<List<Integer>> components) {
        List<Map<String, Object>> componentObjects = new ArrayList<>();
        for (int i = 0; i < components.size(); i++) {
            Map<String, Object> component = new HashMap<>();
            component.put("id", i);
            component.put("size", components.get(i).size());
            component.put("nodes", components.get(i));
            componentObjects.add(component);
        }
        return componentObjects;
    }

    private List<Integer> createTaskOrder(List<List<Integer>> components, List<Integer> componentOrder) {
        List<Integer> taskOrder = new ArrayList<>();
        for (int compId : componentOrder) {
            taskOrder.addAll(components.get(compId));
        }
        return taskOrder;
    }

    private List<Object> createDistanceDisplay(int[] distances) {
        List<Object> display = new ArrayList<>();
        for (int distance : distances) {
            if (distance == Integer.MAX_VALUE || distance == Integer.MIN_VALUE) {
                display.add("UNREACHABLE");
            } else {
                display.add(distance);
            }
        }
        return display;
    }

    private void exportGraphCsv(Map<String, Object> results, int condensationSize, String baseName) throws IOException {
        exportAlgorithmMetricsCsv(results, baseName);
        exportComponentsCsv(results, baseName);
        exportPathsCsv(results, condensationSize, baseName);
    }

    private void exportAlgorithmMetricsCsv(Map<String, Object> results, String baseName) throws IOException {
        String csvFile = baseDir + "/csv/" + baseName + "_metrics.csv";

        try (PrintWriter writer = new PrintWriter(new FileWriter(csvFile))) {
            writer.println("algorithm,time_ms,operations,components,source,has_cycles,critical_path_length");

            String filename = (String) results.get("filename");
            int source = (Integer) results.get("original_source");
            int components = ((List<?>) results.get("components")).size();
            boolean hasCycles = !(Boolean) results.get("is_dag");
            int criticalPathLength = getCriticalPathLength(results);

            double sccTime = (Double) results.get("scc_time_ms");
            int sccOperations = (Integer) results.get("scc_dfs_visits") + (Integer) results.get("scc_edge_traversals");
            writer.printf("SCC,%.3f,%d,%d,%d,%b,%d%n", sccTime, sccOperations, components, source, hasCycles, criticalPathLength);

            if (results.containsKey("topo_time_ms")) {
                double topoTime = (Double) results.get("topo_time_ms");
                int topoOperations = (Integer) results.get("topo_queue_pushes") + (Integer) results.get("topo_queue_pops");
                writer.printf("TopologicalSort,%.3f,%d,%d,%d,%b,%d%n", topoTime, topoOperations, components, source, hasCycles, criticalPathLength);
            }

            if (results.containsKey("shortest_paths_time_ms")) {
                double pathTime = (Double) results.get("shortest_paths_time_ms") + (Double) results.get("longest_paths_time_ms");
                int relaxOperations = (Integer) results.get("relax_operations");
                writer.printf("PathFinding,%.3f,%d,%d,%d,%b,%d%n", pathTime, relaxOperations, components, source, hasCycles, criticalPathLength);
            }

            if (results.containsKey("total_time_ms")) {
                double totalTime = (Double) results.get("total_time_ms");
                int totalOperations = sccOperations +
                        (results.containsKey("topo_queue_pushes") ? (Integer) results.get("topo_queue_pushes") + (Integer) results.get("topo_queue_pops") : 0) +
                        (Integer) results.get("relax_operations");
                writer.printf("TOTAL,%.3f,%d,%d,%d,%b,%d%n", totalTime, totalOperations, components, source, hasCycles, criticalPathLength);
            }
        }
    }

    private void exportComponentsCsv(Map<String, Object> results, String baseName) throws IOException {
        String csvFile = baseDir + "/csv/" + baseName + "_components.csv";

        try (PrintWriter writer = new PrintWriter(new FileWriter(csvFile))) {
            writer.println("component_id,size,node_list,is_cycle");

            List<List<Integer>> components = (List<List<Integer>>) results.get("components");
            for (int i = 0; i < components.size(); i++) {
                List<Integer> component = components.get(i);
                boolean isCycle = component.size() > 1;
                writer.printf("%d,%d,\"%s\",%b%n", i, component.size(), component, isCycle);
            }
        }
    }

    private void exportPathsCsv(Map<String, Object> results, int condensationSize, String baseName) throws IOException {
        String csvFile = baseDir + "/csv/" + baseName + "_paths.csv";

        try (PrintWriter writer = new PrintWriter(new FileWriter(csvFile))) {
            writer.println("component_id,shortest_distance,longest_distance,reachable,is_source_component");

            int[] shortestDist = (int[]) results.get("shortest_distances");
            int[] longestDist = (int[]) results.get("longest_distances");
            int sourceComponent = (Integer) results.get("source_component");

            for (int i = 0; i < condensationSize; i++) {
                boolean reachable = shortestDist[i] != Integer.MAX_VALUE && longestDist[i] != Integer.MIN_VALUE;
                boolean isSource = (i == sourceComponent);
                String shortestDisplay = reachable ? String.valueOf(shortestDist[i]) : "UNREACHABLE";
                String longestDisplay = reachable ? String.valueOf(longestDist[i]) : "UNREACHABLE";

                writer.printf("%d,%s,%s,%b,%b%n", i, shortestDisplay, longestDisplay, reachable, isSource);
            }
        }
    }

    public void createSummaryReports(List<Map<String, Object>> allResults) {
        try {
            createSummaryCsv(allResults);
            createSummaryJson(allResults);
            System.out.println("Summary reports created");

        } catch (Exception e) {
            System.err.println("Summary report creation failed: " + e.getMessage());
        }
    }

    private void createSummaryCsv(List<Map<String, Object>> allResults) throws IOException {
        String summaryFile = baseDir + "/summary.csv";

        try (PrintWriter writer = new PrintWriter(new FileWriter(summaryFile))) {
            writer.println("filename,nodes,edges,components,source,scc_time_ms,topo_time_ms,path_time_ms,total_time_ms,critical_path_length,has_cycles");

            for (Map<String, Object> result : allResults) {
                String filename = (String) result.get("filename");
                int nodes = (Integer) result.get("node_count");
                int edges = (Integer) result.get("edge_count");
                int components = ((List<?>) result.get("components")).size();
                int source = (Integer) result.get("original_source");
                double sccTime = (Double) result.get("scc_time_ms");
                double topoTime = result.containsKey("topo_time_ms") ? (Double) result.get("topo_time_ms") : 0.0;
                double pathTime = result.containsKey("total_time_ms") ? (Double) result.get("total_time_ms") - sccTime - topoTime : 0.0;
                double totalTime = result.containsKey("total_time_ms") ? (Double) result.get("total_time_ms") : sccTime;
                int criticalPathLength = getCriticalPathLength(result);
                boolean hasCycles = !(Boolean) result.get("is_dag");

                writer.printf("%s,%d,%d,%d,%d,%.3f,%.3f,%.3f,%.3f,%d,%b%n",
                        filename, nodes, edges, components, source, sccTime, topoTime, pathTime, totalTime, criticalPathLength, hasCycles);
            }
        }
    }

    private void createSummaryJson(List<Map<String, Object>> allResults) throws IOException {
        Map<String, Object> summary = new HashMap<>();
        summary.put("total_datasets", allResults.size());

        List<Map<String, Object>> datasetSummaries = new ArrayList<>();
        Map<String, Object> statistics = new HashMap<>();

        int totalNodes = 0;
        int totalEdges = 0;
        int totalComponents = 0;
        double totalSccTime = 0;
        double totalTopoTime = 0;
        double totalPathTime = 0;
        double totalTime = 0;
        int dagCount = 0;
        int cyclicCount = 0;

        for (Map<String, Object> result : allResults) {
            Map<String, Object> datasetSummary = createDatasetSummary(result);
            datasetSummaries.add(datasetSummary);

            totalNodes += (Integer) result.get("node_count");
            totalEdges += (Integer) result.get("edge_count");
            totalComponents += ((List<?>) result.get("components")).size();
            totalSccTime += (Double) result.get("scc_time_ms");

            if (result.containsKey("topo_time_ms")) {
                totalTopoTime += (Double) result.get("topo_time_ms");
            }
            if (result.containsKey("total_time_ms")) {
                totalTime += (Double) result.get("total_time_ms");
                totalPathTime += (Double) result.get("total_time_ms") - (Double) result.get("scc_time_ms") -
                        (result.containsKey("topo_time_ms") ? (Double) result.get("topo_time_ms") : 0);
            }

            if ((Boolean) result.get("is_dag")) {
                dagCount++;
            } else {
                cyclicCount++;
            }
        }

        summary.put("datasets", datasetSummaries);

        statistics.put("average_nodes", totalNodes / (double) allResults.size());
        statistics.put("average_edges", totalEdges / (double) allResults.size());
        statistics.put("average_components", totalComponents / (double) allResults.size());
        statistics.put("average_scc_time_ms", totalSccTime / allResults.size());
        statistics.put("average_topo_time_ms", totalTopoTime / allResults.size());
        statistics.put("average_path_time_ms", totalPathTime / allResults.size());
        statistics.put("average_total_time_ms", totalTime / allResults.size());
        statistics.put("dag_count", dagCount);
        statistics.put("cyclic_count", cyclicCount);
        statistics.put("dag_percentage", (dagCount * 100.0) / allResults.size());

        summary.put("statistics", statistics);

        String jsonFile = baseDir + "/summary.json";
        jsonMapper.writeValue(new File(jsonFile), summary);
    }

    private Map<String, Object> createDatasetSummary(Map<String, Object> result) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("filename", result.get("filename"));
        summary.put("nodes", result.get("node_count"));
        summary.put("edges", result.get("edge_count"));
        summary.put("components", ((List<?>) result.get("components")).size());
        summary.put("source", result.get("original_source"));
        summary.put("scc_time_ms", result.get("scc_time_ms"));
        summary.put("has_cycles", !(Boolean) result.get("is_dag"));

        if (result.containsKey("topo_time_ms")) {
            summary.put("topo_time_ms", result.get("topo_time_ms"));
        }
        if (result.containsKey("total_time_ms")) {
            summary.put("total_time_ms", result.get("total_time_ms"));
        }
        if (result.containsKey("critical_path")) {
            DAGShortestPath.CriticalPath cp = (DAGShortestPath.CriticalPath) result.get("critical_path");
            summary.put("critical_path_length", cp.length);
        }

        return summary;
    }

    private int getCriticalPathLength(Map<String, Object> result) {
        if (result.containsKey("critical_path")) {
            DAGShortestPath.CriticalPath cp = (DAGShortestPath.CriticalPath) result.get("critical_path");
            return cp.length;
        }
        return 0;
    }

    public String getResultsDirectory() {
        return baseDir;
    }
}