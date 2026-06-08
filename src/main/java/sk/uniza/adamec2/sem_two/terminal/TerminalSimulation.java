package sk.uniza.adamec2.sem_two.terminal;

import sk.uniza.adamec2.core.EventCore;
import sk.uniza.adamec2.generator.*;
import sk.uniza.adamec2.sem_two.segment.Arrival;
import sk.uniza.adamec2.sem_two.terminal.entity.Person;
import sk.uniza.adamec2.sem_two.terminal.entity.factory.PersonFactory;
import sk.uniza.adamec2.sem_two.terminal.event.PersonArrivalEvent;
import sk.uniza.adamec2.sem_two.terminal.event.StatsSnapshotEvent;
import sk.uniza.adamec2.sem_two.terminal.event.WarmUpEndEvent;
import sk.uniza.adamec2.sem_two.terminal.segment.SingleTerminal;
import sk.uniza.adamec2.stat.MeanStat;
import sk.uniza.adamec2.stat.TimeWeightedMeanStat;
import sk.uniza.adamec2.util.TimeParser;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Random;

public class TerminalSimulation extends EventCore {

    private final static double ONE_DAY = 24.0;
    private final static double WARM_UP = 60.0;

    // ---- Replication bookkeeping for GUI ----
    private int currentReplication = 0;
    private int totalReplications = 0;

    // ---- Segments
    private final SingleTerminal terminal1;
    private final SingleTerminal terminal2;
    private final Arrival<PersonArrivalEvent, Person> arrival;

    // ---- Generators
    private final ContinuousUniformGen terminalChoiceGen;

    // ---- Replication statistics
    private final MeanStat averageTimeInSim;
    private final TimeWeightedMeanStat averageQueueLengthBeforePlaceCrate;
    private final TimeWeightedMeanStat averageQueueLengthBeforeTakeCrate;

    // ---- Simulation statistics
    private final MeanStat combAverageTimeInSim;
    private final MeanStat combAverageQueueLengthBeforePlaceCrate;
    private final MeanStat combAverageQueueLengthBeforeTakeCrate;

    public TerminalSimulation() {
        super(ONE_DAY + WARM_UP);


        // ---- Arrival initialization
        Gen<Double> arrivalGen = new ExponentialGen(60.0);
        Gen<Integer> baggageGen = new DiscreteEmpiricGen(
                new DiscreteEmpiricGen.InputWrapper(0, 1, 0.15),
                new DiscreteEmpiricGen.InputWrapper(1, 2, 0.68),
                new DiscreteEmpiricGen.InputWrapper(2, 3, 0.17)
        );
        arrival = new Arrival<>(arrivalGen, new PersonFactory(baggageGen), this);
        PersonArrivalEvent ae = new PersonArrivalEvent(0.0, this, null, arrival);
        arrival.addArrivalEvent(ae);

        // ---- Terminal initialization
        terminal1 = new SingleTerminal(this);
        terminal2 = new SingleTerminal(this);

        // ---- Generators initialization
        terminalChoiceGen = new ContinuousUniformGen(0.0, 1.0);

        // ---- Statistics
        averageTimeInSim = new MeanStat();
        averageQueueLengthBeforePlaceCrate = new TimeWeightedMeanStat();
        averageQueueLengthBeforeTakeCrate = new TimeWeightedMeanStat();
        combAverageTimeInSim = new MeanStat();
        combAverageQueueLengthBeforePlaceCrate = new MeanStat();
        combAverageQueueLengthBeforeTakeCrate = new MeanStat();
    }

    public void startSimulation(int replications, int capacity, int beforeXrayMaxSize, int afterXrayMaxSize) {
        this.totalReplications = replications;
        setArrivalGen(capacity);
        terminal1.changeFrontBeforeXrayMaxSize(beforeXrayMaxSize);
        terminal1.changeFrontAfterXrayMaxSize(afterXrayMaxSize);
        terminal2.changeFrontBeforeXrayMaxSize(beforeXrayMaxSize);
        terminal2.changeFrontAfterXrayMaxSize(afterXrayMaxSize);
        runSimulation(replications);
    }

    @Override
    public void beforeSimulation(int numberOfReplications) {
        super.beforeSimulation(numberOfReplications);
        combAverageTimeInSim.clear();
        combAverageQueueLengthBeforePlaceCrate.clear();
        combAverageQueueLengthBeforeTakeCrate.clear();
    }

    @Override
    public void beforeReplication(int replicationNumber) {
        time = 0.0;
        this.currentReplication = replicationNumber;
        arrival.planNextArrivalEvent();
        addEvent(new WarmUpEndEvent(WARM_UP, this));
        averageQueueLengthBeforeTakeCrate.setInitialValues(0, time);
        averageQueueLengthBeforeTakeCrate.setInitialValues(0, time);
        super.beforeReplication(replicationNumber);
    }


    @Override
    public void afterReplication(int replicationNumber) {
        // Close place-queue stat at end of day
        int sum1 = terminal1.getQueueLengthBeforePlaceCrate()
                + terminal2.getQueueLengthBeforePlaceCrate();
        averageQueueLengthBeforePlaceCrate.add(sum1, ONE_DAY + WARM_UP);

        // Close take-queue stat at end of day
        int sum2 = terminal1.getQueueLengthBeforeTakeCrate()
                + terminal2.getQueueLengthBeforeTakeCrate();
        averageQueueLengthBeforeTakeCrate.add(sum2, ONE_DAY + WARM_UP);

        combAverageTimeInSim.add(averageTimeInSim.getMean());
        combAverageQueueLengthBeforePlaceCrate.add(averageQueueLengthBeforePlaceCrate.getMean());
        combAverageQueueLengthBeforeTakeCrate.add(averageQueueLengthBeforeTakeCrate.getMean());

        super.afterReplication(replicationNumber);

        averageTimeInSim.clear();
        averageQueueLengthBeforePlaceCrate.clear();
        averageQueueLengthBeforeTakeCrate.clear();
        terminal1.clear();
        terminal2.clear();
        arrival.clear();
    }

    @Override
    protected void afterSimulation(int numberOfCompletedReplications) {
        super.afterSimulation(numberOfCompletedReplications);
    }

    private void setArrivalGen(int capacity) {
        arrival.setArrivalGen(new ExponentialGen((double) capacity / ONE_DAY));
    }

    public void lengthBeforePlaceCrateChange() {
        int sum = terminal1.getQueueLengthBeforePlaceCrate()
                + terminal2.getQueueLengthBeforePlaceCrate();
        averageQueueLengthBeforePlaceCrate.add(sum, getTime());
    }

    public void lengthBeforeTakeCrateChange() {
        int sum = terminal1.getQueueLengthBeforeTakeCrate()
                + terminal2.getQueueLengthBeforeTakeCrate();
        averageQueueLengthBeforeTakeCrate.add(sum, getTime());
    }

    public void personLeftTerminal(Person person) {
        averageTimeInSim.add(getTime() - person.getArriveTime());
    }

    public void planArrivalToTerminal(Person entity) {
        if (terminal1.getQueueLengthBeforePlaceCrate() > terminal2.getQueueLengthBeforePlaceCrate()) {
            terminal2.arrivalToTerminal(entity);
            return;
        } else if (terminal1.getQueueLengthBeforePlaceCrate() < terminal2.getQueueLengthBeforePlaceCrate()) {
            terminal1.arrivalToTerminal(entity);
            return;
        }
        double prob = terminalChoiceGen.next();
        if (prob < 0.5) {
            terminal1.arrivalToTerminal(entity);
        } else {
            terminal2.arrivalToTerminal(entity);
        }
    }

    public int getCurrentReplication() {
        return currentReplication;
    }

    public int getTotalReplications() {
        return totalReplications;
    }

    public double getCombinedAverageTimeInSim() {
        return combAverageTimeInSim.getMean();
    }

    public double getCombinedAverageQueueLengthBeforePlaceCrate() {
        return combAverageQueueLengthBeforePlaceCrate.getMean();
    }

    public double getCombinedAverageQueueLengthBeforeTakeCrate() {
        return combAverageQueueLengthBeforeTakeCrate.getMean();
    }

    public SingleTerminal getTerminal1() {
        return terminal1;
    }

    public SingleTerminal getTerminal2() {
        return terminal2;
    }

    // ---- For GUI: arrival snapshot ----

    public double getNextArrivalTime() {
        return arrival.getNextArrivalTime();
    }

    public Person getNextArrivalPerson() {
        return arrival.getWillArrive();
    }

    public void resetStatistics() {
        averageTimeInSim.clear();
        averageQueueLengthBeforePlaceCrate.clear();
        averageQueueLengthBeforeTakeCrate.clear();
        int sum1 = terminal1.getQueueLengthBeforePlaceCrate()
                + terminal2.getQueueLengthBeforePlaceCrate();
        averageQueueLengthBeforePlaceCrate.setInitialValues(sum1, time);
        int sum2 = terminal1.getQueueLengthBeforeTakeCrate()
                + terminal2.getQueueLengthBeforeTakeCrate();
        averageQueueLengthBeforeTakeCrate.setInitialValues(sum2, time);
    }


    // ---- For warm up faze approximation
    public double getAverageTimeInSimSnapshot() {
        return averageTimeInSim.getMean();   // MeanStat already exists
    }

    public double getAverageQueueLengthBeforePlaceCrateSnapshot() {
        return averageQueueLengthBeforePlaceCrate.getMean();  // TimeWeightedMeanStat
    }

    public double getAverageQueueLengthBeforeTakeCrateSnapshot() {
        return averageQueueLengthBeforeTakeCrate.getMean();   // TimeWeightedMeanStat
    }

    public double getMaxTime() {   // needed by the event to know when to stop
        return maxTime;
    }

    public double getCombinedAverageTimeInSimVariance() {
        return combAverageTimeInSim.getVariance();
    }

    public double getCombinedAverageQueueLengthBeforePlaceCrateVariance() {
        return combAverageQueueLengthBeforePlaceCrate.getVariance();
    }

    public double getCombinedAverageQueueLengthBeforeTakeCrateVariance() {
        return combAverageQueueLengthBeforeTakeCrate.getVariance();
    }
}
