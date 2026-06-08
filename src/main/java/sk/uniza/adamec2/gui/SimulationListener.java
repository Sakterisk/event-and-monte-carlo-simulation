package sk.uniza.adamec2.gui;

public interface SimulationListener {
    void onReplicationComplete(int replicationNumber, double averageTime);
    void onSimulationComplete(double finalAverage);
    void onSimulationStopped(double currentAverage, int completedReplications);
}
