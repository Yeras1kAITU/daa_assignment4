package graph.scc;

import graph.Graph;
import graph.Metrics;
import java.util.*;

public class StronglyConnectedComponents {
    private final Graph graph;
    private final Metrics metrics;
    private boolean[] visited;
    private int[] componentId;
    private List<List<Integer>> components;
    private Graph condensationGraph;

    public StronglyConnectedComponents(Graph graph, Metrics metrics) {
        this.graph = graph;
        this.metrics = metrics;
    }

    public List<List<Integer>> findSCCs() {
        metrics.startTimer();
        initializeDataStructures();

        // Step 1: First DFS for finishing times (order of completion)
        Stack<Integer> stack = new Stack<>();
        for (int i = 0; i < graph.getNodeCount(); i++) {
            if (!visited[i]) {
                dfsFirstPass(i, stack);
            }
        }

        // Step 2: Transpose the graph
        Graph transposed = transposeGraph();

        // Step 3: Second DFS on transposed graph in reverse finishing order
        Arrays.fill(visited, false);
        int currentComponent = 0;

        while (!stack.isEmpty()) {
            int node = stack.pop();
            if (!visited[node]) {
                List<Integer> component = new ArrayList<>();
                dfsSecondPass(transposed, node, currentComponent, component);
                components.add(component);
                currentComponent++;
            }
        }

        buildCondensationGraph();
        metrics.stopTimer();
        return components;
    }

    private void initializeDataStructures() {
        int n = graph.getNodeCount();
        components = new ArrayList<>();
        visited = new boolean[n];
        componentId = new int[n];
        Arrays.fill(componentId, -1);
    }

    private void dfsFirstPass(int node, Stack<Integer> stack) {
        metrics.incrementDfsVisits();
        visited[node] = true;

        for (Graph.Edge edge : graph.getNeighbors(node)) {
            metrics.incrementEdgeTraversals();
            if (!visited[edge.target]) {
                dfsFirstPass(edge.target, stack);
            }
        }
        stack.push(node);
    }

    private void dfsSecondPass(Graph transposed, int node, int compId, List<Integer> component) {
        metrics.incrementDfsVisits();
        visited[node] = true;
        componentId[node] = compId;
        component.add(node);

        for (Graph.Edge edge : transposed.getNeighbors(node)) {
            metrics.incrementEdgeTraversals();
            if (!visited[edge.target]) {
                dfsSecondPass(transposed, edge.target, compId, component);
            }
        }
    }

    private Graph transposeGraph() {
        Graph transposed = new Graph(graph.getNodeCount(), true);
        for (int u = 0; u < graph.getNodeCount(); u++) {
            for (Graph.Edge edge : graph.getNeighbors(u)) {
                transposed.addEdge(edge.target, u, edge.weight);
            }
        }
        return transposed;
    }

    private void buildCondensationGraph() {
        condensationGraph = new Graph(components.size(), true);

        // For each component, add edges to other components (avoid duplicates)
        for (int uComp = 0; uComp < components.size(); uComp++) {
            Set<Integer> targetComponents = new HashSet<>();

            for (int u : components.get(uComp)) {
                for (Graph.Edge edge : graph.getNeighbors(u)) {
                    int vComp = componentId[edge.target];
                    if (uComp != vComp && !targetComponents.contains(vComp)) {
                        condensationGraph.addEdge(uComp, vComp, edge.weight);
                        targetComponents.add(vComp);
                    }
                }
            }
        }
    }

    public List<List<Integer>> getComponents() {
        return Collections.unmodifiableList(components);
    }

    public Graph getCondensationGraph() {
        return condensationGraph;
    }

    public int[] getComponentIds() {
        return componentId.clone();
    }

    public Graph getWeightedCondensationGraph() {
        // For now, returns the same as getCondensationGraph()
        // In advanced implementation will aggregate weights from original nodes
        return condensationGraph;
    }
}