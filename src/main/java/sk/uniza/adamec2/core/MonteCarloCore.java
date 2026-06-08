package sk.uniza.adamec2.core;

abstract public class MonteCarloCore {

    protected boolean isSimulationRunning = false;

    public void runSimulation(int numberOfReplications) {
        int replicationNumber = 1;
        beforeSimulation(numberOfReplications);
        isSimulationRunning = true;
        while (replicationNumber <= numberOfReplications && isSimulationRunning) {
            beforeReplication(replicationNumber);
            simulateReplication(replicationNumber);
            afterReplication(replicationNumber);
            replicationNumber++;
        }
        afterSimulation(replicationNumber);
        isSimulationRunning = false;
    }

    public void setSimulationRunning(boolean isSimulationRunning) {
        this.isSimulationRunning = isSimulationRunning;
    }
    public boolean shouldSimulationStop() {
        return !isSimulationRunning;
    }

    abstract protected void beforeSimulation(int numberOfReplications);
    abstract protected void beforeReplication(int replicationNumber);
    abstract protected void simulateReplication(int replicationNumber);
    abstract protected void afterReplication(int replicationNumber);
    abstract protected void afterSimulation(int numberOfCompletedReplications);
}
