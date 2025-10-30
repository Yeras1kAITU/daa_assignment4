package graph;

import java.util.List;

public class GraphData {
    public boolean directed;
    public int n;
    public List<Edge> edges;
    public int source;
    public String weight_model;

    public static class Edge {
        public int u;
        public int v;
        public int w;

        @Override
        public String toString() {
            return u + "->" + v + "(" + w + ")";
        }
    }

    @Override
    public String toString() {
        return "GraphData{n=" + n + ", edges=" + edges + ", directed=" + directed + "}";
    }
}