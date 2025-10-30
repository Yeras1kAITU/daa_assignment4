package graph;

public class Metrics {
    private long startTime;
    private long endTime;

    // Operation counters
    private int dfsVisits;
    private int edgeTraversals;
    private int queuePushes;
    private int queuePops;
    private int relaxOperations;

    public void startTimer() {
        this.startTime = System.nanoTime();
    }

    public void stopTimer() {
        this.endTime = System.nanoTime();
    }

    public long getElapsedTimeNanos() {
        return endTime - startTime;
    }

    public double getElapsedTimeMillis() {
        return getElapsedTimeNanos() / 1_000_000.0;
    }

    // Counter increment methods
    public void incrementDfsVisits() { dfsVisits++; }
    public void incrementEdgeTraversals() { edgeTraversals++; }
    public void incrementQueuePushes() { queuePushes++; }
    public void incrementQueuePops() { queuePops++; }
    public void incrementRelaxOperations() { relaxOperations++; }

    // Getters
    public int getDfsVisits() { return dfsVisits; }
    public int getEdgeTraversals() { return edgeTraversals; }
    public int getQueuePushes() { return queuePushes; }
    public int getQueuePops() { return queuePops; }
    public int getRelaxOperations() { return relaxOperations; }

    public void reset() {
        dfsVisits = 0;
        edgeTraversals = 0;
        queuePushes = 0;
        queuePops = 0;
        relaxOperations = 0;
        startTime = 0;
        endTime = 0;
    }

    @Override
    public String toString() {
        return String.format(
                "Metrics[time=%.3fms, dfsVisits=%d, edges=%d, queueOps=(push=%d, pop=%d), relax=%d]",
                getElapsedTimeMillis(), dfsVisits, edgeTraversals, queuePushes, queuePops, relaxOperations
        );
    }
}