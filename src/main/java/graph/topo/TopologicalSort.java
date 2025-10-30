package graph.topo;

import graph.Graph;
import graph.Metrics;
import java.util.*;

public class TopologicalSort {
    private final Graph graph;
    private final Metrics metrics;

    public TopologicalSort(Graph graph, Metrics metrics) {
        this.graph = graph;
        this.metrics = metrics;
    }

    // Performs topological sort using Kahn's algorithm
    public List<Integer> topologicalOrder() {
        metrics.startTimer();
        int n = graph.getNodeCount();
        int[] inDegree = new int[n];

        calculateInDegrees(inDegree);

        // Initialize queue with nodes having zero in-degree
        Queue<Integer> queue = initializeZeroInDegreeQueue(inDegree);

        List<Integer> result = new ArrayList<>();
        processNodes(queue, inDegree, result);

        metrics.stopTimer();

        // Check if topological sort includes all nodes (no cycles)
        if (result.size() != n) {
            return new ArrayList<>();
        }

        return result;
    }

    private void calculateInDegrees(int[] inDegree) {
        for (int u = 0; u < graph.getNodeCount(); u++) {
            for (Graph.Edge edge : graph.getNeighbors(u)) {
                inDegree[edge.target]++;
            }
        }
    }

    private Queue<Integer> initializeZeroInDegreeQueue(int[] inDegree) {
        Queue<Integer> queue = new LinkedList<>();
        for (int i = 0; i < inDegree.length; i++) {
            if (inDegree[i] == 0) {
                queue.offer(i);
                metrics.incrementQueuePushes();
            }
        }
        return queue;
    }

    private void processNodes(Queue<Integer> queue, int[] inDegree, List<Integer> result) {
        while (!queue.isEmpty()) {
            metrics.incrementQueuePops();
            int u = queue.poll();
            result.add(u);

            // Decrement in-degree of neighbors
            for (Graph.Edge edge : graph.getNeighbors(u)) {
                inDegree[edge.target]--;
                if (inDegree[edge.target] == 0) {
                    queue.offer(edge.target);
                    metrics.incrementQueuePushes();
                }
            }
        }
    }

    // Checks if the graph is a DAG\
    public boolean isDAG() {
        return !topologicalOrder().isEmpty();
    }

    // Generates a topological order for a list of SCC components\
    public List<Integer> topologicalOrderOfComponents(List<List<Integer>> components, Graph condensationGraph) {
        TopologicalSort condensationTopo = new TopologicalSort(condensationGraph, metrics);
        return condensationTopo.topologicalOrder();
    }
}