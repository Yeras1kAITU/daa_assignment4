package graph;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a directed graph with weighted edges
 */
public class Graph {
    private final int n;
    private final List<List<Edge>> adjList;
    private final boolean directed;

    public Graph(int n, boolean directed) {
        this.n = n;
        this.directed = directed;
        this.adjList = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            adjList.add(new ArrayList<>());
        }
    }

    /**
     * Adds a directed edge from u to v with given weight
     */
    public void addEdge(int u, int v, int weight) {
        validateNode(u);
        validateNode(v);
        adjList.get(u).add(new Edge(v, weight));
    }

    /**
     * Returns all outgoing edges from the given node
     */
    public List<Edge> getNeighbors(int node) {
        validateNode(node);
        return adjList.get(node);
    }

    public int getNodeCount() {
        return n;
    }

    public boolean isDirected() {
        return directed;
    }

    private void validateNode(int node) {
        if (node < 0 || node >= n) {
            throw new IllegalArgumentException("Node " + node + " is out of range [0, " + (n-1) + "]");
        }
    }

    /**
     * Represents a directed edge with weight
     */
    public static class Edge {
        public final int target;
        public final int weight;

        public Edge(int target, int weight) {
            this.target = target;
            this.weight = weight;
        }

        @Override
        public String toString() {
            return "->" + target + "(" + weight + ")";
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Graph(n=").append(n).append(", directed=").append(directed).append(")\n");
        for (int i = 0; i < n; i++) {
            sb.append(i).append(": ").append(adjList.get(i)).append("\n");
        }
        return sb.toString();
    }
}