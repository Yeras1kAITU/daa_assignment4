package graph.dagsp;

import graph.Graph;
import graph.Metrics;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.*;

// Computes shortest and longest paths in DAGs
public class DAGShortestPath {
    private final Graph graph;
    private final Metrics metrics;
    private int[] dist;
    private int[] prev;

    public DAGShortestPath(Graph graph, Metrics metrics) {
        this.graph = graph;
        this.metrics = metrics;
    }

    // Computes single-source the shortest paths in a DAG
    public int[] shortestPaths(int source, List<Integer> topologicalOrder) {
        validateInput(source, topologicalOrder);
        metrics.startTimer();

        initializeArrays();
        dist[source] = 0;

        processTopologicalOrder(source, topologicalOrder, false);

        metrics.stopTimer();
        return dist.clone();
    }

    // Computes single-source longest paths in a DAG (critical path)
    public int[] longestPaths(int source, List<Integer> topologicalOrder) {
        validateInput(source, topologicalOrder);
        metrics.startTimer();

        initializeArraysForLongestPath();
        dist[source] = 0;

        processTopologicalOrder(source, topologicalOrder, true);

        metrics.stopTimer();
        return dist.clone();
    }

    private void validateInput(int source, List<Integer> topologicalOrder) {
        if (source < 0 || source >= graph.getNodeCount()) {
            throw new IllegalArgumentException("Source node " + source + " is out of range");
        }
        if (topologicalOrder.size() != graph.getNodeCount()) {
            throw new IllegalArgumentException("Invalid topological order - graph may contain cycles");
        }
    }

    private void initializeArrays() {
        int n = graph.getNodeCount();
        dist = new int[n];
        prev = new int[n];

        Arrays.fill(dist, Integer.MAX_VALUE);
        Arrays.fill(prev, -1);
    }

    private void initializeArraysForLongestPath() {
        int n = graph.getNodeCount();
        dist = new int[n];
        prev = new int[n];

        Arrays.fill(dist, Integer.MIN_VALUE);
        Arrays.fill(prev, -1);
    }

    private void processTopologicalOrder(int source, List<Integer> topologicalOrder, boolean isLongestPath) {
        int sourceIndex = topologicalOrder.indexOf(source);
        if (sourceIndex == -1) {
            throw new IllegalArgumentException("Source node not found in topological order");
        }

        for (int i = sourceIndex; i < topologicalOrder.size(); i++) {
            int u = topologicalOrder.get(i);

            if (dist[u] != (isLongestPath ? Integer.MIN_VALUE : Integer.MAX_VALUE)) {
                relaxEdges(u, isLongestPath);
            }
        }
    }

    private void relaxEdges(int u, boolean isLongestPath) {
        for (Graph.Edge edge : graph.getNeighbors(u)) {
            metrics.incrementRelaxOperations();

            int newDist;
            if (isLongestPath) {
                newDist = dist[u] + edge.weight;
                if (newDist > dist[edge.target]) {
                    dist[edge.target] = newDist;
                    prev[edge.target] = u;
                }
            } else {
                if (dist[u] == Integer.MAX_VALUE) {
                    continue;
                }
                newDist = dist[u] + edge.weight;
                if (newDist < dist[edge.target]) {
                    dist[edge.target] = newDist;
                    prev[edge.target] = u;
                }
            }
        }
    }

    // Reconstructs the shortest/longest path from source to target
    public List<Integer> reconstructPath(int target) {
        if (prev == null) {
            throw new IllegalStateException("Must compute paths before reconstructing");
        }

        List<Integer> path = new ArrayList<>();
        if (dist[target] == Integer.MAX_VALUE || dist[target] == Integer.MIN_VALUE) {
            return path;
        }

        for (int at = target; at != -1; at = prev[at]) {
            path.add(at);
        }
        Collections.reverse(path);
        return path;
    }

    // Finds the longest path in the DAG
    public CriticalPath findCriticalPath(int source, List<Integer> topologicalOrder) {
        int[] longestDists = longestPaths(source, topologicalOrder);

        int maxDist = Integer.MIN_VALUE;
        int criticalNode = -1;
        for (int i = 0; i < longestDists.length; i++) {
            if (longestDists[i] > maxDist && longestDists[i] != Integer.MIN_VALUE) {
                maxDist = longestDists[i];
                criticalNode = i;
            }
        }

        if (criticalNode == -1) {
            return new CriticalPath(0, new ArrayList<>());
        }

        List<Integer> path = reconstructPath(criticalNode);
        return new CriticalPath(maxDist, path);
    }

    public int[] getDistances() {
        return dist != null ? dist.clone() : null;
    }

    public int[] getPrevious() {
        return prev != null ? prev.clone() : null;
    }

    // Represents a critical path with its length and node sequence
    public static class CriticalPath {
        @JsonProperty("length")
        public final int length;

        @JsonProperty("path")
        public final List<Integer> path;

        public CriticalPath(int length, List<Integer> path) {
            this.length = length;
            this.path = Collections.unmodifiableList(path);
        }

        @Override
        public String toString() {
            return "CriticalPath{length=" + length + ", path=" + path + "}";
        }
    }
}